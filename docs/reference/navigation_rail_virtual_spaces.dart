// REFERENCE IMPLEMENTATION — FluffyBeep (Flutter/Dart)
// Source: fluffychat_src/lib/pages/chat_list/navigation_rail.dart
//
// DO NOT copy. Reimplementar en Kotlin/Compose.
// Este archivo muestra cómo FluffyBeep implementa los Virtual Spaces en el sidebar.
//
// Kotlin equivalente: features/beeperbridge/impl/ui/BeeperSpacesSideBar.kt
// ============================================================

// KEY CONCEPTS:
//
// 1. THREE types of sidebar items coexist:
//    - Matrix Spaces (real rooms with isSpace=true)
//    - Virtual Network Spaces (dynamically generated from detected bridge rooms)
//    - Beeper Labels (from com.beeper.labels account data)
//    - Matrix custom tags (u.xxx tags on rooms)
//
// 2. Virtual space IDs (string-based, NOT real room IDs):
//    - Network:  'virtual_space_whatsapp'
//    - Label:    'virtual_beeper_label_<uuid>'
//    - Tag:      'virtual_tag_u.mytag'
//
// 3. Network filtering (which rooms belong to a virtual space):
//    BeeperBridgeUtils.getBaseNetworkKey(
//        BeeperBridgeUtils.getRoomData(room).networkKey ?? ''
//    ) == net.key
//
// 4. Label filtering:
//    final rooms = labelData['rooms'] as List?;
//    rooms?.contains(room.id) == true
//
// 5. Hidden networks stored in com.beeper.labels under '_hidden_networks' key:
//    labels['_hidden_networks'] = {
//      'title': 'Hidden Networks',
//      'rooms': ['whatsapp', 'instagram'],  // network keys, not room IDs!
//      'isShownInInbox': false,
//    }
//
// 6. Network ordering stored in local SharedPreferences (DataStore in Kotlin):
//    key: 'beeper_networks_order'
//    value: List<String> of network keys in display order
//
// 7. Deduplication: if a network is already covered by a Label, don't show
//    the standalone network icon in the sidebar.
//
// IN COMPOSE, the sidebar structure would be:
//
// @Composable
// fun BeeperSpacesSideBar(
//     spaces: List<MatrixSpace>,
//     beeperNetworks: List<BeeperNetwork>,
//     beeperLabels: List<BeeperLabel>,
//     matrixTags: List<String>,
//     selectedSpaceId: String?,
//     onSpaceSelected: (String?) -> Unit,
// ) {
//     LazyColumn {
//         // "All chats" item (selectedSpaceId == null)
//         item { AllChatsItem(isSelected = selectedSpaceId == null, ...) }
//
//         // Real Matrix Spaces
//         items(spaces) { space -> SpaceItem(space, ...) }
//
//         // Virtual network spaces
//         items(beeperNetworks) { net ->
//             NetworkSpaceItem(
//                 network = net,
//                 isSelected = selectedSpaceId == "virtual_space_${net.key}",
//                 onClick = { onSpaceSelected("virtual_space_${net.key}") }
//             )
//         }
//
//         // Beeper Labels
//         items(beeperLabels) { label ->
//             LabelSpaceItem(
//                 label = label,
//                 isSelected = selectedSpaceId == "virtual_beeper_label_${label.uuid}",
//                 onClick = { onSpaceSelected("virtual_beeper_label_${label.uuid}") }
//             )
//         }
//
//         // Matrix custom tags
//         items(matrixTags) { tag ->
//             TagSpaceItem(tag = tag, ...)
//         }
//
//         // "Add" button at the bottom
//         item { AddSpaceButton(...) }
//     }
// }
//
// ROOM LIST FILTERING (applied when a virtual space is selected):
//
// fun filterRoomsForSpace(
//     rooms: List<RoomSummary>,
//     selectedSpaceId: String?,
//     beeperService: BeeperBridgeService,
// ): List<RoomSummary> {
//     if (selectedSpaceId == null) return rooms  // All chats
//
//     return when {
//         selectedSpaceId.startsWith("virtual_space_") -> {
//             val networkKey = selectedSpaceId.removePrefix("virtual_space_")
//             rooms.filter { room ->
//                 beeperService.getRoomData(room.roomId).networkKey == networkKey
//             }
//         }
//         selectedSpaceId.startsWith("virtual_beeper_label_") -> {
//             val labelUuid = selectedSpaceId.removePrefix("virtual_beeper_label_")
//             val labelRooms = beeperService.getLabelRoomIds(labelUuid)
//             rooms.filter { it.roomId in labelRooms }
//         }
//         selectedSpaceId.startsWith("virtual_tag_") -> {
//             val tag = selectedSpaceId.removePrefix("virtual_tag_")
//             rooms.filter { room -> tag in room.tags }
//         }
//         else -> rooms  // Real Matrix Space — handled by upstream Element X
//     }
// }
