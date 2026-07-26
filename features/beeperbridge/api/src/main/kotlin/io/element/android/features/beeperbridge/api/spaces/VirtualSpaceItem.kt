/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api.spaces

import androidx.compose.ui.graphics.vector.ImageVector

data class VirtualSpaceItem(
    val id: VirtualSpaceId,
    val displayName: String,
    val icon: ImageVector? = null,
    val networkIcon: Int? = null,  // drawable res id
    val emoji: String? = null,
    val badgeCount: Int = 0
)
