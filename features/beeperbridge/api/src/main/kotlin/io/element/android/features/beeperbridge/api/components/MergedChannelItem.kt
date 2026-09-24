/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.beeperbridge.api.components

import androidx.compose.runtime.Immutable
import io.element.android.features.beeperbridge.api.BeeperNetwork

@Immutable
data class MergedChannelItem(
    val roomId: String,
    val network: BeeperNetwork,
    val displayName: String? = null,
    val unreadCount: Long = 0,
)
