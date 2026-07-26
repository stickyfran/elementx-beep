/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api.spaces

sealed interface VirtualSpaceId {
    data object AllChats : VirtualSpaceId
    data class NetworkSpace(val networkKey: String) : VirtualSpaceId
    data class LabelSpace(val labelId: String) : VirtualSpaceId
    data class TagSpace(val tag: String) : VirtualSpaceId
    data class RealSpace(val roomId: String) : VirtualSpaceId
}
