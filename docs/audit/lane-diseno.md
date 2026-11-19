# Crítica de diseño y funcionalidad — oscpad v1.1.0

Carril D. Dispositivo `ofzh7drczxz9l74d`, 1080x2392 px, densidad 450 (2.8125 px/dp).
APK instalado: `ar.matu.oscpad` release. Sin ediciones, sin commits, sin mutar datos.

## Método

- Capturas con `adb shell screencap` + `adb pull` a `captures/` (ignorada por git).
- Para cada tab: dump de `uiautomator` para leer bounds reales y verificar alturas de touch target.
- Lo que no se pudo ver en pantalla se marca «código» y cita `archivo:línea`. Nada de hallazgos por intuición.

## Límites de esta pasada

1. El equipo se bloqueó con patrón a las 00:23 (ver `captures/lock.png`) y no intenté desbloquearlo. Cerré la captura ahí: las pantallas de canal tipo Botón/XY/Sensor y la fila de preset con datos no se pudieron fotografiar. Esas partes son «código».
2. El usuario tiene 0 presets, así que el estado con filas tampoco era observable. Idem.
3. A las 00:19 apareció un snackbar «Canal borrado / Deshacer» que no disparé; se fue solo a los ~5 s. Inventario antes y después: los mismos 5 canales con los mismos valores (`canal1`, `canal2`, `canal4`, `canal3`, `canal5`), 0 presets. No reproduje el evento. Puede ser un tap mío que cayó corrido por un cambio de pantalla en paralelo. Queda como observación, no como hallazgo.
4. `adb shell input keyevent BACK` cierra la app (historial de tabs de 1 elemento → `finish()`). La app quedó cerrada al terminar.

## Capturas

| Archivo | Pantalla |
|---|---|
| `captures/d_inicio.png` | Inicio, grilla de 5 canales |
| `captures/d_canales.png` | Canales, canal1 (Color) |
| `captures/d_presets.png` | Presets, estado vacío |
| `captures/d_ajustes.png` | Ajustes completo |
| `captures/dump-inicio.xml` | Bounds de la grilla (mediciones) |
| `captures/dump-canales.xml` | Bounds de la página de canal (mediciones) |
| `captures/lock.png` | Bloqueo del equipo (límite 1) |

## Resumen

| Severidad | Cantidad | Qué significa |
|---|---|---|
| P0 | 1 | Rompe un flujo completo |
| P1 | 11 | Pérdida de datos, mentira de estado o control inaccesible |
| P2 | 12 | Fricción seria de uso diario |
| P3 | 9 | Pulido, copy, consistencia |

---

# P0

### H01 · No existe forma de crear el primer preset
`captures/d_presets.png` + `PresetsPanel.kt:139-164` y `PresetsPanel.kt:401-418`.

Con `presets.isEmpty()` la pantalla sólo muestra el botón «Creá tu primer preset», que llama `onGoChannels` (`MainActivity.kt:691`): cambia a Canales y, si no hay canales, crea uno. El botón «Nuevo preset» —único que abre el diálogo de nombre, único que llama `onSaveNew`— vive en la rama `else`, o sea que sólo existe cuando ya hay presets. Con 0 presets el ciclo es: tocás → te lleva a Canales → volvés a Presets → el mismo botón. Nunca se crea el preset. La única salida es importar un backup que ya traiga presets.

**Fix:** mostrar «Nuevo preset» también en el vacío y que el CTA del estado vacío abra el diálogo de nombre en vez de saltar a Canales.

---

# P1

### H02 · El punto verde de estado miente
`captures/d_canales.png` (punto verde, TD sin verificar) + `MainActivity.kt:207`, `MainActivity.kt:339-345`, `MainActivity.kt:538-557`.

`connOk` arranca en `true` sin haber enviado un solo paquete: la primera vez que abrís la app el semáforo ya está en verde. Después, `markSent(ok)` recibe lo que devuelve `sender.send(...)`, que es «el socket UDP aceptó el datagrama». UDP no tiene ACK: si TouchDesigner está cerrado, el punto sigue verde igual.

**Fix:** estado de tres valores (sin datos / último envío ok / error) y texto «último envío hace X s» al lado del punto.

### H03 · Borrar canal no pide confirmación; borrar preset sí
`ChannelModule.kt:252-262` vs `PresetsPanel.kt:422-434`.

La X del encabezado borra al primer toque con un «Deshacer» de 5 s (`MainActivity.kt:307-337`). El preset, que es un snapshot y se puede volver a guardar, tiene diálogo modal con texto. La jerarquía de riesgo está al revés y el patrón no es el mismo entre tabs.

**Fix:** unificar en un solo patrón (confirmación inline o snackbar con undo) para las dos cosas.

### H04 · «Sobrescribir preset» es silencioso y destructivo
`PresetsPanel.kt:323-335`.

El icono de disquete pisa el preset con el estado actual: sin confirmación, sin snackbar, sin undo. Es la acción más peligrosa de la pantalla y es la que menos feedback da. Un roce al hacer scroll y perdiste el preset.

**Fix:** confirmación de una línea + snackbar «Preset sobrescrito» con Deshacer.

### H05 · Aplicar un preset te cambia la IP y el puerto sin avisar
`MainActivity.kt:614-624`.

`net = preset.net` en cada aplicación. El preset guarda red además de valores. Si dos presets se armaron con hosts distintos, tocar una fila te reconecta a otra máquina en plena función y el único indicio es el texto de la barra superior.

**Fix:** aplicar sólo canales; la red se cambia desde Ajustes, o como mucho preguntar cuando difiere.

### H06 · «Borrar canales» en Ajustes: doble toque sin timeout, sin undo, con presets huérfanos
`SettingsPanel.kt:174-194`, `MainActivity.kt:731-735`.

Primer toque arma (`confirmClear = true`), segundo borra todo. El estado armado no expira: si tocás una vez, te vas a otra pestaña y volvés, el botón sigue armado a la espera de un roce. No hay Deshacer y los presets quedan apuntando a canales que ya no existen.

**Fix:** diálogo modal con conteo («se borran 5 canales») y undo por snackbar.

### H07 · Cuatro iconos de 40dp por fila en Presets
`PresetsPanel.kt:225-362` (código; sin datos no se pudo fotografiar).

Chevron, Save, Edit y X: 4 × 40dp = 160dp de los 411dp útiles de la fila. 40dp está por debajo del mínimo de 48dp, y son cuatro blancos contiguos sin separación, con la X destructiva al lado del lápiz.

**Fix:** dejar el chevron y un menú de tres puntos; el borrado va ahí adentro.

### H08 · Las filas de componente (máscara) miden 35dp de alto
`captures/dump-inicio.xml` + `ChannelModule.kt:710-744`.

Medido: fila R `[171,411][393,511]` = 222×100 px = 79×35.5dp; fila B = 48dp porque la letra es más alta. La altura depende del texto, así que la zona táctil cambia de tamaño según la letra y nunca llega a 48dp en R y G. Encima, esa fila es un toggle que no se anuncia: el único indicio de muteado es bajar la opacidad a 0.35.

**Fix:** `heightIn(min = 48.dp)` fijo y un indicador explícito de mute (barra tachada o switch chico).

### H09 · El carrusel de Canales no dice cuántos canales hay ni dónde estás
`captures/d_canales.png` + `MainActivity.kt:960-1003`, `MainActivity.kt:1175-1188`.

Cinco pantallas idénticas salvo el nombre, separadas por una barra de 2dp que casi no se ve. No hay puntos de página, ni «1/5», ni flechas. Un usuario nuevo no descubre que puede deslizar; uno experimentado no sabe si hay algo a la derecha.

**Fix:** indicador de posición arriba del nombre («2 / 5») y divider de 2dp con más contraste.

### H10 · En la página Color, las letras que parecen etiquetas son botones de mute
`captures/d_canales.png` + `ChannelModule.kt:444-468`.

R, G y B están abajo de los sliders como si rotularan, pero son toggles de máscara. Tocar «G» para leerlo mutea el canal. Los sliders en sí no tienen label ni `contentDescription`: TalkBack lee «SeekBar 0» tres veces sin decir cuál es cuál.

**Fix:** mover el toggle al extremo de cada slider con ícono de ojo/altavoz y darle `contentDescription = "Rojo"` al slider.

### H11 · Números con punto decimal en una interfaz en español
`captures/d_inicio.png` («0.24») + `ChannelModule.kt:61-65`.

`"%.2f".format(Locale.US, ...)`. Todo el resto está en rioplatense; los valores no.

**Fix:** formatear con el locale del sistema (o coma fija) sin tocar el contrato OSC.

### H12 · Los diálogos pintan el destructivo en verde y el cancelar en rojo
`captures/d_ajustes.png` (Exportar/Importar/«Borrar canales» con esa lógica) + `PresetsPanel.kt:493-516`, `MainActivity.kt:1020-1064`.

«Borrar» y «Reemplazar» van en `primary` (lima), «Cancelar» en `error` (rojo). En el resto de la app el rojo es error y el verde es «adelante». El usuario lee el color, no el verbo.

**Fix:** confirmación destructiva en `error`, cancelar en neutro.

---

# P2

### H13 · Jerarquía invertida en la grilla de Inicio
`captures/d_inicio.png` + `MainActivity.kt:879-935`.

El nombre del canal es el texto más chico de la tarjeta (titleSmall 14sp) y el valor el más grande (22-30sp en mono). Para encontrar «canal4» hay que leer números. Además no hay contenedor: las tarjetas flotan sobre negro sin borde ni fondo, así que tampoco se ve dónde termina una y empieza otra, ni que son tocables.

**Fix:** subir el nombre a 18-20sp con peso, bajar el valor, y darle a la tarjeta un `surfaceVariant` con radio.

### H14 · Tres encabezados distintos para el mismo nivel
`captures/d_presets.png` («Presets» en displaySmall 36sp), `captures/d_inicio.png` (sin título), `captures/d_canales.png` (nombre centrado 22sp), `captures/d_ajustes.png` (sin título).

Cada tab inventó su cabecera. Entrar a Presets parece entrar a otra app.

**Fix:** un header común (título a la izquierda, 24sp) para las cuatro tabs.

### H15 · La barra superior repite marca y endpoint en las cuatro pestañas
`MainActivity.kt:524-558`, visible en las cuatro capturas.

«oscpad» + «192.168.1.35:8000» ocupan ~100px en cada pantalla. La IP ya está editable en Ajustes; en Canales le come alto a un fader vertical, que es lo único que importa ahí.

**Fix:** dejar sólo el punto de estado en tabs de control y la marca completa en Inicio.

### H16 · Ajustes es una pila sin secciones
`captures/d_ajustes.png` + `SettingsPanel.kt:71-219`.

Red, prueba, export/import, borrado, brillo y versión con el mismo espaciado y sin subtítulos. «Brillo bajo» aparece después de «Borrar canales» como si fueran parientes. Nada explica qué hace: baja la pantalla a 5% (`MainActivity.kt:185`).

**Fix:** tres bloques con subtítulo (Conexión / Datos / Pantalla) y una línea de ayuda bajo el switch.

### H17 · El destructivo es el texto más apagado de Ajustes
`captures/d_ajustes.png` + `SettingsPanel.kt:186-193`.

«Borrar canales» en `onSurfaceVariant` (gris 8:1, mismo peso que el resto de los labels) cuando es la única acción irreversible de la pantalla.

**Fix:** color `error` en reposo y separación vertical del resto.

### H18 · Tres estados vacíos con tres criterios
`MainActivity.kt:780-812` (no observable: la grilla tenía datos), `MainActivity.kt:941-958`, `captures/d_presets.png`.

Inicio explica y ofrece «Crear demo» con subtítulo; Canales muestra un «+» de 120dp sin una sola palabra; Presets ofrece un botón que hace otra cosa (H01). Ninguno se parece al otro.

**Fix:** mismo molde para los tres: título, una línea de qué es, una acción primaria.

### H19 · Reordenar no tiene handle y el mensaje filtra implementación
`MainActivity.kt:824-876` + `MainActivity.kt:856-863`.

El drag arranca con long-press sobre la tarjeta, que también es «tap para abrir» y contiene toggles. Si la grilla no se midió todavía, sale «Deslizá la grilla primero»: una frase sobre el layout interno, no sobre lo que el usuario tiene que hacer. En Presets el long-press está en el `Column` que contiene los cuatro IconButton (`PresetsPanel.kt:204-219`), así que mantener apretado sobre el lápiz también arranca drag.

**Fix:** handle de arrastre visible a la derecha de cada tarjeta y sacar el snackbar técnico.

### H20 · El fader esconde dos gestos
`Fader.kt:46-49`, `Fader.kt:96-114` + `MainActivity.kt:272-286`.

Doble tap resetea a 0.5 sin que nada lo indique. El thumb es un rectángulo de 40dp con una línea de 2dp adentro; no hay marcas de mín/máx ni valor sobre el riel. En una mesa de luces el pulgar tiene que leerse de reojo.

**Fix:** thumb relleno de 24×40dp, marcas en los extremos y el valor pegado al thumb.

### H21 · La paleta usa el mismo verde para todo y mezcla semántica con marca
`captures/d_canales.png`, `XyPad.kt:40`, `ChannelModule.kt:418-443`, `MainActivity.kt:98-110`.

`#39FF14` es marca, tab activa, dirección OSC, valor del canal G, relleno del fader y botón primario. El canal G comparte color con el acento: cambiar el acento cambia el significado del canal. Hay un cuarto verde (pad XY, `#4ADE80`) y el rojo de error (`#FF5252`) es el mismo del canal R.

**Fix:** acento para acciones, escala neutra para datos y tintes de canal propios y documentados.

### H22 · Los controles custom no dan feedback al tocar
`Fader.kt:42-70`, `XyPad.kt:42-69`, `ToggleControl.kt:49-68`.

Son `Canvas` con `pointerInput`: sin ripple, sin escala, sin cambio de borde. El único que acusa recibo es el botón circular (el arco blanco). En el resto, tocar y no tocar se ve igual hasta que el valor se mueve.

**Fix:** `scale 0.985` en press sobre el control y un halo de 1dp en el borde.

### H23 · El botón primario de Ajustes mide 46dp
`captures/d_ajustes.png` + `SettingsPanel.kt:132-141`.

Los campos miden 56dp y el botón que importa, 46. Queda más fino que los inputs que lo rodean.

**Fix:** 56dp y mismo radio que los campos.

### H24 · La dirección OSC va en el color de acción
`captures/d_inicio.png` (`/canal1` en lima) + `MainActivity.kt:930-935`.

El texto más brillante de cada tarjeta es metadata de debug. Además verde = tocable en toda la app, y esa dirección no hace nada al tocarla.

**Fix:** dirección en gris, y sólo mostrarla cuando el prefijo no está vacío.

---

# P3

### H25 · Monospace en etiquetas de componente
`ChannelModule.kt:729-735`. La letra «R» va en Space Mono y el resto de la fila en Grotesk. Mezcla dos familias en la misma línea y usa mono para algo que no es una cifra.

**Fix:** letra en Grotesk con el tinte como único diferenciador; mono sólo para el número.

### H26 · Registro mezclado
`SettingsPanel.kt:186` («Tocar de nuevo…», infinitivo) contra `PresetsPanel.kt:159` («Creá…») y `MainActivity.kt:859` («Deslizá…»). Voseo en un lugar, neutro en otro.

**Fix:** voseo en todo el copy de acciones.

### H27 · Copy ambiguo en Ajustes
`SettingsPanel.kt:79` («Este celu»), `SettingsPanel.kt:88` («IP»), `SettingsPanel.kt:119` («Prefijo (opcional)»). «Este celu» está cortado, «IP» no dice de qué equipo y el prefijo no explica que se antepone a la dirección OSC.

**Fix:** «Este celular», «IP de la PC», «Prefijo de las direcciones (opcional)».

### H28 · «Mantener apretado» como switch
`ChannelModule.kt:513-531`. La etiqueta describe un gesto, no un modo. Lo que se elige es enclavado contra momentáneo.

**Fix:** dos opciones explícitas, «Momentáneo» y «Enclavado».

### H29 · El punto de estado sólo cambia de color
`MainActivity.kt:538-550`. Verde contra rojo sin texto ni ícono: indistinguible para daltonismo, y sin transición cuando cambia.

**Fix:** sumar ícono o texto corto («ok» / «error») al lado del punto.

### H30 · El icono de duplicar es FilterNone
`ChannelModule.kt:245-250`. Dos cuadrados superpuestos se leen «capa», no «duplicar». Es el único lugar de la app donde el usuario tiene que adivinar un ícono.

**Fix:** ícono ContentCopy y mismo glifo que el swipe de duplicado en la grilla.

### H31 · El contador de canales vive escondido
`MainActivity.kt:1207-1212`. «5/8» aparece sólo en la página «Agregar nuevo canal». En ningún otro lado se ve el tope ni cuántos canales tenés.

**Fix:** «5 de 8 canales» en el header de Inicio.

### H32 · El snackbar tapa la fila de tipos
Medido en dos dumps: el snackbar ocupa `[34,1953]-[1046,2088]` (`captures/dump-inicio.xml`) y los chips de tipo `[45,1950]-[1035,2085]` (`captures/dump-canales.xml`). Misma franja vertical, mismo lugar en el `Column`: mientras está el «Deshacer» no podés cambiar el tipo de canal.

**Fix:** snackbar por encima de la barra de tabs en vez de pegado a los controles.

### H33 · Tema de ventana claro
`AndroidManifest.xml:13`: `@android:style/Theme.Material.NoActionBar` sin `windowBackground` oscuro. Riesgo de flash blanco en el arranque en frío (código; no se pudo filmar el frame).

**Fix:** `Theme.Material.NoActionBar` con `android:windowBackground` = `#0A0A0A`.

---

# Medidas

Densidad 450: los px salen de los dumps y los dp de dividir por 2.8125. Lo marcado «código» no lo pude medir en pantalla.

| Elemento | px | dp | Veredicto |
|---|---|---|---|
| Tarjeta de Inicio | 473×465 | 168×165 | sobra |
| Fila de componente R/G | 222×100 | 79×35.5 | **bajo** |
| Fila de componente B | 222×135 | 79×48 | justo |
| Chips de tipo (Canales) | 198×135 | 70×48 | ok |
| Duplicar / borrar canal | 135×135 | 48×48 | ok |
| Ítem de nav | 252×225 | 90×80 | ok |
| Íconos de fila de preset | — | 40×40 | **bajo** (código) |
| «Probar canales» | — | 46 de alto | **bajo** (código) |

Contraste (calculado sobre `#0A0A0A`):

| Token | Ratio | Nota |
|---|---|---|
| `onSurface` #E2E2E2 | 15.3:1 | ok |
| `onSurfaceVariant` #A6A6A6 | 8.1:1 | ok |
| `primary` #39FF14 | 14.6:1 | ok, pero lima puro a 36sp es agresivo |
| Valor muteado (0.35 de #E2E2E2) | **2.7:1** | falla AA y hasta el 3:1 de texto grande |
| Tinte muteado (#39FF14 al 35%) | **2.3:1** | idem |

# Flows

| Flow | Estado | Por qué |
|---|---|---|
| Crear canal | Funciona, con fricción | «+» de 120dp sin texto (`MainActivity.kt:941-958`); al llegar a 8 el «+» desaparece sin explicación; duplicar en el tope no hace nada ni avisa (`MainActivity.kt:407-408`). |
| Aplicar preset | Roto en el arranque | Imposible crear el primero (H01). Si existiera: aplica sin feedback, mete IP y puerto de prepo (H05) y no hay «aplicado» en ningún lado. |
| Reordenar | Funciona a medias | Long-press sin handle, compite con tap y con los toggles (H19); en Presets compite con los iconos. Sin haptic ni fantasma de destino. |
| Exportar | Funciona | Comparte un JSON por el chooser del sistema. Sin confirmación de que salió, y si el usuario cancela el chooser no se entera. |
| Importar | Funciona | Diálogo con conteo de canales y presets: es el mejor texto de la app. Le falla el color (verde para reemplazar, rojo para cancelar, H12). |
| Probar canales | Funciona, miente | Snackbar «5/5 enviados» cuenta escrituras al socket, no recepción (H02). |

# Lo que está bien

- La página de canal a pantalla completa con carrusel es la decisión correcta para una mesa de control: un control grande por página.
- El color vivo del canal Color sobre negro se lee al instante; el swatch de 160dp funciona como preview.
- El voseo aparece («Creá», «Deslizá») y los nombres de canal se sanearon bien.
- El dump de accesibilidad expone labels decentes donde hay `contentDescription` (duplicar, borrar, sobrescribir, expandir).
- El diálogo de importar dice qué se va a reemplazar y con cuántos canales. Ese nivel de precisión debería estar en todos los destructivos.

# Orden sugerido

1. H01 (nadie puede usar Presets hoy).
2. H02, H04, H05, H06 (mentira de estado y pérdida de datos silenciosa).
3. H08, H10, H09 (controles que no se pueden agarrar o descubrir).
4. H13, H14, H15 (jerarquía y cabeceras: es lo que hace que se vea «de juguete»).
5. H21, H22, H20 (lenguaje visual y feedback táctil).
6. El resto.
