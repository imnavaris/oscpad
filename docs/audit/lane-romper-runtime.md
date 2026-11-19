# Lane B — stress runtime en dispositivo real

Fecha: 2026-11-19. App: `ar.matu.oscpad` v1.1.0 (release ya instalada, no recompilada).
Dispositivo: Xiaomi 25100RA69G (`ofzh7drczxz9l74d`), Android 16, 1080x2392.
Herramientas: `adb` (`uiautomator dump`, `input tap`, `input draganddrop`), `tools/osc_listen.py` en :8000.

Criterio: cada escenario sobrevive con el mismo PID de proceso y sin `FATAL EXCEPTION` / `ANR in` de la app.

## Resultado

Sobrevive todos los escenarios. Ningún crash, ANR ni restart del proceso atribuible a la app.
PID 18848 estable antes y después de navegación, rotación, background/foreground, gestos y envío OSC.

| Escenario | Resultado | Evidencia |
|---|---|---|
| 80 taps rápidos entre las 4 tabs (inicio/Canales/Presets/Ajustes), 10.8 s | OK | PID 18848, sin crash buffer, sin ANR, dump final consistente |
| 40 taps rápidos en celdas del overview | OK funcional, con efecto colateral (ver Incidente) | PID 18848, sin crash |
| 30 taps seguros en overview (celdas izq.) + transición a Canales con `animateScrollToItem` | OK | PID 18848, sin crash |
| Rotación retrato→paisaje→retrato en cada tab (4 ciclos) | OK | PID 18848, `rotation="0"` al volver, configuración intacta |
| Rotación rápida x5 (300 ms por giro) sobre Inicio | OK | PID 18848, sin relayout roto |
| Background/foreground x10 (HOME → `am start`, 200 ms y 2 s alternados) | OK | PID 18848, `topResumedActivity=…oscpad/.MainActivity`, WifiLock único (`oscpad:live`), sin acumulación |
| Back x6 tras navegar (historial) | OK: cierra la activity por diseño | Sin FATAL/ANR. `pidof` devolvió 16164 justo después y 18848 al relanzar; no encontré líneas de arranque de proceso que expliquen el salto, así que no afirmo restart. Pantalla se apagó después (Dozing) |
| Ajustes → Probar canales con listener UDP | OK | 5/5 datagramas recibidos, valores coinciden con el contrato |
| Drag-reorder en grilla de inicio (long-press + arrastre, `input draganddrop`) | OK: reordena y persiste; restaurado a mano | Orden pasó a canal2,canal4,canal1,canal3,canal5 y volvió al original |
| Aplicar presets repetido y rápido | N/A: 0 presets en el dispositivo | La tab Presets muestra "Creá tu primer preset"; crear presets estaba prohibido |

### Proveniencia OSC (Probar canales)

```
192.168.1.84 /canal1 [0.0, 0.2403000295162201, 1.0]
192.168.1.84 /canal2 [1.0, 0.0, 0.5]
192.168.1.84 /canal4 [0.5]
192.168.1.84 /canal3 [0.5]
192.168.1.84 /canal5 [0.5]
```

3 floats para color, 1 float para fader: contrato respetado. IP de la PC 192.168.1.35 = destino configurado.

### Jank (acumulado desde reset previo a los taps de overview)

```
Total frames rendered: 5808      Janky frames: 32 (0.55%)
50th: 9ms   90th: 14ms   95th: 17ms   99th: 44ms
Number Missed Vsync: 8    Slow UI thread: 30    Slow issue draw commands: 23
```

Sin frames perdidos por encima de lo esperable en una app Compose con animaciones de tab.
Memoria al cierre: TOTAL PSS 70.988 KB, Java Heap 7.296 KB / 35.584 KB.

## Incidente y limpieza (declarado)

1. Los 40 taps del overview navegan a Canales; los que cayeron en x≥765 y 270<y<405 golpearon el botón **Duplicar canal** de la página destino. Se crearon 2 canales (`canal1_2`, `canal1_2_2`) persistidos en `ChannelStore`.
   - Limpieza: navegué a cada uno y usé **Borrar canal** (sin Deshacer). Estado final idéntico al baseline: canal1 0.00/0.24/1.00, canal2 1.00/0.00/0.50, canal4 0.50, canal3 0.50, canal5 0.50.
   - Secuela menor: el contador `nextId` avanzó 2. Sin efecto visible; los ids sólo crecen.
2. El drag-reorder persistió un orden nuevo antes de restaurarlo con un segundo drag. Verificado por dump: mismo orden y mismos `bounds` que el baseline.
3. El proceso de `uiautomator` crasheó una vez (`UiAutomationService … already registered!`, pid 23396). Es el tool de dump, no la app: `FATAL EXCEPTION` con package `com.android.uiautomator`, sin línea `Process: ar.matu.oscpad`.
4. Tras los BACK el equipo se durmió y quedó en el patrón de desbloqueo. No lo desbloqueé (requiere credencial del usuario); las verificaciones posteriores son las de logcat.

## No cubierto

- Presets: aplicar, reordenar, expandir (no hay presets; crearlos estaba prohibido).
- Edición de Ajustes (IP/puerto/prefijo), export e import reales.
- Cancelación de long-press y reorder con lista llena (>5 presets, filtro activo).
- Caída de red / destino inalcanzable (el listener siempre respondió).
- Orientación por sensor físico: usé `settings put system user_rotation` porque `accelerometer_rotation` ya estaba en 0; al terminar quedó 0/0 como estaba.
