# Gap analysis oscpad — Carril E

Fuente: `docs/plans/2026-11-18-oscpad-design.md`, README, AGENTS, HANDOFF, código `app/src/main/java/ar/matu/oscpad/`.

## Bucket 1 — prometido en el diseño y no hecho

| # | Item | Valor/Costo | Deps | Evidencia |
|---|---|---|---|---|
| 1 | Feedback desde TD (fase 4): OSC Out → botón refleja estado real | L | listener UDP + threading | diseño §Fases fila 4; no hay `receive()`/OscListener en Osc.kt (83 líneas) |
| 2 | IP local de la PC precargada al compilar + IP del celu al lado | M | buildConfigField | diseño §Stack "se precarga con la IP de la máquina al compilar"; SettingsPanel.kt no la muestra |
| 3 | Encoder test JVM imprime `/fader 0.5` vía tools/osc_listen.py | S | tools/osc_listen.py | diseño §Fases fila 1; verificar existencia |

## Bucket 2 — estándar de categoría (TouchOSC/MK2) que falta

| # | Item | Valor/Costo | Deps | Evidencia |
|---|---|---|---|---|
| 4 | mDNS/OSCQuery auto-discovery del host | L | dependencia nueva | diseño §Fuera explícito; SettingsPanel.kt pide IP manual |
| 5 | TCP + bundles/timetags OSC | L | encoder | diseño §Fuera explícito (solo UDP single float) |
| 6 | Editor de layouts / multi-pantalla | L | nueva arquitectura | diseño §Fuera explícito |
| 7 | Proximidad / sensor de proximidad | S | SensorSpec.kt | CHANGELOG "proximidad eliminada"; catalog sin TYPE_PROXIMITY |
| 8 | Lectura de archivos .tosc | L | parser | diseño §Fuera explícito |

## Bucket 3 — deuda y QoL visto en código

| # | Item | Valor/Costo | Deps | Evidencia |
|---|---|---|---|---|
| 9 | MainActivity 1221 líneas — monolito, sin DI | M | refactor | MainActivity.kt |
| 10 | Sin tests de UI / instrumentados | M | androidTest | solo app/src/test (ChannelTest, OscTest) |
| 11 | Sin foreground service para background | M | Fase 4 | diseño Riesgo 3 |
| 12 | Config backup .bak rescatar — verificar restore | S | Config.kt | git log "rescate desde .bak" |
| 13 | Throttle hardcoded 66ms sin config | S | Osc.kt | AGENTS contrato |
| 14 | Sin i18n (solo es-AR) | M | resources | AGENTS "UI en español rioplatense" |

## Ranking por valor/costo (S primero)

S: 3, 7, 12, 13 → M: 2, 9, 10, 11, 14 → L: 1, 4, 5, 6, 8

## Verificación

- `git log --oneline` + `Select-String` sobre el repo. Read-only. No se modificó código.