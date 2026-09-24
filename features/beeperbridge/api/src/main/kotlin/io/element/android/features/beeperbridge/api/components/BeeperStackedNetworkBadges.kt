/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.features.beeperbridge.api.BeeperNetwork
import kotlinx.collections.immutable.ImmutableList

import kotlinx.collections.immutable.persistentListOf

private const val MAX_STACKED_NETWORKS = 4

@Composable
fun BeeperStackedNetworkBadges(
    networks: ImmutableList<BeeperNetwork>,
    modifier: Modifier = Modifier,
    activeNetwork: BeeperNetwork? = null,
    unreadNetworks: ImmutableList<BeeperNetwork> = persistentListOf(),
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val validNetworks = networks.filter { it != BeeperNetwork.UNKNOWN }.distinct().take(MAX_STACKED_NETWORKS)
    if (validNetworks.isEmpty()) return

    // Active network goes first in visual order
    val sortedNetworks = if (activeNetwork != null && validNetworks.contains(activeNetwork)) {
        (listOf(activeNetwork) + (validNetworks - activeNetwork)).distinct()
    } else {
        validNetworks
    }

    if (sortedNetworks.size == 1) {
        BeeperNetworkBadge(
            network = sortedNetworks.first(),
            modifier = modifier,
            size = 9.dp,
            borderWidth = borderWidth,
            borderColor = borderColor,
        )
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sortedNetworks.forEach { network ->
            val isActive = network == activeNetwork
            val hasUnread = unreadNetworks.contains(network)
            val dotSize = when {
                isActive -> 10.dp
                hasUnread -> 8.dp
                else -> 6.5.dp
            }
            val dotBorderWidth = if (isActive) 1.5.dp else borderWidth
            val colorHex = android.graphics.Color.parseColor(network.colorHex)
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .border(dotBorderWidth, borderColor, CircleShape)
                    .clip(CircleShape)
                    .background(Color(colorHex))
            )
        }
    }
}

