# AGENTS.md — oscpad

Controlador OSC Android → TouchDesigner. Kotlin + Compose Material3, un módulo.

## Toolchain (sin Android Studio)

- JDK 17 Temurin, SDK `C:\Android` (android-35, build-tools 35.0.0), Gradle wrapper 8.11.1.
- Env: `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`, `ANDROID_HOME=C:\Android`.
- Build: `.\gradlew.bat assembleRelease test --no-daemon` (exit 0 = verde).
- Release con R8 (~1.3MB). Keystore local en `C:\Android` + `local.properties` (ignorado, nunca commitear).
- Instalar: `adb install -r app\build\outputs\apk\release\app-release.apk`. Lanzar: `adb shell am start -W -n ar.matu.oscpad/.MainActivity`.

## Reglas duras

- **Cero comentarios en código.** Ni `//`, ni KDoc. La explicación va en el commit o el chat.
- UI en español rioplatense. Tema oscuro propio (`OscScheme` en MainActivity).
- Un commit por lote verde. Commits en español, formato `tipo: qué` + cuerpo con el porqué.
- No agregar dependencias sin motivo (material-icons-extended ya está por los iconos Save/Edit).

## Arquitectura

- `Config.kt`: modelo (`Channel` con `level..level4` + `mask` de componentes) + stores en SharedPreferences (`oscpad`). JSON tolerante (`ignoreUnknownKeys`).
- `Osc.kt`: `encodeFloat(s)` + `OscSender` (dedup de datagramas idénticos) + `Throttle`.
- `ChannelModule.kt`: página del canal + `OverviewCompRow` (fila letra+valor con toggle de máscara) + `axisTint(i)`.
- `MainActivity.kt`: tabs (Inicio grilla / Canales carrusel / Presets / Ajustes), orden manual por drag, feed de sensores, AnimatedContent entre tabs.
- `PresetsPanel.kt`: CRUD + reorder por long-press + chevron animado. Tap en fila = aplicar.
- `SensorSpec.kt`: catálogo (sin proximidad) + `SensorFeed` de 4 componentes.
- Tests JUnit en `app/src/test` (`.\gradlew.bat test`). 26 tests al cerrar este ciclo.

## Contrato OSC (no romper)

- Un mensaje multi-float por canal: xy=2, color RGB=3, sensor XYZM=4. `Channel.outputValues()` aplica la máscara (apagado = 0).
- Lado TD: OSC In CHOP, puerto 8000, UDP Messaging, scope `/*`, Strip 0, Queued on.
- Throttle 66ms (~15/s) + envío final al soltar. Red 192.168.1.x por DHCP: nunca hardcodear IPs salvo defaults.

## Verificación

- Build + tests en verde. Dumps: `adb shell uiautomator dump` + parseo de `text=`/`bounds=` (sin observer salvo pedido expreso).
- Gestos por adb: `draganddrop` para long-press+arrastrar (`swipe` común no dispara long-press).
- No mutar la data del usuario en pruebas (canales/presets reales). Tap en overview APLICA presets: no tocarlo en vivo.
