// REFERENCE IMPLEMENTATION — FluffyBeep (Flutter/Dart)
// Source: fluffychat_src/lib/pages/chat_list/chat_list_item.dart
//
// DO NOT copy. Reimplementar en Kotlin/Compose.
// Este archivo muestra cómo FluffyBeep reemplaza display name y avatar
// en el ítem de la lista de chats.
//
// Kotlin equivalente: features/beeperbridge/impl/ui/BeeperRoomListItem.kt
// ============================================================

// KEY PATTERN: How BeeperBridgeUtils is used in the chat list item
//
// 1. Get Beeper data once per build cycle:
//    final beeperData = BeeperBridgeUtils.getRoomData(widget.room);
//
// 2. Override display name:
//    final displayname = beeperData.displayName
//        ?? room.getLocalizedDisplayname(MatrixLocals(L10n.of(context)));
//
// 3. Override avatar:
//    final mxContent = beeperData.avatarUrl;  // null if no Beeper data
//
// 4. Override DM detection:
//    final isBeeperFakeDM = beeperData.isFakeDM;
//    final isDirectChat = directChatMatrixId != null || isBeeperFakeDM;
//
// 5. Filter last event (skip com.beeper.* events):
//    final lastEvent = cachedRealEvent
//        ?? (room.lastEvent?.type.startsWith('com.beeper') == true
//            ? null
//            : room.lastEvent);
//
// 6. Network badge (positioned top-right of avatar):
//    Positioned(
//      top: 0, right: 0,
//      child: NetworkBadge(network: BeeperBridgeUtils.getRoomNetwork(room)),
//    )
//
// IN COMPOSE, this would look like:
//
// @Composable
// fun BeeperAwareRoomListItem(
//     room: RoomListRoomSummary,
//     beeperData: BeeperRoomData?,
//     onClick: () -> Unit,
// ) {
//     val displayName = beeperData?.displayName ?: room.name
//     val avatarUrl = beeperData?.avatarUrl ?: room.avatarUrl
//     val network = beeperData?.network
//
//     RoomListItem(
//         room = room.copy(name = displayName, avatarUrl = avatarUrl),
//         onClick = onClick,
//         // inject network badge via slot API or wrapper
//         trailingContent = { if (network != null) NetworkBadge(network) }
//     )
// }
