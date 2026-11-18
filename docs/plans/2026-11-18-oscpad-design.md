# oscpad — controlador OSC Android para TouchDesigner

Fecha: 2026-11-18
Estado: diseño aprobado, implementación no iniciada.

## Objetivo

App Android propia que reemplaza a TouchOSC para una pantalla: un botón on/off y un fader,
que mandan OSC por UDP a TouchDesigner 2025.32460 en la LAN.

## Alcance

Dentro: dos controles, configuración de destino (host, puerto, address por control, rango de
valores), encoder OSC propio, APK instalable por `adb`.

Fuera, explícito: editor de layouts, lectura de archivos `.tosc`, scripting, MIDI, TCP, mDNS/OSCQuery,
multi-pantalla, feedback desde TD (queda como fase 4 opcional).

## Contrato OSC

| Control | Mensaje | Valor |
|---|---|---|
| Botón | `/<addr-boton>` | `1.0` al encender, `0.0` al apagar |
| Fader | `/<addr-fader>` | float en `[min, max]` de la config |

Ambos son OSC 1.0 con un único argumento float32: `address\0` + `,f\0\0` + 4 bytes big-endian,
todo alineado a 4. Sin bundles ni timetags.

En TouchDesigner: **OSC In CHOP** con Network Port igual al puerto configurado en la app y
*OSC Address Scope* `/*`. El CHOP nombra los canales con el address y `/`→`_`, así que `/fader`
aparece como canal `fader`; con *Strip Prefix Segments* queda limpio si se usan prefijos tipo
`/oscpad/fader`.

## Stack

- Kotlin + Jetpack Compose, una Activity, sin DI ni navegación.
- Encoder OSC propio (~40 líneas) en vez de `com.illposed.osc:javaosc-core`: sin bundles no
  hay nada que ganar con la dependencia, y el encoder se testea en JVM.
- `DatagramSocket` UDP, un socket por destino, throttle 16 ms por control + envío forzado al soltar.
- `SharedPreferences` para la config. Defaults: puerto 8000 (el que usa la guía TouchOSC de
  Derivative), rango 0.0–1.0, addresses `/button` y `/fader`.
- Campos de config: **IP local de la PC** (destino, se precarga con la IP de la máquina al
  compilar la app y se muestra la IP del celular al lado para comparar subred), puerto, address
  del botón, address del fader, mínimo y máximo del fader. Un botón "probar" que manda los dos
  mensajes y muestra si el socket no pudo salir.
- Controles dibujados con `Canvas` (fader vertical, botón de dos estados). Sin librerías de knob.
- `KEEP_SCREEN_ON` en la Activity: uso en vivo, la pantalla no se apaga.

## Estructura

```
oscpad/
  settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew.bat
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/java/ar/matu/oscpad/
    MainActivity.kt      pantalla + estado
    Osc.kt               encoder + OscSender + Throttle
    Config.kt            data class + load/save
    Fader.kt             Canvas
    ToggleControl.kt     Canvas
    SettingsPanel.kt     host, puerto, addresses, rango
  app/src/test/java/ar/matu/oscpad/OscTest.kt
  tools/osc_listen.py    verificador UDP sin TD
  docs/plans/            este archivo
```

## Build sin Android Studio

Ni JDK ni SDK ni Gradle están instalados; `adb` sí (winget, platform-tools). Pasos:

1. `winget install EclipseAdoptium.Temurin.17.JDK` (17.0.21.9, verificado en winget).
2. Android command-line tools: zip de https://developer.android.com/studio#command-line-tools-only
   descomprimido en `C:\Android\cmdline-tools\latest` (`ANDROID_HOME=C:\Android`).
3. `sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0"` y aceptar licencias.
4. Gradle 8.11.1: zip de https://services.gradle.org/distributions/, `gradle.bat wrapper` una vez,
   después todo es `gradlew.bat assembleDebug`.
5. `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Debug se firma solo con el keystore
   de debug que genera AGP: instalable sin configurar nada. Inalámbrico: `adb pair` + `adb connect`.

Pines de versión: AGP 8.7.3, Kotlin 2.0.21, compileSdk/targetSdk 35, minSdk 26.

Salida si la instalación se traba: build en GitHub Actions y bajar el APK del artefacto. Un día
más de ciclo para cada cambio, así que es plan B.

## Fases

| # | Entrega | Check binario |
|---|---|---|
| 0 | Toolchain instalado + repo Gradle compila el esqueleto | `gradlew.bat assembleDebug` exit 0 y `adb install` OK |
| 1 | `Osc.kt` + `OscTest.kt` | `gradlew.bat test` verde y `python tools/osc_listen.py` imprime `/fader 0.5` al mandar desde un test JVM |
| 2 | Fader + botón + panel de config | mover el fader mueve el canal `fader` en OSC In CHOP de TD, con el puerto correcto |
| 3 | Persistencia + rangos + KEEP_SCREEN_ON | cerrar y reabrir la app conserva host, puerto, addresses y rango |
| 4 | Feedback desde TD (opcional) | `OSC Out CHOP` manda al puerto de escucha y el botón refleja el estado real |

## Riesgos

1. **Aislamiento de clientes en el WiFi.** Si el AP lo tiene activado, el celular no ve la PC.
   Se detecta en fase 2 y no se arregla desde la app: hotspot del celular o red sin aislamiento.
2. **Firewall de Windows** bloqueando UDP entrante al puerto de TD. Regla de entrada explícita.
3. **Doze / batería** con la app en segundo plano. En primer plano con `KEEP_SCREEN_ON` no aplica;
   si se necesita background, foreground service en fase 4.
4. **Puerto ocupado**: dos apps escuchando el mismo puerto en Windows falla en silencio para una.
   Verificar con `netstat -ano | Select-String 8000`.

## Verificación

- Encoder: test JUnit contra bytes conocidos (`/fader` + `,f` + `0.5` = 20 bytes).
- Transporte: `tools/osc_listen.py` (socket UDP, bind del puerto, imprime address y floats).
- Integración: OSC In CHOP de TD 2025.32460, mirar el canal y el valor; después OSC In DAT con
  `onReceiveOSC` si hace falta loguear address y args.
- Sin evidencia ejecutada no se declara nada terminado.
