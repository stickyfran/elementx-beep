package io.element.android.features.beeperbridge.impl.spaces

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperBridgeService
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceItem
import io.element.android.libraries.di.AppScope
import io.element.android.features.home.impl.model.RoomListRoomSummary
import kotlinx.coroutines.flow.Flow
import io.element.android.features.beeperbridge.api.spaces.VirtualSpacesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@ContributesBinding(AppScope::class)
class DefaultVirtualSpacesProvider @Inject constructor(
    private val beeperBridgeService: BeeperBridgeService,
    private val labelsRepository: BeeperLabelsRepository
) : VirtualSpacesProvider {

    private val selectedSpaceFlow = MutableStateFlow<VirtualSpaceId>(VirtualSpaceId.AllChats)

    override fun getSelectedSpace(): StateFlow<VirtualSpaceId> = selectedSpaceFlow.asStateFlow()

    override fun selectSpace(spaceId: VirtualSpaceId) {
        selectedSpaceFlow.value = spaceId
    }

    override fun getAvailableSpaces(): Flow<List<VirtualSpaceItem>> {
        return labelsRepository.getLabelsFlow().map { labels ->
            val spaces = mutableListOf<VirtualSpaceItem>()
            
            // 1. All Chats
            spaces.add(
                VirtualSpaceItem(
                    id = VirtualSpaceId.AllChats,
                    displayName = "All Chats",
                    // icon = CompoundIcons.Chat() // We can map this in the UI
                )
            )

            // 2. Network Spaces
            BeeperNetwork.entries.forEach { network ->
                if (network != BeeperNetwork.UNKNOWN) {
                    spaces.add(
                        VirtualSpaceItem(
                            id = VirtualSpaceId.NetworkSpace(network.name),
                            displayName = network.displayName,
                            networkIcon = network.iconResId
                        )
                    )
                }
            }

            // 3. Label Spaces
            labels.filter { it.isShownInInbox }.forEach { label ->
                spaces.add(
                    VirtualSpaceItem(
                        id = VirtualSpaceId.LabelSpace(label.id),
                        displayName = label.title,
                        emoji = label.emoji
                    )
                )
            }

            spaces
        }
    }

    override suspend fun filterRoomsForSpace(
        rooms: List<RoomSummary>,
        spaceId: VirtualSpaceId
    ): List<RoomSummary> {
        return when (spaceId) {
            is VirtualSpaceId.AllChats -> {
                val hiddenNetworks = labelsRepository.getHiddenNetworks()
                rooms.filter { room ->
                    val network = beeperBridgeService.getNetworkForRoom(room.roomId.value)
                    network == null || !hiddenNetworks.contains(network.name)
                }
            }
            is VirtualSpaceId.NetworkSpace -> {
                rooms.filter { room ->
                    beeperBridgeService.getNetworkForRoom(room.roomId.value)?.name == spaceId.networkKey
                }
            }
            is VirtualSpaceId.LabelSpace -> {
                val labels = labelsRepository.getLabels()
                val label = labels.find { it.id == spaceId.labelId }
                if (label != null) {
                    rooms.filter { label.roomIds.contains(it.roomId.value) }
                } else {
                    emptyList()
                }
            }
            is VirtualSpaceId.RealSpace -> {
                // Not supported yet without Matrix SDK Space API
                rooms
            }
            is VirtualSpaceId.TagSpace -> {
                rooms // Not supported yet
            }
        }
    }
}
