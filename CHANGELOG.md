# Changelog

## 1.3.1 — sin broad/silent excepts
- `logErr(tag, msg)` a logcat, seguro en tests JVM.
- Catches angostos: stores/backup (`IllegalArgumentException`), IP local (`SocketException`), import/export (`IOException`, `IllegalArgumentException`), versión (`NameNotFoundException`).
- Caídas de red se loguean una vez por transición en `markSent`.

## 1.3.0 — header común + Ajustes remodelado
- Header único por tab (Inicio conserva marca + IP; control solo punto + estado).
- Ajustes en secciones Conexión/Pantalla/Datos (Datos al final), tema Oscuro/Claro, decimales punto/coma, ayudas.
- Tema en vivo sin recreate (estado en `setContent`); default oscuro.

## 1.2.0 — batch swarm audit
- Presets: se puede crear el primero; borrar con undo (sin modal); sobrescribir con Deshacer; aplicar no toca la red.
- Import robusto: cap 512KB/32/64, BOM, defaults, swap de rangos, dedupe, rate mínimo, puerto validado, `syncNextId`.
- UI: filas de 48dp con strike al mutear, sliders con label, página "1/5", handle de reorder, G desacoplado del acento, decimales con coma opcional, diálogos destructivos en rojo, tema claro/oscuro, Scaffold.
- Seguridad: `allowBackup=false`.
- 46 tests verdes.

## 1.1.0 — red team
- Fix crash de magnitud en sensores escalares; feed se recrea al cambiar tipo/rate.
- Niveles clamp en load/import/presets/live/fade (adiós B=6.00); editor resincroniza tras preset/import.
- Import capado con `syncNextId`; throttle UI 100ms; sin envíos en background; fade cancelable.
- `sameSubnet` eliminado; canal nuevo numera por hueco.

## 1.0.0 — robustez + features
- Canales multi-valor con máscara por componente (XY, RGB, XYZM de sensores); tap para silenciar.
- Overview con filas R/G/B y X/Y/Z/M con letras de color; reorder por drag en Inicio y Presets.
- Presets: aplicar con fade de 300ms, dirty-dot, undo al borrar, filtro, exportar/importar JSON.
- Fader/XY con tap-to-jump y doble-tap reset; envío a 15/s + final al soltar.
- Sensores sin selector de eje (siempre 4 componentes); proximidad eliminada.
- Backup .bak de prefs, crash log con diálogo, migración de orden versionada.
- UI: Material icons, brillo bajo, demo inicial, landscape 4x2, prefijo de dirección opcional.
- Tests: 30 verdes (Channel 23 + Osc 7).
