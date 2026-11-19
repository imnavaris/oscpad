# oscpad — reverse engineering / threat model (carril C)

APK: `app/build/outputs/apk/release/app-release.apk` (1.42 MB, v1.1.0 / versionCode 3). Solo análisis estático. APK copiado a `C:\Users\Usuario\AppData\Local\Temp\opencode\rev` (no sale de la máquina).

## Superficie de ataque

- Permisos: `INTERNET`, `VIBRATE`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `WAKE_LOCK`. Sin `READ/WRITE_EXTERNAL_STORAGE`, sin `MANAGE_EXTERNAL_STORAGE`, sin `READ_CONTACTS`. Superficie de permisos mínima — bueno.
- Componentes exportados: solo `MainActivity` (launcher, exported=true) y `ProfileInstallReceiver` (exported=true, actions INSTALL_PROFILE/SKIP_FILE/SAVE_PROFILE/BENCHMARK_OPERATION) — estándar de androidx.profileinstaller, sin datos de usuario.
- FileProvider: authorities `ar.matu.oscpad.fileprovider`, `exported=false`, `grantUriPermissions` activo. No se encontró `res/xml/filepaths.xml` en el APK (resources.arsc sin entry resoluble vía unzip); paths no inspeccionables sin aapt2. Asumir default del manifest del proyecto si existiera.
- `allowBackup`: **ausente** en el manifest → default `true` en Android. Consecuencia: `adb backup` (en devices ≤ Android 12 / con `adb backup` habilitado) puede extraer SharedPreferences + databases del app. **Explotable** con acceso físico + USB debugging.
- Backup de datos: las prefs del app contienen IP/puerto del host OSC (default detectado en dex: `192.168.0.112`). Un atacante con backup extrae la config del host objetivo.
- Tráfico: OSC sobre UDP puerto 8000, sin cifrado ni auth (OSC no tiene auth). Alcance LAN. Cualquiera en la misma red puede enviar paquetes OSC al host (spoofing de controles) y sniffear. **Explotable** en red LAN compartida.
- R8: ofuscación nominal. Classes `a`, `b`, ... presentes (paquete `a`, `b`, etc.) pero strings en claro, nombres de recursos y mensajes de error completos. No es protección real contra RE.
- Strings sensibles en dex: IP `192.168.0.112` (default), textos en español ("Tocar de nuevo para borrar canales", "Preset borrado", "Prefijo (opcional)", "Nuevo preset"). Sin API keys, tokens, passwords, endpoints HTTP. Sin secrets embebidos.

## Hallazgos por severidad

1. **[MEDIO] allowBackup=true por defecto** — backup extrae config del host (IP/puerto). Mitigación: `android:allowBackup="false"` en manifest.
2. **[MEDIO] OSC UDP sin auth/cifrado** — spoofing en LAN. Inherente al protocolo OSC; mitigar con red aislada / VLAN.
3. **[BAJO] R8 nominal** — strings en claro. No crítico (app sin secrets).
4. **[BAJO] FileProvider grantUriPermissions** — solo si paths amplios; no verificable sin filepaths.xml.
5. **[INFO] Sin secrets embebidos** — descartado con evidencia (Select-String sobre classes.dex).

## Lo que NO extrae un atacante

Sin credenciales, sin datos personales, sin storage externo. Solo config de host OSC en prefs.
