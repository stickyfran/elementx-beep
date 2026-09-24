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

private const val MAX_STACKED_NETWORKS = 4

@Composable
fun BeeperStackedNetworkBadges(
    networks: ImmutableList<BeeperNetwork>,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val validNetworks = networks.filter { it != BeeperNetwork.UNKNOWN }.distinct().take(MAX_STACKED_NETWORKS)
    if (validNetworks.isEmpty()) return

    if (validNetworks.size == 1) {
        BeeperNetworkBadge(
            network = validNetworks.first(),
            modifier = modifier,
            size = 9.dp,
            borderWidth = borderWidth,
            borderColor = borderColor,
        )
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        validNetworks.forEach { network ->
            val colorHex = android.graphics.Color.parseColor(network.colorHex)
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .border(borderWidth, borderColor, CircleShape)
                    .clip(CircleShape)
                    .background(Color(colorHex))
            )
        }
    }
}
