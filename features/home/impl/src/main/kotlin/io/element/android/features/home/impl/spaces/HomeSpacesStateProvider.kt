/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spaces

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceItem
import kotlinx.collections.immutable.toImmutableList

open class HomeSpacesStateProvider : PreviewParameterProvider<HomeSpacesState> {
    override val values: Sequence<HomeSpacesState>
        get() = sequenceOf(
            aHomeSpacesState(
                spaces = aListOfVirtualSpaces(),
            ),
            aHomeSpacesState(
                spaces = emptyList(),
            ),
        )
}

internal fun aHomeSpacesState(
    spaces: List<VirtualSpaceItem> = aListOfVirtualSpaces(),
    selectedSpaceId: VirtualSpaceId = VirtualSpaceId.AllChats,
    eventSink: (HomeSpacesEvents) -> Unit = {},
) = HomeSpacesState(
    spaces = spaces.toImmutableList(),
    selectedSpaceId = selectedSpaceId,
    eventSink = eventSink,
)

fun aListOfVirtualSpaces(): List<VirtualSpaceItem> {
    return listOf(
        VirtualSpaceItem(id = VirtualSpaceId.AllChats, displayName = "All Chats"),
        VirtualSpaceItem(id = VirtualSpaceId.NetworkSpace("whatsapp"), displayName = "WhatsApp", networkIcon = 0),
        VirtualSpaceItem(id = VirtualSpaceId.LabelSpace("label1"), displayName = "Favorites", emoji = "⭐"),
    )
}
