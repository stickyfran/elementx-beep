# Contributing to ElementX-Beep

## Setup

1. **Clone Element X Android** (our upstream):
   ```bash
   git clone https://github.com/element-hq/element-x-android.git
   cd element-x-android
   ```

2. **Add this repo as a remote** (for our customizations):
   ```bash
   git remote add beep https://github.com/<your-org>/elementx-beep.git
   ```

3. **Build the Rust SDK** (one-time setup):
   ```bash
   # Requires Rust toolchain + cargo
   ./tools/sdk/build-rust-sdk
   ```

4. **Open in Android Studio** and run on a device/emulator.

## Project Structure

```
element-x-android/                    ← upstream (don't modify unnecessarily)
  features/
    beeperbridge/                     ← OUR MODULE (new)
      api/
        src/main/kotlin/
          BeeperBridgeService.kt      ← Main service interface
          BeeperRoomData.kt           ← Data model
          BeeperNetwork.kt            ← Network enum/model
          BeeperLabel.kt              ← Label model
      impl/
        src/main/kotlin/
          BeeperBridgeServiceImpl.kt  ← Implementation
          BeeperNetworkMap.kt         ← networkMap constant
          ui/
            BeeperNetworkBadge.kt     ← @Composable badge
            BeeperSettingsNode.kt     ← Settings screen
            BeeperLabelEditorSheet.kt ← Label editor bottom sheet
      test/
        src/test/kotlin/
          BeeperBridgeServiceTest.kt
          FakeBeeperBridgeService.kt

docs/
  ROADMAP.md          ← Implementation roadmap
  CONTRIBUTING.md     ← This file
  reference/          ← FluffyBeep Dart reference implementations
    beeper_bridge_utils.dart
    chat_list_item_beeper_integration.dart
    navigation_rail_virtual_spaces.dart
    settings_patches.dart
```

## Development Workflow

1. All new code goes into `features/beeperbridge/`.
2. When modifying existing Element X files:
   - Prefer **extension functions** and **wrapper Composables** over patching existing code.
   - If you must modify an existing file, add a `// BEEP:` comment explaining why.
3. Run linting before committing:
   ```bash
   ./gradlew ktlintCheck detekt
   ```
4. Keep Beeper features behind a runtime check so non-Beeper users aren't affected:
   ```kotlin
   if (beeperService.isEnabled()) { /* beeper stuff */ }
   ```

## Phase 0 Checklist (do this first!)

Before writing any feature code, verify these APIs exist in the Rust SDK bindings:

- [ ] Can we read `m.room.member` state events per member? (`room.members()`)
- [ ] Can we read Account Data for arbitrary event types? (`client.accountData(type)`)
- [ ] Can we write Account Data? (`client.setAccountData(type, content)`)
- [ ] Can we override the display name shown in `RoomListRoomSummary`?
- [ ] Can we override the avatar shown in `RoomListRoomSummary`?
- [ ] Can we add a custom Composable slot to the room list item?

Document findings in `docs/PHASE0_FINDINGS.md`.

## Key Resources

- [Element X Android source](https://github.com/element-hq/element-x-android)
- [Matrix Rust SDK](https://github.com/matrix-org/matrix-rust-sdk)
- [Compound Android Design System](https://github.com/element-hq/compound-android)
- [FluffyBeep reference](../fluffybeep/) — original Flutter implementation
- [Roadmap](ROADMAP.md)
