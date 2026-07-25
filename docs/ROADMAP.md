# FluffyBeep → Element X Android: Análisis de Viabilidad y Roadmap

## Resumen Ejecutivo

Portar las funcionalidades de FluffyBeep a Element X Android es **técnicamente viable y manejable** si limitamos el scope a las Categorías A y B (lógica de protocolo + UI). Sin la Categoría C (push/crypto/cache), desaparecen las fases más riesgosas y el proyecto se convierte en un trabajo fundamentalmente de **Kotlin + Jetpack Compose**, sin necesidad de tocar el Rust SDK en profundidad.

> [!IMPORTANT]
> FluffyBeep modifica **FluffyChat** (Flutter/Dart, Matrix Dart SDK).
> Element X Android usa **Kotlin + Jetpack Compose + Matrix Rust SDK**.
> No hay código reutilizable directamente — la lógica debe reimaginarse en Kotlin.

---

## Análisis Comparativo de Arquitecturas

| Aspecto | FluffyChat (FluffyBeep) | Element X Android |
|---|---|---|
| **Lenguaje** | Dart (Flutter) | Kotlin (Jetpack Compose) |
| **SDK Matrix** | `matrix` Dart SDK | Matrix Rust SDK (via UniFFI) |
| **Estado** | Provider + streams | Molecule + StateFlow |
| **Navegación** | GoRouter | Appyx |
| **Almacenamiento** | SQLite + SharedPreferences | Rust SDK interno + DataStore |
| **UI** | Flutter Widgets | Jetpack Compose + Compound Design System |
| **Licencia** | AGPL-3.0 | AGPL-3.0 (dual con Element Commercial) |
| **Acceso al protocolo** | Directo (Dart → Matrix API) | Indirecto (Kotlin → FFI → Rust → Matrix API) |

---

## Inventario de Funcionalidades FluffyBeep

### Categoría A: Lógica de Protocolo/Datos (IN SCOPE)
Operan a nivel del protocolo Matrix. Son framework-agnósticas conceptualmente.

| Feature | Complejidad | Descripción |
|---|---|---|
| Detección de bridge bots | 🟢 Baja | Parsear `@whatsapp_xxx:beeper.com` del member ID |
| Detección de red (`networkMap`) | 🟢 Baja | Mapeo de prefijos MXID → red |
| Fake DM detection | 🟡 Media | Heurística: ≤3 miembros + sin `m.room.name` + contacto real |
| Beeper Labels (`com.beeper.labels`) | 🟡 Media | Leer/escribir Account Data custom |
| Sticker pack management (`im.ponies.user_emotes`) | 🟡 Media | Upload + Account Data manipulation |
| Hidden networks | 🟢 Baja | Persistencia en Account Data |
| Manual read receipts | 🟢 Baja | Flag local que inhibe `setReadMarker` |

### Categoría B: UI/UX (IN SCOPE)
Requieren reimplementación total en Jetpack Compose.

| Feature | Complejidad | Descripción |
|---|---|---|
| Bridge display names en chat list | 🟡 Media | Reemplazar nombre vanilla con lógica Beeper |
| Bridge avatars override | 🟡 Media | Usar avatar del contacto real, no del bot |
| Network badge indicators | 🟢 Baja | Composable con ícono de red en cada chat |
| Virtual spaces por red | 🔴 Alta | Sidebar con "espacios virtuales" dinámicos por bridge |
| Label editor dialog | 🟡 Media | Bottom sheet en Compose |
| Settings "Parches de Beeper" | 🟡 Media | Nueva pantalla en el árbol de settings |
| Native app fallbacks (tel:, instagram://) | 🟢 Baja | Android Intents |

### ~~Categoría C: Infraestructura/Sistema~~ — **FUERA DE SCOPE**
> Push/crypto/cache management descartados. Element X ya provee estas funcionalidades de forma nativa. No vale la pena reimplementarlas.

---

## Restricciones Arquitectónicas

### 🟡 El "Rust SDK Wall" — Impacto reducido

Sin Categoría C, el Rust SDK deja de ser un bloqueante. Las únicas dependencias:

1. **Room member data** — ¿Se expone el MXID de cada miembro y su display name/avatar via FFI? (Muy probable: es funcionalidad básica).
2. **Account Data custom** (`com.beeper.labels`, `im.ponies.user_emotes`) — ¿Hay API para leer/escribir Account Data arbitrario? (Muy probable).
3. **Room display name override** — ¿Se puede interceptar el nombre/avatar en la lista? Esto es UI pura en Compose.

Estos puntos son APIs estándar que cualquier cliente Matrix necesita — Element X ya las usa internamente.

### 🟡 Decisiones Fork vs. Upstream

| Opción | Pros | Contras |
|---|---|---|
| **Fork Element X** (estrategia actual) | Control total | Merge conflicts periódicos con upstream |
| **Plugin/overlay approach** | Menor fricción | Limitado por las APIs expuestas |

---

## Roadmap

### Fase 0: Preparación y PoC (2-3 semanas)
> Objetivo: Confirmar viabilidad técnica con pruebas de concepto

- [ ] Configurar entorno de desarrollo Element X (clonar, compilar con Rust SDK)
- [ ] Auditar APIs del Rust SDK expuestas para room members y Account Data
- [ ] PoC: módulo `beeper-bridge-detector` que detecte bridge rooms y muestre un badge
- [ ] Confirmar que se puede interceptar display name/avatar en `RoomListItem` (Compose)

---

### Fase 1: Núcleo de Detección de Bridges (3-4 semanas)
> Objetivo: Infraestructura base para identificar y categorizar rooms bridgeados

- [ ] Crear módulo `features/beeperbridge/`
  - `api/`: `BeeperBridgeService`, `BeeperRoomData`, `BeeperNetwork`
  - `impl/`: implementación con Rust SDK bindings
- [ ] Implementar `BeeperRoomDataProvider`
  - Detección de bots por MXID pattern
  - Detección de red por prefijo
  - Fake DM heurística
  - Cache en memoria con invalidación por sync
- [ ] Persistencia de cache con DataStore (Kotlin)
- [ ] Tests unitarios para toda la lógica de detección

---

### Fase 2: Integración Visual Básica (3-4 semanas)
> Objetivo: Los chats bridgeados se ven correctamente en la lista

- [ ] Override de display names en `RoomListItem`
- [ ] Override de avatares (contacto real vs bot)
- [ ] `@Composable NetworkBadge(network: BeeperNetwork)` integrado en lista y app bar
- [ ] Compound Design System integration (colores de red como tokens)

---

### Fase 3: Virtual Spaces y Labels (4-5 semanas)
> Objetivo: Organización de chats por red y etiquetas custom

- [ ] Beeper Labels engine (CRUD sobre `com.beeper.labels`)
- [ ] Virtual Spaces en sidebar (por red detectada)
- [ ] Filtrado de room list por espacio virtual seleccionado
- [ ] Label Editor (Compose Bottom Sheet)
- [ ] Settings screen "Parches de Beeper"

---

### Fase 4: Funcionalidades de Chat Avanzadas (3-4 semanas)
> Objetivo: Funcionalidades específicas de bridge dentro de los chats

- [ ] WhatsApp sticker → `im.ponies.user_emotes`
- [ ] Native app fallbacks (tel:, instagram://, etc.)
- [ ] Manual read receipts toggle
- [ ] Filtrado de last real message (skip state/bot events)

---

### Fase 5: Pulido y Estabilidad (2-3 semanas)
> Objetivo: Robustez y performance

- [ ] Performance benchmark con 1000+ rooms bridgeados
- [ ] Graceful degradation para cuentas no-Beeper
- [ ] Tests de integración por red (WhatsApp, Telegram, Instagram…)

---

## Estimación de Esfuerzo

| Fase | Duración | Dificultad | Riesgo |
|---|---|---|---|
| Fase 0: Preparación & PoC | 2-3 sem | 🟡 Media | 🟡 Medio |
| Fase 1: Detección de bridges | 3-4 sem | 🟡 Media | 🟢 Bajo |
| Fase 2: Visual básico | 3-4 sem | 🟡 Media | 🟢 Bajo |
| Fase 3: Spaces/Labels | 4-5 sem | 🟡 Media | 🟡 Medio |
| Fase 4: Chat avanzado | 3-4 sem | 🟢 Baja | 🟢 Bajo |
| Fase 5: Pulido | 2-3 sem | 🟢 Baja | 🟢 Bajo |
| **TOTAL** | **~17-23 semanas** | | |

> [!TIP]
> MVP funcional (Fases 0→1→2): ~8-11 semanas. Permite validar el approach antes de continuar.

---

## Lo que obtenés de Element X vs FluffyBeep

| Element X | FluffyBeep (FluffyChat) |
|---|---|
| ✅ Sliding Sync (login instantáneo) | ❌ Sync lento en cuentas grandes |
| ✅ Performance nativa (Rust SDK) | 🟡 Dart SDK más lento |
| ✅ Mantenimiento upstream (Element HQ) | 🟡 Solo mantenimiento propio |
| ✅ Compound Design System profesional | 🟡 Material Design base |
| ✅ Kotlin/Compose (más adoptado en Android) | 🟡 Flutter (cross-platform pero más niche) |
