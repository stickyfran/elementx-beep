# FluffyBeep → Element X Android: Análisis de Viabilidad y Roadmap Detallado

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
| Hidden Networks | 🟢 Baja | Persistencia en Account Data |
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

## CI/CD: GitHub Action para Build de APK

> [!IMPORTANT]
> Esta GitHub Action permite compilar y testear APKs automáticamente en cada push y PR,
> sin necesidad de tener configurado un entorno Android local.

### Configuración Requerida

#### Secrets del Repositorio (Settings → Secrets → Actions)
Para **debug builds** no se necesitan secrets. Para release builds futuros:
- `KEYSTORE_BASE64` — Keystore codificado en base64
- `KEYSTORE_PASSWORD` — Password del keystore
- `KEY_ALIAS` — Alias de la key
- `KEY_PASSWORD` — Password de la key

#### Archivo: `.github/workflows/build-apk.yml`

```yaml
name: Build Debug APK

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  workflow_dispatch:  # Permite ejecución manual desde la UI de GitHub

concurrency:
  group: build-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Build & Test
    runs-on: ubuntu-latest
    timeout-minutes: 60

    steps:
      # ── 1. Checkout ──────────────────────────────────────────────
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history para versionado

      # ── 2. Cache de Gradle ───────────────────────────────────────
      - name: Cache Gradle dependencies
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle.kts', '**/gradle.properties', '**/gradle-wrapper.properties') }}
          restore-keys: |
            gradle-${{ runner.os }}-

      # ── 3. Setup JDK 17 ─────────────────────────────────────────
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      # ── 4. Setup Android SDK ─────────────────────────────────────
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      # ── 5. Hacer Gradle ejecutable ───────────────────────────────
      - name: Make Gradle executable
        run: chmod +x ./gradlew

      # ── 6. Lint check (ktlint + detekt) ─────────────────────────
      - name: Run lint checks
        run: ./gradlew ktlintCheck detekt
        continue-on-error: true  # No bloquear build por lint warnings iniciales

      # ── 7. Unit tests ───────────────────────────────────────────
      - name: Run unit tests
        run: ./gradlew test --no-daemon

      # ── 8. Build Debug APK ──────────────────────────────────────
      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon

      # ── 9. Upload APK como artifact ─────────────────────────────
      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: elementx-beep-debug-${{ github.sha }}
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 14

      # ── 10. Upload test results ─────────────────────────────────
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results-${{ github.sha }}
          path: '**/build/reports/tests/'
          retention-days: 7

      # ── 11. Comentar en PR con link de descarga ─────────────────
      - name: Comment PR with APK link
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const { owner, repo } = context.repo;
            const run_id = context.runId;
            const body = `## 📱 APK de prueba disponible\n\n` +
              `El build se completó exitosamente.\n\n` +
              `**Descargá el APK desde:** [Artifacts de este workflow run](https://github.com/${owner}/${repo}/actions/runs/${run_id})\n\n` +
              `> Buscá el artifact \`elementx-beep-debug-${context.sha.substring(0, 7)}\`\n\n` +
              `Commit: \`${context.sha.substring(0, 7)}\``;
            github.rest.issues.createComment({
              owner, repo,
              issue_number: context.issue.number,
              body
            });
```

> [!TIP]
> Para ejecutar el build manualmente, andá a **Actions → Build Debug APK → Run workflow**.
> El APK aparece como artifact descargable en cada run.

---

## Roadmap Detallado: Paso a Paso

### Fase 0: Preparación y PoC (2-3 semanas)
> **Objetivo**: Confirmar viabilidad técnica con pruebas de concepto

#### Paso 0.1: Setup del entorno de desarrollo
- [ ] **0.1.1** Clonar el repo upstream de Element X Android:
  ```bash
  git clone https://github.com/element-hq/element-x-android.git
  cd element-x-android
  ```
- [ ] **0.1.2** Configurar remotes para el fork:
  ```bash
  git remote add beep https://github.com/<tu-org>/elementx-beep.git
  git checkout -b beep/develop
  ```
- [ ] **0.1.3** Instalar prerrequisitos:
  - Android Studio (última versión estable)
  - JDK 17 (Temurin/Adoptium)
  - Android SDK + NDK r25+ (via SDK Manager)
  - Rust toolchain (`rustup`) con targets Android:
    ```bash
    rustup target add aarch64-linux-android x86_64-linux-android
    ```
- [ ] **0.1.4** Compilar el Rust SDK con el script incluido:
  ```bash
  ./tools/sdk/build-rust-sdk
  ```
- [ ] **0.1.5** Compilar y correr Element X vanilla en un emulador o device para verificar que el build funciona.
- [ ] **0.1.6** Configurar la GitHub Action `build-apk.yml` (ver sección CI/CD arriba) y hacer el primer push para verificar el build en CI.

#### Paso 0.2: Auditoría de APIs del Rust SDK
Documentar hallazgos en `docs/PHASE0_FINDINGS.md`.

- [ ] **0.2.1** Buscar la API de Room Members:
  - Localizar en los bindings Kotlin generados por UniFFI: `room.members()`, `room.membersNoSync()`, etc.
  - Verificar que se exponen: `userId`, `displayName`, `avatarUrl`, `membership` para cada miembro.
  - **Buscar en**: `libraries/matrix/impl/src/main/kotlin/` y los bindings generados.
- [ ] **0.2.2** Buscar la API de Account Data:
  - Buscar métodos como `client.accountData(eventType)`, `client.setAccountData(eventType, content)`.
  - Verificar si acepta tipos custom como `com.beeper.labels`.
  - **Si no existe**: investigar si se puede usar la REST API directamente como fallback (`/_matrix/client/v3/user/{userId}/account_data/{type}`).
- [ ] **0.2.3** Buscar la API de Room State:
  - Verificar acceso a `m.room.name` state event por room.
  - Verificar acceso a custom state events.
- [ ] **0.2.4** Analizar `RoomListRoomSummary` y `RoomListItem` Composable:
  - Verificar si `name` y `avatarUrl` son mutables/overrideables.
  - Identificar si hay slot API, ViewModel hooks, o `Provider` pattern.
  - **Buscar en**: `features/roomlist/impl/src/main/kotlin/`
- [ ] **0.2.5** Analizar el sistema de filtrado de Room List:
  - ¿Dónde se filtra la lista de rooms? ¿Hay `RoomListFilter` interface?
  - ¿Se puede inyectar un filtro custom sin forkear?
- [ ] **0.2.6** Documentar conclusiones y decisión go/no-go en `PHASE0_FINDINGS.md`.

#### Paso 0.3: Proof of Concept — Bridge Badge
- [ ] **0.3.1** Crear el módulo Gradle `features/beeperbridge/api/build.gradle.kts`:
  ```kotlin
  plugins {
      id("io.element.android-library")
  }
  android { namespace = "io.element.android.features.beeperbridge.api" }
  dependencies {
      // Matrix SDK types que necesitemos exponer
  }
  ```
- [ ] **0.3.2** Crear `features/beeperbridge/impl/build.gradle.kts` con dependencias a `api` y al Rust SDK.
- [ ] **0.3.3** Crear `features/beeperbridge/test/build.gradle.kts` con dependencias a `api`.
- [ ] **0.3.4** Registrar los 3 módulos en `settings.gradle.kts`:
  ```kotlin
  include(":features:beeperbridge:api")
  include(":features:beeperbridge:impl")
  include(":features:beeperbridge:test")
  ```
- [ ] **0.3.5** Implementar una versión hardcodeada del `BeeperRoomDataProvider` que detecte un MXID de WhatsApp y muestre un badge verde en la UI.
- [ ] **0.3.6** Verificar el PoC en el emulador: ¿se ve el badge? ¿Funciona el build en CI?
- [ ] **0.3.7** Compilar el APK de prueba y distribuirlo via GitHub Actions artifact.

> **Criterio de éxito Fase 0**: El build compila, el badge de red aparece en al menos una sala bridgeada, y la GitHub Action produce un APK descargable.

---

### Fase 1: Núcleo de Detección de Bridges (3-4 semanas)
> **Objetivo**: Infraestructura base para identificar y categorizar rooms bridgeados

#### Paso 1.1: Modelo de datos completo (`api/`)

- [ ] **1.1.1** Expandir `BeeperNetwork.kt` con propiedades:
  ```kotlin
  enum class BeeperNetwork(
      val displayName: String,
      val colorHex: String,
      val iconResId: Int  // drawable resource
  ) {
      WHATSAPP("WhatsApp", "#25D366", R.drawable.ic_whatsapp),
      INSTAGRAM("Instagram", "#E1306C", R.drawable.ic_instagram),
      TELEGRAM("Telegram", "#2AABEE", R.drawable.ic_telegram),
      SIGNAL("Signal", "#3A76F0", R.drawable.ic_signal),
      DISCORD("Discord", "#5865F2", R.drawable.ic_discord),
      FACEBOOK("Facebook Messenger", "#00B2FF", R.drawable.ic_facebook),
      SLACK("Slack", "#4A154B", R.drawable.ic_slack),
      GOOGLECHAT("Google Chat", "#34A853", R.drawable.ic_googlechat),
      UNKNOWN("Unknown", "#808080", R.drawable.ic_bridge_generic)
  }
  ```
- [ ] **1.1.2** Expandir `BeeperRoomData.kt` con todos los campos necesarios:
  ```kotlin
  data class BeeperRoomData(
      val network: BeeperNetwork,
      val isFakeDm: Boolean,
      val botMxid: String? = null,
      val realContactMxid: String? = null,
      val overrideDisplayName: String? = null,
      val overrideAvatarUrl: String? = null,
      val networkKey: String? = null,
      val fromCache: Boolean = false
  )
  ```
- [ ] **1.1.3** Crear `BeeperLabel.kt`:
  ```kotlin
  data class BeeperLabel(
      val id: String,       // UUID
      val title: String,
      val emoji: String? = null,
      val roomIds: List<String>,
      val isShownInInbox: Boolean = true,
      val createdAt: Long
  )
  ```
- [ ] **1.1.4** Expandir `BeeperBridgeService.kt` con la API completa:
  ```kotlin
  interface BeeperBridgeService {
      fun isEnabled(): Boolean
      fun getRoomData(roomId: String): BeeperRoomData?
      fun getNetworkForRoom(roomId: String): BeeperNetwork?
      fun isFakeDm(roomId: String): Boolean
      fun getLabels(): List<BeeperLabel>
      fun getHiddenNetworks(): Set<String>
      suspend fun invalidateCache()
      suspend fun refreshRoomData(roomId: String)
  }
  ```

#### Paso 1.2: NetworkMap y detección de bots (`impl/`)

- [ ] **1.2.1** Crear `BeeperNetworkMap.kt`:
  ```kotlin
  object BeeperNetworkMap {
      // Mapeo de prefijo MXID → BeeperNetwork
      private val prefixMap = mapOf(
          "whatsapp" to BeeperNetwork.WHATSAPP,
          "whatsappgo" to BeeperNetwork.WHATSAPP,
          "instagram" to BeeperNetwork.INSTAGRAM,
          "instagramgo" to BeeperNetwork.INSTAGRAM,
          // ... etc para cada red + variante "go"
      )

      fun detectNetwork(userId: String): BeeperNetwork? { ... }
      fun isBeeperBot(userId: String): Boolean { ... }
      fun getBaseNetworkKey(key: String): String { ... }
  }
  ```
- [ ] **1.2.2** Implementar `isBeeperBot()`:
  - Lógica: `localpart.endsWith("bot") && userId.contains("beeper")`
  - Considerar dominios: `beeper.com`, `beeper.local`, etc.
- [ ] **1.2.3** Implementar `detectNetwork()`:
  - Extraer localpart del MXID (`@prefix_xxx:domain`)
  - Matchear el prefijo contra `prefixMap`
  - Normalizar variantes Go: `whatsappgo` → `whatsapp`

#### Paso 1.3: Fake DM Detection (`impl/`)

- [ ] **1.3.1** Crear `FakeDmDetector.kt`:
  ```kotlin
  class FakeDmDetector(
      private val networkMap: BeeperNetworkMap
  ) {
      fun analyze(
          roomName: String?,
          members: List<RoomMember>,
          localUserId: String
      ): FakeDmResult { ... }
  }

  data class FakeDmResult(
      val isFakeDm: Boolean,
      val botMxid: String?,
      val contactMxid: String?,
      val network: BeeperNetwork?
  )
  ```
- [ ] **1.3.2** Implementar la heurística:
  1. Si `roomName` no es null/vacío → **no es fake DM**
  2. Contar miembros activos (join/invite). Si > 3 → **no es fake DM**
  3. Buscar un bot entre los miembros no-locales (`isBeeperBot()`)
  4. Buscar un contacto real (no-bot, no-local)
  5. Si bot encontrado + contacto real → **es fake DM**
- [ ] **1.3.3** Manejar el edge case de primera sincronización:
  - Si encontramos bot pero no contacto y hay ≤ 2 miembros → marcar como `incomplete`
  - Reintentar en el siguiente sync

#### Paso 1.4: BeeperRoomDataProvider completo (`impl/`)

- [ ] **1.4.1** Implementar `BeeperRoomDataProvider` integrando NetworkMap + FakeDmDetector:
  ```kotlin
  class BeeperRoomDataProvider @Inject constructor(
      private val matrixClient: MatrixClient,
      private val networkMap: BeeperNetworkMap,
      private val fakeDmDetector: FakeDmDetector
  ) : BeeperBridgeService {
      private val cache = ConcurrentHashMap<String, BeeperRoomData>()

      override fun getRoomData(roomId: String): BeeperRoomData? {
          return cache.getOrPut(roomId) { computeRoomData(roomId) }
      }

      private fun computeRoomData(roomId: String): BeeperRoomData? {
          // 1. Obtener miembros del room via Rust SDK
          // 2. Detectar bot y red
          // 3. Ejecutar heurística Fake DM
          // 4. Calcular overrides de nombre/avatar
          // 5. Cachear resultado
      }
  }
  ```
- [ ] **1.4.2** Implementar `isEnabled()`:
  - Verificar si el usuario logueado tiene al menos un room con un bridge bot de Beeper.
  - Cachear el resultado tras primera detección.
- [ ] **1.4.3** Implementar invalidación de cache:
  - Escuchar eventos de sync (nuevos miembros, cambios de estado)
  - Invalidar rooms afectados
- [ ] **1.4.4** Agregar DataStore persistence para el cache (opcional, mejora performance).

#### Paso 1.5: Filtrado de último mensaje real

- [ ] **1.5.1** Crear `BeeperMessageFilter.kt`:
  ```kotlin
  object BeeperMessageFilter {
      private val realEventTypes = setOf(
          "m.room.message",
          "m.room.encrypted",
          "m.sticker"
      )

      fun isRealMessage(event: TimelineEvent): Boolean {
          if (event.type.startsWith("com.beeper.")) return false
          return event.type in realEventTypes
      }

      fun getLastRealMessage(events: List<TimelineEvent>): TimelineEvent? {
          return events.lastOrNull { isRealMessage(it) }
      }
  }
  ```

#### Paso 1.6: Tests unitarios

- [ ] **1.6.1** Tests para `BeeperNetworkMap`:
  - `@whatsapp_123:beeper.com` → WHATSAPP
  - `@whatsappgobot:beeper.com` → WHATSAPP (normalización Go)
  - `@instagram_abc:beeper.com` → INSTAGRAM
  - `@user:matrix.org` → null (no es Beeper)
  - `@signalbot:beeper.com` → isBeeperBot = true
  - `@signal_user:beeper.com` → isBeeperBot = false
- [ ] **1.6.2** Tests para `FakeDmDetector`:
  - Room con nombre → no fake DM
  - Room sin nombre, 2 miembros (1 bot + 1 contacto) → fake DM ✓
  - Room sin nombre, 4 miembros → no fake DM
  - Room sin nombre, 2 miembros pero ningún bot → no fake DM
  - Room sin nombre, solo bot + local user → incomplete (primer sync)
- [ ] **1.6.3** Tests para `BeeperMessageFilter`:
  - `m.room.message` → real
  - `com.beeper.bridge_info` → no real
  - `m.room.member` → no real (state event)
- [ ] **1.6.4** Tests para `FakeBeeperBridgeService` (mock para testing de capas superiores).
- [ ] **1.6.5** Correr tests en CI y verificar que pasan.

> **Criterio de éxito Fase 1**: `./gradlew :features:beeperbridge:impl:test` pasa al 100%. El service detecta correctamente redes, bots, y fake DMs con datos reales de un cuenta Beeper.

---

### Fase 2: Integración Visual Básica (3-4 semanas)
> **Objetivo**: Los chats bridgeados se ven correctamente en la lista

#### Paso 2.1: Network Badge Composable

- [ ] **2.1.1** Crear iconos drawable para cada red (16dp y 24dp):
  - `res/drawable/ic_whatsapp.xml` (vector asset, color #25D366)
  - `res/drawable/ic_instagram.xml`
  - `res/drawable/ic_telegram.xml`
  - `res/drawable/ic_signal.xml`
  - `res/drawable/ic_discord.xml`
  - `res/drawable/ic_facebook.xml`
  - `res/drawable/ic_slack.xml`
  - `res/drawable/ic_googlechat.xml`
  - `res/drawable/ic_bridge_generic.xml`
- [ ] **2.1.2** Crear `BeeperNetworkBadge.kt`:
  ```kotlin
  @Composable
  fun BeeperNetworkBadge(
      network: BeeperNetwork,
      modifier: Modifier = Modifier,
      size: Dp = 16.dp
  ) {
      Box(
          modifier = modifier
              .size(size)
              .clip(CircleShape)
              .background(Color(parseColor(network.colorHex)))
      ) {
          Icon(
              painter = painterResource(network.iconResId),
              contentDescription = network.displayName,
              tint = Color.White,
              modifier = Modifier.padding(2.dp)
          )
      }
  }
  ```
- [ ] **2.1.3** Crear preview Composable para verificar visualmente todos los badges.

#### Paso 2.2: Override de Display Name en Room List

- [ ] **2.2.1** Identificar el punto de inyección en `RoomListItem`:
  - **Opción A** (preferida): Crear un `BeeperRoomListItemWrapper` que envuelve el `RoomListItem` original y sustituye los parámetros.
  - **Opción B**: Modificar el `RoomListPresenter`/`RoomListState` para incluir un `displayNameOverride`.
- [ ] **2.2.2** Implementar la lógica de override:
  ```kotlin
  // Pseudocódigo del wrapper
  val beeperData = beeperService.getRoomData(room.roomId)
  val effectiveDisplayName = beeperData?.overrideDisplayName ?: room.name
  val effectiveAvatar = beeperData?.overrideAvatarUrl ?: room.avatarUrl
  val isDirectChat = room.isDirect || (beeperData?.isFakeDm == true)
  ```
- [ ] **2.2.3** Para Fake DMs: usar el `displayName` del contacto real en lugar del nombre del room.
- [ ] **2.2.4** Añadir comentario `// BEEP:` en cualquier archivo de Element X que se modifique.

#### Paso 2.3: Override de Avatar en Room List

- [ ] **2.3.1** Implementar lógica de avatar override:
  - Para Fake DMs: usar el avatar del contacto real (`realContactMxid`).
  - Si el contacto no tiene avatar: mostrar placeholder con inicial del nombre + color de la red.
  - Si NO es fake DM: usar el avatar original del room.
- [ ] **2.3.2** Crear `BeeperAvatarWithBadge` composable que combine avatar + badge de red:
  ```kotlin
  @Composable
  fun BeeperAvatarWithBadge(
      avatarUrl: String?,
      displayName: String,
      network: BeeperNetwork?,
      modifier: Modifier = Modifier
  ) {
      Box(modifier = modifier) {
          // Avatar base (reutilizar componente de Compound)
          Avatar(url = avatarUrl, name = displayName, size = AvatarSize.MEDIUM)
          // Badge de red en la esquina superior derecha
          if (network != null && network != BeeperNetwork.UNKNOWN) {
              BeeperNetworkBadge(
                  network = network,
                  modifier = Modifier.align(Alignment.TopEnd)
              )
          }
      }
  }
  ```

#### Paso 2.4: Integración con Compound Design System

- [ ] **2.4.1** Registrar colores de red como tokens del theme:
  ```kotlin
  // BeeperTokens.kt
  object BeeperTokens {
      val whatsappColor = Color(0xFF25D366)
      val instagramColor = Color(0xFFE1306C)
      val telegramColor = Color(0xFF2AABEE)
      // ... etc
  }
  ```
- [ ] **2.4.2** Crear `BeeperTheme` composable que extienda `ElementTheme` con los tokens Beeper.
- [ ] **2.4.3** Verificar contraste de colores en dark mode y light mode.

#### Paso 2.5: Preview del último mensaje filtrado

- [ ] **2.5.1** Integrar `BeeperMessageFilter` en el snippet de preview del room list item.
- [ ] **2.5.2** Si el último evento es `com.beeper.*`, buscar el anterior `m.room.message`/`m.room.encrypted`.
- [ ] **2.5.3** Manejar el caso donde no hay ningún mensaje real: mostrar "No messages" o similar.

#### Paso 2.6: Tests visuales y verificación

- [ ] **2.6.1** Crear Compose Previews para cada variante de room list item:
  - Room normal (sin bridge)
  - Fake DM de WhatsApp
  - Fake DM de Instagram
  - Grupo bridgeado de Telegram
  - Room con mensaje `com.beeper.*` filtrado
- [ ] **2.6.2** Screenshot tests (si Element X los soporta) o verificación manual.
- [ ] **2.6.3** Buildear APK de prueba y verificar en un device real con cuenta Beeper.

> **Criterio de éxito Fase 2**: Al abrir la app con una cuenta Beeper, todos los chats bridgeados muestran el nombre del contacto real, su avatar, y el badge de red correcto. El APK de CI es funcional.

---

### Fase 3: Virtual Spaces y Labels (4-5 semanas)
> **Objetivo**: Organización de chats por red y etiquetas custom

#### Paso 3.1: Account Data Engine para Labels

- [ ] **3.1.1** Crear `BeeperLabelsRepository.kt`:
  ```kotlin
  class BeeperLabelsRepository @Inject constructor(
      private val matrixClient: MatrixClient
  ) {
      suspend fun getLabels(): List<BeeperLabel> {
          val raw = matrixClient.getAccountData("com.beeper.labels")
          return parseLabels(raw)
      }

      suspend fun saveLabel(label: BeeperLabel) {
          val current = getLabels().toMutableList()
          val index = current.indexOfFirst { it.id == label.id }
          if (index >= 0) current[index] = label else current.add(label)
          matrixClient.setAccountData("com.beeper.labels", serializeLabels(current))
      }

      suspend fun deleteLabel(labelId: String) { ... }

      suspend fun getHiddenNetworks(): Set<String> {
          // Read from special "_hidden_networks" key in com.beeper.labels
      }

      suspend fun setHiddenNetworks(networks: Set<String>) { ... }
  }
  ```
- [ ] **3.1.2** Implementar serialización/deserialización de labels:
  ```json
  {
    "uuid-1": {
      "title": "Familia",
      "emoji": "👨‍👩‍👧‍👦",
      "rooms": ["!room1:beeper.com", "!room2:beeper.com"],
      "isShownInInbox": true,
      "createdAt": 1700000000
    },
    "_hidden_networks": {
      "rooms": ["slack", "googlechat"]
    }
  }
  ```
- [ ] **3.1.3** Tests unitarios para parsing, serialización, CRUD.

#### Paso 3.2: Virtual Spaces Engine

- [ ] **3.2.1** Crear `VirtualSpaceId.kt`:
  ```kotlin
  sealed class VirtualSpaceId {
      data object AllChats : VirtualSpaceId()
      data class NetworkSpace(val networkKey: String) : VirtualSpaceId()
      data class LabelSpace(val labelId: String) : VirtualSpaceId()
      data class TagSpace(val tag: String) : VirtualSpaceId()
      data class RealSpace(val roomId: String) : VirtualSpaceId()
  }
  ```
- [ ] **3.2.2** Crear `VirtualSpacesProvider.kt`:
  ```kotlin
  class VirtualSpacesProvider @Inject constructor(
      private val beeperService: BeeperBridgeService,
      private val labelsRepository: BeeperLabelsRepository,
      private val matrixClient: MatrixClient
  ) {
      fun getAvailableSpaces(): Flow<List<VirtualSpaceItem>> { ... }
      fun filterRoomsForSpace(
          rooms: List<RoomSummary>,
          spaceId: VirtualSpaceId
      ): List<RoomSummary> { ... }
  }
  ```
- [ ] **3.2.3** Implementar `filterRoomsForSpace()`:
  - `AllChats`: todos los rooms excepto los de hidden networks
  - `NetworkSpace(key)`: rooms donde `beeperService.getNetworkForRoom(roomId)?.key == key`
  - `LabelSpace(id)`: rooms donde `roomId in label.roomIds`
  - `TagSpace(tag)`: rooms donde `tag in room.tags`
  - `RealSpace(roomId)`: rooms que son hijos del space real

#### Paso 3.3: Virtual Spaces Sidebar UI

- [ ] **3.3.1** Crear `BeeperSpacesSidebar.kt`:
  ```kotlin
  @Composable
  fun BeeperSpacesSidebar(
      spaces: List<VirtualSpaceItem>,
      selectedSpace: VirtualSpaceId,
      onSpaceSelected: (VirtualSpaceId) -> Unit,
      modifier: Modifier = Modifier
  ) {
      NavigationRail(modifier = modifier) {
          // "All Chats" item siempre primero
          // Real Matrix Spaces
          // Divider
          // Virtual Network Spaces (auto-generados)
          // Beeper Labels
          // Custom Matrix Tags
          // "Add" button al final
      }
  }
  ```
- [ ] **3.3.2** Crear `VirtualSpaceItem.kt` (data class para el modelo de UI):
  ```kotlin
  data class VirtualSpaceItem(
      val id: VirtualSpaceId,
      val displayName: String,
      val icon: ImageVector? = null,
      val networkIcon: Int? = null,  // drawable res id
      val emoji: String? = null,
      val badgeCount: Int = 0
  )
  ```
- [ ] **3.3.3** Implementar la integración con el room list:
  - Cuando se selecciona un space, filtrar la lista de rooms.
  - El `RoomListPresenter` o un wrapper debe escuchar el space seleccionado.
- [ ] **3.3.4** Implementar hidden networks:
  - En "All Chats", excluir rooms de redes ocultas.
  - Las redes ocultas siguen siendo accesibles si se selecciona su space explícitamente.

#### Paso 3.4: Label Editor UI

- [ ] **3.4.1** Crear `BeeperLabelEditorSheet.kt` (Bottom Sheet):
  ```kotlin
  @Composable
  fun BeeperLabelEditorSheet(
      existingLabel: BeeperLabel? = null,  // null = crear nuevo
      availableRooms: List<RoomSummary>,
      onSave: (BeeperLabel) -> Unit,
      onDelete: (() -> Unit)? = null,
      onDismiss: () -> Unit
  ) {
      // Campos:
      // - Title (TextField)
      // - Emoji (picker o TextField simple)
      // - "Mostrar en inbox" toggle
      // - Room selector (multi-select list con búsqueda)
      // - Botón Guardar / Eliminar
  }
  ```
- [ ] **3.4.2** Implementar el room selector con búsqueda y checkboxes.
- [ ] **3.4.3** Conectar con `BeeperLabelsRepository` para persistencia.

#### Paso 3.5: Settings Screen "Parches de Beeper"

- [ ] **3.5.1** Crear `BeeperSettingsNode.kt` (Appyx node):
  ```kotlin
  // Secciones:
  // 1. Manual Read Receipts toggle
  // 2. "Contener Bridges" — toggles por red
  // 3. "Beeper Labels" — lista con Add/Edit/Delete
  // 4. "Custom Matrix Tags" — toggles de containment
  ```
- [ ] **3.5.2** Registrar el nuevo settings node en el árbol de navegación de Settings.
- [ ] **3.5.3** Conectar cada toggle con su respectivo repository/service.
- [ ] **3.5.4** Verificar que los cambios se persisten y se reflejan inmediatamente en la UI.

#### Paso 3.6: Tests y verificación

- [ ] **3.6.1** Tests unitarios para `BeeperLabelsRepository` (CRUD, serialización).
- [ ] **3.6.2** Tests unitarios para `VirtualSpacesProvider` (filtrado por cada tipo).
- [ ] **3.6.3** Tests unitarios para hidden networks.
- [ ] **3.6.4** Compose previews para sidebar, label editor, settings.
- [ ] **3.6.5** Build APK y testear manualmente la navegación entre spaces.

> **Criterio de éxito Fase 3**: El sidebar muestra espacios virtuales por red. Se pueden crear/editar/borrar labels. Las redes se pueden ocultar del inbox. Todo persiste tras cerrar y reabrir la app.

---

### Fase 4: Funcionalidades de Chat Avanzadas (3-4 semanas)
> **Objetivo**: Funcionalidades específicas de bridge dentro de los chats

#### Paso 4.1: Sticker Pack Management

- [ ] **4.1.1** Crear `BeeperStickerRepository.kt`:
  ```kotlin
  class BeeperStickerRepository @Inject constructor(
      private val matrixClient: MatrixClient
  ) {
      suspend fun getUserEmotes(): Map<String, StickerPack> {
          val raw = matrixClient.getAccountData("im.ponies.user_emotes")
          return parseStickerPacks(raw)
      }

      suspend fun addSticker(
          packName: String,
          stickerName: String,
          mxcUrl: String
      ) { ... }

      suspend fun uploadAndAddSticker(
          packName: String,
          stickerName: String,
          imageBytes: ByteArray,
          mimeType: String
      ) { ... }
  }
  ```
- [ ] **4.1.2** Implementar parsing del formato `im.ponies.user_emotes`:
  ```json
  {
    "images": {
      "sticker_name": {
        "url": "mxc://beeper.com/abc123",
        "body": "sticker_name",
        "info": { "w": 512, "h": 512, "mimetype": "image/webp" }
      }
    }
  }
  ```
- [ ] **4.1.3** Implementar upload de sticker:
  1. Upload del archivo al media server → obtener `mxc://` URL
  2. Leer Account Data actual
  3. Agregar nuevo sticker al pack
  4. Escribir Account Data actualizado
- [ ] **4.1.4** Crear UI básica para ver stickers importados (Composable grid).

#### Paso 4.2: Native App Fallbacks

- [ ] **4.2.1** Crear `NativeAppLauncher.kt`:
  ```kotlin
  object NativeAppLauncher {
      fun canLaunchNativeApp(network: BeeperNetwork): Boolean {
          // Check if native app intent is resolvable
      }

      fun launchNativeApp(
          context: Context,
          network: BeeperNetwork,
          contactIdentifier: String?
      ) {
          val intent = when (network) {
              BeeperNetwork.WHATSAPP -> Intent(Intent.ACTION_VIEW,
                  Uri.parse("https://wa.me/$contactIdentifier"))
              BeeperNetwork.INSTAGRAM -> Intent(Intent.ACTION_VIEW,
                  Uri.parse("instagram://user?username=$contactIdentifier"))
              BeeperNetwork.TELEGRAM -> Intent(Intent.ACTION_VIEW,
                  Uri.parse("tg://resolve?domain=$contactIdentifier"))
              // ... etc
          }
          context.startActivity(intent)
      }
  }
  ```
- [ ] **4.2.2** Agregar botón "Abrir en app nativa" en la app bar del chat (solo si `canLaunchNativeApp` es true).
- [ ] **4.2.3** Implementar la extracción del identifier del contacto desde el MXID:
  - `@whatsapp_5491123456789:beeper.com` → `+5491123456789`
  - `@instagram_username:beeper.com` → `username`
  - `@telegram_123456:beeper.com` → `123456`

#### Paso 4.3: Manual Read Receipts

- [ ] **4.3.1** Crear `ManualReadReceiptsPreference.kt`:
  ```kotlin
  class ManualReadReceiptsPreference @Inject constructor(
      private val dataStore: DataStore<Preferences>
  ) {
      val isEnabled: Flow<Boolean> = dataStore.data.map {
          it[MANUAL_READ_RECEIPTS_KEY] ?: false
      }

      suspend fun setEnabled(enabled: Boolean) {
          dataStore.edit { it[MANUAL_READ_RECEIPTS_KEY] = enabled }
      }

      companion object {
          val MANUAL_READ_RECEIPTS_KEY = booleanPreferencesKey("beeper_manual_read_receipts")
      }
  }
  ```
- [ ] **4.3.2** Interceptar el punto donde Element X envía `setReadMarker`:
  - Buscar en el código del timeline/room: `room.markAsRead()` o similar.
  - Envolver con check: `if (!manualReadReceiptsEnabled) room.markAsRead()`.
  - Añadir comentario `// BEEP:` en la modificación.
- [ ] **4.3.3** Agregar botón "Marcar como leído" en el menú del chat cuando manual read receipts está habilitado.

#### Paso 4.4: Filtrado de último mensaje real en preview

- [ ] **4.4.1** Integrar `BeeperMessageFilter` en el pipeline de room list item preview.
- [ ] **4.4.2** Asegurarse de que el timestamp mostrado corresponda al último mensaje real, no al último evento `com.beeper.*`.

#### Paso 4.5: Tests

- [ ] **4.5.1** Tests unitarios para `BeeperStickerRepository` (parsing, CRUD).
- [ ] **4.5.2** Tests unitarios para `NativeAppLauncher` (extracción de identifiers).
- [ ] **4.5.3** Tests unitarios para `ManualReadReceiptsPreference`.
- [ ] **4.5.4** Tests de integración: verificar que read receipts no se envían cuando la feature está activa.

> **Criterio de éxito Fase 4**: Los stickers se pueden importar, las apps nativas se abren correctamente, y los read receipts manuales funcionan. APK de prueba verificado.

---

### Fase 5: Pulido y Estabilidad (2-3 semanas)
> **Objetivo**: Robustez y performance

#### Paso 5.1: Performance Optimization

- [ ] **5.1.1** Benchmark con cuenta de 1000+ rooms bridgeados:
  - Medir tiempo de carga de room list
  - Medir memoria usada por el cache de `BeeperRoomData`
  - Medir tiempo de detección de bridge al abrir un chat
- [ ] **5.1.2** Optimizar el cache:
  - Implementar LRU cache con límite configurable
  - Pre-computar datos de bridge durante el sync
  - Lazy loading: solo calcular datos de rooms visibles
- [ ] **5.1.3** Optimizar el sidebar de Virtual Spaces:
  - Debounce en la actualización de badge counts
  - Paginación del room list filtrado
- [ ] **5.1.4** Profile de memoria con Android Profiler.

#### Paso 5.2: Graceful Degradation

- [ ] **5.2.1** Verificar comportamiento con cuenta no-Beeper:
  - `beeperService.isEnabled()` debe retornar `false`
  - Ningún UI element de Beeper debe mostrarse
  - La sidebar de Virtual Spaces no debe aparecer
  - Settings "Parches de Beeper" debe ocultarse
- [ ] **5.2.2** Verificar comportamiento con cuenta Beeper parcial (solo algunos bridges):
  - Solo mostrar badges para redes detectadas
  - Virtual spaces solo para redes presentes
- [ ] **5.2.3** Manejar errores de red en Account Data:
  - Si `com.beeper.labels` no se puede leer → usar valores por defecto
  - Si `setAccountData` falla → retry con backoff + notificación al usuario
- [ ] **5.2.4** Verificar que la app no crashea si el Rust SDK no expone alguna API esperada.

#### Paso 5.3: Tests de integración por red

- [ ] **5.3.1** Test con bridge de **WhatsApp**:
  - Fake DM detectado correctamente
  - Avatar del contacto de WA visible
  - Badge verde de WhatsApp visible
  - "Abrir en WhatsApp" funcional
- [ ] **5.3.2** Test con bridge de **Telegram**:
  - Grupos bridgeados correctamente identificados
  - Badge azul de Telegram
- [ ] **5.3.3** Test con bridge de **Instagram**:
  - Fake DM con Instagram handle
  - Badge rosa de Instagram
- [ ] **5.3.4** Test con bridge de **Signal**, **Discord**, **Facebook**, **Slack**, **Google Chat**:
  - Detección correcta
  - Badges con colores correctos
- [ ] **5.3.5** Test con múltiples bridges simultáneos:
  - Virtual spaces muestra todas las redes
  - Filtrado funciona correctamente
  - No hay conflictos de cache

#### Paso 5.4: Merge upstream y resolución de conflictos

- [ ] **5.4.1** Hacer merge del último upstream de Element X:
  ```bash
  git fetch upstream
  git merge upstream/develop
  ```
- [ ] **5.4.2** Resolver conflictos (esperados en room list, settings, navigation).
- [ ] **5.4.3** Re-verificar que todo funciona post-merge.
- [ ] **5.4.4** Documentar el proceso de merge para futuras actualizaciones.

#### Paso 5.5: Documentación final

- [ ] **5.5.1** Actualizar `README.md` con estado actual de features.
- [ ] **5.5.2** Actualizar `CONTRIBUTING.md` con instrucciones finales.
- [ ] **5.5.3** Crear `docs/RELEASE_NOTES.md` con changelog.
- [ ] **5.5.4** Actualizar `PHASE0_FINDINGS.md` con hallazgos finales.

> **Criterio de éxito Fase 5**: La app funciona establemente con 1000+ rooms. No hay crashes en cuentas no-Beeper. Todos los bridges detectados muestran correctamente. El merge con upstream está limpio.

---

## Estimación de Esfuerzo

| Fase | Duración | Dificultad | Riesgo | Items totales |
|---|---|---|---|---|
| Fase 0: Preparación & PoC | 2-3 sem | 🟡 Media | 🟡 Medio | ~15 pasos |
| Fase 1: Detección de bridges | 3-4 sem | 🟡 Media | 🟢 Bajo | ~20 pasos |
| Fase 2: Visual básico | 3-4 sem | 🟡 Media | 🟢 Bajo | ~15 pasos |
| Fase 3: Spaces/Labels | 4-5 sem | 🟡 Media | 🟡 Medio | ~20 pasos |
| Fase 4: Chat avanzado | 3-4 sem | 🟢 Baja | 🟢 Bajo | ~15 pasos |
| Fase 5: Pulido | 2-3 sem | 🟢 Baja | 🟢 Bajo | ~15 pasos |
| **TOTAL** | **~17-23 semanas** | | | **~100 pasos** |

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

---

## Apéndice A: Estructura completa del módulo

```
features/beeperbridge/
├── api/
│   ├── build.gradle.kts
│   └── src/main/kotlin/io/element/android/features/beeperbridge/api/
│       ├── BeeperBridgeService.kt      ← Interface principal
│       ├── BeeperRoomData.kt           ← Data class para datos de room
│       ├── BeeperNetwork.kt            ← Enum de redes con colores/iconos
│       ├── BeeperLabel.kt              ← Data class para labels
│       └── VirtualSpaceId.kt           ← Sealed class para space IDs
├── impl/
│   ├── build.gradle.kts
│   └── src/main/kotlin/io/element/android/features/beeperbridge/impl/
│       ├── BeeperRoomDataProvider.kt    ← Implementación de BeeperBridgeService
│       ├── BeeperNetworkMap.kt          ← Mapeo de prefijos MXID → red
│       ├── FakeDmDetector.kt            ← Heurística de Fake DM
│       ├── BeeperMessageFilter.kt       ← Filtrado de mensajes reales
│       ├── BeeperLabelsRepository.kt    ← CRUD de labels (Account Data)
│       ├── BeeperStickerRepository.kt   ← Gestión de sticker packs
│       ├── VirtualSpacesProvider.kt     ← Engine de espacios virtuales
│       ├── ManualReadReceiptsPreference.kt ← DataStore preference
│       ├── NativeAppLauncher.kt         ← Android Intents para apps nativas
│       └── ui/
│           ├── BeeperNetworkBadge.kt    ← @Composable badge de red
│           ├── BeeperAvatarWithBadge.kt ← Avatar + badge overlay
│           ├── BeeperSpacesSidebar.kt   ← Sidebar de Virtual Spaces
│           ├── BeeperLabelEditorSheet.kt ← Bottom sheet editor de labels
│           ├── BeeperSettingsNode.kt    ← Pantalla de settings Beeper
│           └── BeeperTokens.kt          ← Design tokens (colores)
├── test/
│   ├── build.gradle.kts
│   └── src/test/kotlin/io/element/android/features/beeperbridge/test/
│       ├── FakeBeeperBridgeService.kt   ← Mock para testing
│       ├── BeeperNetworkMapTest.kt      ← Tests de detección de red
│       ├── FakeDmDetectorTest.kt        ← Tests de heurística Fake DM
│       ├── BeeperMessageFilterTest.kt   ← Tests de filtrado
│       ├── BeeperLabelsRepositoryTest.kt ← Tests de CRUD labels
│       └── NativeAppLauncherTest.kt     ← Tests de intent generation
└── res/
    └── drawable/
        ├── ic_whatsapp.xml
        ├── ic_instagram.xml
        ├── ic_telegram.xml
        ├── ic_signal.xml
        ├── ic_discord.xml
        ├── ic_facebook.xml
        ├── ic_slack.xml
        ├── ic_googlechat.xml
        └── ic_bridge_generic.xml
```

## Apéndice B: GitHub Action — Workflow completo

El archivo `.github/workflows/build-apk.yml` descrito arriba se debe crear en el repositorio.
Para activarlo:

1. Crear el directorio `.github/workflows/` en la raíz del proyecto.
2. Copiar el YAML de la sección CI/CD.
3. Hacer push a `main` o `develop`.
4. Verificar en la pestaña **Actions** de GitHub.

### Workflow adicional para releases (opcional futuro):

```yaml
name: Release APK

on:
  push:
    tags: [ 'v*' ]

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - uses: android-actions/setup-android@v3

      - name: Decode keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/keystore.jks

      - name: Build Release APK
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/release/*.apk
          generate_release_notes: true
```
