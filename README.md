# ElementX-Beep

> Element X Android with Beeper bridge integration — Categories A & B only.

A fork of [Element X Android](https://github.com/element-hq/element-x-android) that adds first-class visual support for Beeper's Matrix bridges (WhatsApp, Instagram, Telegram, Signal, Discord, Slack, Facebook, Google Chat).

## What this adds

| Feature | Status |
|---|---|
| Bridge network detection (MXID parsing) | 🔲 Planned |
| Fake DM detection (bridged 1:1 heuristic) | 🔲 Planned |
| Display name override for bridge rooms | 🔲 Planned |
| Avatar override (real contact, not bot) | 🔲 Planned |
| Network badge on room list items | 🔲 Planned |
| Virtual sidebar spaces per network | 🔲 Planned |
| Beeper Labels (`com.beeper.labels`) | 🔲 Planned |
| Label editor UI | 🔲 Planned |
| Contain/hide networks from inbox | 🔲 Planned |
| WhatsApp sticker → personal pack | 🔲 Planned |
| Native app fallback buttons (tel:, instagram://) | 🔲 Planned |
| Manual read receipts toggle | 🔲 Planned |
| Last real message filtering (skip state events) | 🔲 Planned |
| Settings screen "Beeper Patches" | 🔲 Planned |

## What this does NOT change
- Push notification system (uses Element X defaults)
- E2EE / crypto (managed by Rust SDK)
- Cache management (managed by Rust SDK)
- All vanilla Element X features remain intact

## Architecture
```
features/beeperbridge/
  api/      → interfaces, data models (BeeperBridgeService, BeeperRoomData, BeeperNetwork)
  impl/     → presenters, use cases, Composables
  test/     → unit tests, fakes
```

## Setup

```bash
git clone https://github.com/element-hq/element-x-android.git
cd element-x-android
# Apply our customizations (see docs/CONTRIBUTING.md)
```

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md) for the full implementation roadmap.

## Based on
- [Element X Android](https://github.com/element-hq/element-x-android) — AGPL-3.0
- [FluffyBeep](../fluffybeep) — reference implementation (Flutter/Dart)

## License
AGPL-3.0 (same as Element X Android)
