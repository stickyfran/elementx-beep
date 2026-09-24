/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceItem
import io.element.android.features.beeperbridge.api.spaces.VirtualSpacesProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeVirtualSpacesProvider : VirtualSpacesProvider {
    private val selectedSpaceFlow = MutableStateFlow<VirtualSpaceId>(VirtualSpaceId.AllChats)
    private val availableSpacesFlow = MutableStateFlow<List<VirtualSpaceItem>>(emptyList())

    override fun getAvailableSpaces(): Flow<List<VirtualSpaceItem>> = availableSpacesFlow.asStateFlow()

    override fun getSelectedSpace(): StateFlow<VirtualSpaceId> = selectedSpaceFlow.asStateFlow()

    override fun selectSpace(spaceId: VirtualSpaceId) {
        selectedSpaceFlow.value = spaceId
    }

    fun emitAvailableSpaces(spaces: List<VirtualSpaceItem>) {
        availableSpacesFlow.value = spaces
    }
}
