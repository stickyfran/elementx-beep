/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl.spaces

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import io.element.android.features.beeperbridge.api.BeeperNetwork
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceItem
import io.element.android.libraries.di.AppScope
import kotlinx.coroutines.flow.Flow
import io.element.android.features.beeperbridge.api.spaces.VirtualSpacesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@ContributesBinding(AppScope::class)
class DefaultVirtualSpacesProvider @Inject constructor(
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
}
