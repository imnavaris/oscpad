# HANDOFF — oscpad

Controlador OSC Android (botón on/off + fader) para TouchDesigner 2025.32460.
Diseño completo: `docs/plans/2026-11-18-oscpad-design.md`.

## Estado

- Fase 0 cerrada: toolchain instalado y APK debug compilando.
- No instalado en ningún dispositivo todavía (`adb devices` vacío).

## Toolchain (sin Android Studio)

| Qué | Dónde |
|---|---|
| JDK 17.0.21 Temurin | `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot` |
| Android SDK | `C:\Android` (platforms;android-35, build-tools;35.0.0, platform-tools) |
| Gradle 8.11.1 | `C:\Android\gradle-8.11.1` |
| Env de usuario | `ANDROID_HOME=C:\Android`, `JAVA_HOME=<jdk>` |

Build: `.\gradlew.bat assembleDebug` → `app\build\outputs\apk\debug\app-debug.apk` (8.7 MB).
Instalar: `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
Logs: `adb logcat -s oscpad`.

## Próximo paso

Fase 1: `Osc.kt` (encoder float32 + `OscSender` + `Throttle`) y `OscTest.kt`; verificar con
`python tools/osc_listen.py 8000`. Después fase 2: UI (fader, botón, panel de config con IP de
la PC `192.168.0.112`, puerto, addresses, rango).

## Verificado

- `gradle --version` → Gradle 8.11.1, JVM 17.0.21.
- `assembleDebug` → BUILD SUCCESSFUL exit 0.
- `sdkmanager` instaló paquetes, `C:\Android\licenses\android-sdk-license` presente.

## Pendiente de decisión

- Registrar el proyecto en `01 - Proyectos/INDEX.md` del vault (el código vive fuera del vault).
