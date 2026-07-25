# Project-Scoped Rules — ElementX-Beep

## Project Goal
Fork of Element X Android with Beeper bridge integration.
We implement **Category A** (protocol/data logic) and **Category B** (UI/UX) features only.
Category C (push notifications, E2EE crypto, cache management) is out of scope.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Compound Design System
- **Matrix SDK**: Matrix Rust SDK (via UniFFI FFI bindings)
- **State**: Molecule + StateFlow
- **Navigation**: Appyx
- **Build**: Gradle (Kotlin DSL)
- **Base repo**: https://github.com/element-hq/element-x-android

## Architecture Rules
- Follow Element X's strict **3-module pattern** for all new features:
  - `features/beeperbridge/api/` — interfaces, data models
  - `features/beeperbridge/impl/` — presenters, views, use cases
  - `features/beeperbridge/test/` — fakes, unit tests
- Do NOT modify files outside of `features/beeperbridge/` unless strictly necessary.
- When touching existing Element X files (e.g., RoomListItem), prefer **extending** over **modifying**.
- All new Kotlin code must follow **ktlint** and **detekt** rules (run `./gradlew lint` to check).

## Beeper Integration Principles
- Bridge detection is done by parsing Matrix room member MXIDs for known prefixes (e.g., `@whatsapp_xxx:beeper.com`).
- The single source of truth for Beeper room data is `BeeperBridgeService` (our service, not the Rust SDK).
- Beeper labels are stored in Matrix Account Data under the key `com.beeper.labels`.
- Sticker packs use `im.ponies.user_emotes` (Matrix account data).
- **Never** assume a user is on Beeper — all features must degrade gracefully for non-Beeper accounts.

## Network Map (current)
| Key | Display Name | Color |
|---|---|---|
| whatsapp | WhatsApp | #25D366 |
| instagram | Instagram | #E1306C |
| telegram | Telegram | #2AABEE |
| signal | Signal | #3A76F0 |
| discord | Discord | #5865F2 |
| facebook | Facebook Messenger | #00B2FF |
| slack | Slack | #4A154B |
| googlechat | Google Chat | #34A853 |

## Reference Implementations (Flutter/Dart)
See `docs/reference/` for the original FluffyBeep implementations to use as design reference.
These are NOT to be copied verbatim — they must be reimplemented in Kotlin/Compose.

## Key Custom Matrix Events
- `com.beeper.labels` — Account Data for Beeper label/inbox management
- `im.ponies.user_emotes` — Account Data for sticker packs
- `com.beeper.*` — Beeper-specific room events to filter from message previews

## Fake DM Detection Heuristic
A room is a "Fake DM" (bridged 1:1 conversation) if:
1. It has NO `m.room.name` state event (or it's empty)
2. It has ≤ 3 active members (join/invite)
3. One of the non-local members matches a bridge bot pattern
4. At least one real contact MXID is identifiable (non-bot)
