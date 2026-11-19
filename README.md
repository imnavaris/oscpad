# oscpad

Controlador táctil desde el celular para **TouchDesigner**: faders, botones, pads XY, sensores y colores que llegan como canales OSC.

## Qué hace

- Canales dinámicos: fader, botón (latch o momentáneo), pad XY, sensores del celu (acelerómetro, giro, magnetómetro, luz, presión) y color RGB.
- Cada canal multi-valor muestra sus componentes (X/Y, R/G/B, X/Y/Z/M) y permite **mutearlos tocándolos** (se grisán y dejan de enviar).
- Presets: guardá, renombrá, reordená y aplicá sets completos con un tap.
- Vista Inicio (grilla reordenable) + Canales (pantalla completa por canal) + Ajustes (IP, puerto, rangos).

## Conexión con TouchDesigner

1. Misma WiFi en celu y PC. En la app, Ajustes → IP = la IP de tu PC, puerto 8000.
2. En Windows, abrir el puerto (admin): `New-NetFirewallRule -DisplayName "oscpad" -Direction Inbound -Protocol UDP -LocalPort 8000 -Action Allow`
3. En TD: CHOP `OSC In`, `Network Port: 8000`, `Protocol: UDP Messaging`, `OSC Address Scope: /*`, `Queued: on`.
4. En la app tocá **Probar canales**: tienen que aparecer los canales (`/canal1`, …). Movelos y listo.

## Compilar

Sin Android Studio: JDK 17 + SDK Android + `.\gradlew.bat assembleRelease` (ver `AGENTS.md`).
