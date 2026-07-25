# Phase 0 Findings

> Document your findings here after auditing the Matrix Rust SDK bindings.
> This determines what's possible without forking the Rust SDK.

## Status: 🔲 Not started

---

## Questions to Answer

### 1. Room member data access
**Question**: Can we read individual `m.room.member` state events including each member's MXID, displayname, and avatar URL?

**How to check**: Look for `room.members()` or similar in the Rust SDK's Kotlin bindings.
Look in: `matrix-rust-sdk/bindings/apple/Sources/MatrixRustSDK/` or the generated UniFFI bindings.

**Finding**: _TODO_

---

### 2. Account Data (read)
**Question**: Can we read arbitrary Account Data events by type (e.g., `com.beeper.labels`, `im.ponies.user_emotes`)?

**Finding**: _TODO_

---

### 3. Account Data (write)
**Question**: Can we write Account Data events via `client.setAccountData(type, content)`?

**Finding**: _TODO_

---

### 4. Room list item display name override
**Question**: Can we override the display name shown for a room in the room list without forking the list Composable?

**Options to explore**:
- Does `RoomListRoomSummary` have a mutable `name` field?
- Is there a slot API or ViewModel hook we can intercept?
- Can we inject a `RoomListRoomSummaryProvider` that wraps the default one?

**Finding**: _TODO_

---

### 5. Room list item avatar override
**Question**: Same as above but for the avatar URL.

**Finding**: _TODO_

---

### 6. Room list item custom content injection
**Question**: Can we add a network badge Composable to each room list item without forking the `RoomListItem` composable?

**Finding**: _TODO_

---

### 7. Room filtering hook
**Question**: Where in the Element X architecture is the room list filtered? Can we inject a custom filter without forking?

**Finding**: _TODO_

---

## Conclusion

**Can we proceed without forking the Rust SDK?** _TODO_

**Do we need to fork `RoomListItem`?** _TODO_

**Recommended approach**: _TODO_
