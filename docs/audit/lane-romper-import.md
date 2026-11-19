# Lane romper import — oscpad

Fecha: 2026-11-19
Alcance: `decodeBackup` (Config.kt) + emulación de `syncNextId`. Sin tocar código de producción.
Test temporal: `app/src/test/java/ar/matu/oscpad/FuzzImportTest.kt` (15 tests, eliminado al cerrar).

## Cómo se corrió

```
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
$env:ANDROID_HOME='C:\Android'
.\gradlew.bat testDebugUnitTest --tests "ar.matu.oscpad.FuzzImportTest" --no-daemon
```

Resultado exacto de la última corrida:

```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 29s
EXITCODE=0
tests=15 failures=0 errors=0 time=0.647
```

Una corrida previa con asserts más estrictos (no-null donde el criterio acepta null) dio `15 tests completed, 3 failed`, exit 1. Esos tres eran supuestos míos, no roturas: el criterio binario admite `null`. Corregido, verde.

## Casos y salida observada

Todos los casos: o parsearon dentro de límites, o devolvieron `null`. Ningún `Throwable` escapó de `decodeBackup`, ningún OOM, ningún hang (timeout por test 60-120 s, tiempos reales 0.002-0.249 s).

| Caso | Entrada | Resultado |
|---|---|---|
| Backup vacío | `{}` | `OscBackup` con canales/presets vacíos y net default |
| Raíces degeneradas | `null`, `[]`, `""`, espacios, `// backup` | `null` (JsonDecodingException capturada) |
| 100k canales | lista de 100.000 `{"id","name"}` (~2,2 MB) | parsea, `canales=32` (cap aplicado), ~0.16 s |
| Preset con 100k canales | 1 preset con 100.000 canales | parsea, `canalesPreset=100000` (sin cap interno) |
| Nesting profundo, array desconocido | `{"x":` + 200.000 `[` + `1` + `]` | parsea (`skipElement` iterativo), sin StackOverflow |
| Nesting profundo, objeto desconocido | 200.000 `{"a":` anidados | parsea, sin StackOverflow |
| Ids extremos | `-1`, `Long.MIN_VALUE`, `Long.MAX_VALUE`, `0` | parsean tal cual, sin validación |
| Ids duplicados | 3 canales con `id:5` | parsea, `ids=[5,5,5]`; `bumpCounter(5,[5,5,5])=6` |
| Colisión next_id | `bumpCounter(Long.MAX_VALUE,[Long.MAX_VALUE])` | `9223372036854775807`; `bumpCounter(1,[MIN,MAX])=1` |
| Nombres unicode/control | 日本語, `\u0000`, `\u0007`, emoji escapado, árabe, 1000 chars | parsea, `[3:???, 11:?nul?b, 8:? ????, 1000:aaaaaa]`; no se sanitizan |
| Tipos string desconocidos | `"holograma"`, `""`, `"FADER"` | coercionan a `[fader, fader, fader]` |
| Tipos JSON no-string | `type:null`, `type:42`, `type:["fader"]` | backup entero `null`, no default a fader |
| Floats no finitos | `NaN`, `Infinity`, `-Infinity`, `1e39`, `1e400`, `3.4e39` | `null` (Json estricto rechaza, capturado) |
| Floats extremos finitos | `1e38`, `1.4e-45` | parsean: `min=1.0E38`, `level=1.4E-45` |
| min > max | `min:10,max:-10`; `min2:5,max2:-5` | parsea sin validar; salidas `[[0.0],[0.5,0.0]]` |
| Campos 10 MB | name y host de 10.000.000 chars | parsea si se llama `decodeBackup` directo (el cap 512 KB vive en MainActivity) |
| Truncado / basura | JSON cortado, `{} {}`, `} basura`, `\u0000` final | `null` |
| BOM UTF-8 | `\uFEFF{"channels":[]}` | `null` |
| Claves duplicadas | dos `"channels"` | gana la última: `canales=[b]` |
| Preset ids duplicados | dos presets `id:1` | parsean: `presets=[p, p2]` |
| Escalares raros | `id:1e30`, `id:1.5`, `port:4294967296` | `null` |
| Escalares sin rango | `port:-1`; `mask:-2147483648..2147483647`; `rateMs:-1` y `Long.MAX_VALUE` | parsean tal cual |

## Hallazgos

1. **Los canales dentro de presets no tienen cap.** `decodeBackup` hace `.take(32)` solo en `backup.channels`; `presets` se limita a 64 pero cada `Preset.channels` queda completo. Un preset con 100.000 canales entra entero. El límite real de import (512 KB) acota el total, pero la invariante "32 canales" no se cumple uniformemente: aplicar un preset puede restaurar miles de canales.
2. **`Preset.channels` es obligatorio (sin default).** Un backup bien formado según el export actual lo incluye, pero cualquier JSON externo/legacy sin `channels` en un preset hace fallar todo el import (`MissingFieldException` → `null`). Lo detecté de rebote: mis primeros fixtures con preset sin `channels` devolvían `null` con ids válidos.
3. **BOM UTF-8 rompe el import.** `\uFEFF{"channels":[]}` devuelve `null`. Un archivo guardado desde Windows con BOM se rechaza como "Archivo inválido".
4. **`mask`, `rateMs` y `port` no se validan ni clampan.** Se aceptan `mask` negativo, `rateMs = -1` o `Long.MAX_VALUE`, `port = -1`. `Channel.clamped()` solo toca los `level*`. Un `port:-1` importado llega al sender; `rateMs` negativo afecta el feed de sensores.
5. **`min > max` se acepta y produce salidas invertidas.** `current()` y `xyValues()` con `max < min` devuelven valores fuera del rango nominal (el test XY dio `0.0` para el eje Y con rango `5..-5` y `level=0.5`). No hay validación de rango.
6. **`type:null` rechaza el backup entero.** El caso de tipo desconocido se maneja para strings (`"holograma"` → `fader`) pero `coerceInputValues` está desactivado, así que `null` o un tipo JSON no-string tiran abajo todo el archivo en vez de caer al default.
7. **Ids sin validación: negativos y duplicados entran.** `Long.MIN_VALUE`, `-1` y tres canales con `id:5` parsean. `bumpCounter` evita ids futuros duplicados en el caso normal, pero con `Long.MAX_VALUE` presente `bumpCounter(MAX,[MAX]) = MAX`: el próximo `nextId()` devuelve `MAX` y persiste `MAX+1 = MIN_VALUE`, dejando `next_id` negativo para el canal siguiente.
8. **El cap de 512 KB está solo en el import de `MainActivity`.** `decodeBackup` no tiene límite de tamaño; cualquier otro caller (restore de prefs, llamada directa) acepta strings de 10 MB sin problema. No es explotable desde la UI actual, pero el contrato de tamaño no vive en la función que parsea.
9. **No hay riesgo de recursión/OOM en lo probado.** 200.000 niveles de anidamiento desconocido parsean sin StackOverflow (`skipElement` iterativo) y 100k canales no acercan al OOM. Queda como riesgo residual no medido: `decodeBackup` hace `catch (e: Exception)`, así que un `OutOfMemoryError` o `StackOverflowError` no se capturaría; no logré dispararlo con entradas realistas para el cap de 512 KB.
10. **Duplicados de claves JSON: gana la última, en silencio.** `{"channels":[...a],"channels":[...b]}` deja `[b]`. Es comportamiento estándar de kotlinx, no una validación explícita; vale saberlo si se mergean backups a mano.
11. **Nombres no se sanitizan al importar.** Nombres con NUL, bell, emoji o 日本語 entran tal cual (solo se filtra `isNotBlank`). `sanitizeName` existe pero se usa en la creación manual, no en import. El contrato OSC `address()` los va a emitir crudos.

## Cobertura que no corrió

- No se ejecutó el full suite (prohibido por brief).
- No se probó el path real de `MainActivity` (lectura del `contentResolver`, cap 512 KB, `syncNextId`) porque requiere instrumentación Android; el cap y el `syncNextId` se evaluaron por lectura + emulación de `bumpCounter`.
- No se forzó OOM ni StackOverflow deliberado: el criterio pide que el test termine verde y un OOM mata el worker sin reporte.
