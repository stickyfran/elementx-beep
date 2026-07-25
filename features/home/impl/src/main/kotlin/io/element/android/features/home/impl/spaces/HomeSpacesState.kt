/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.spaces

import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceId
import io.element.android.features.beeperbridge.api.spaces.VirtualSpaceItem
import kotlinx.collections.immutable.ImmutableList

data class HomeSpacesState(
    val spaces: ImmutableList<VirtualSpaceItem>,
    val selectedSpaceId: VirtualSpaceId,
    val eventSink: (HomeSpacesEvents) -> Unit,
)
