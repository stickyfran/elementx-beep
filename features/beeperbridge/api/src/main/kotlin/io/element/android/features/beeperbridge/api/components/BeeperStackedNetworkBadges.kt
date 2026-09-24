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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.element.android.features.beeperbridge.api.BeeperNetwork
import kotlinx.collections.immutable.ImmutableList

private const val MAX_STACKED_NETWORKS = 3
private const val OVERLAP_DP = -6

@Composable
fun BeeperStackedNetworkBadges(
    networks: ImmutableList<BeeperNetwork>,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 16.dp,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    val validNetworks = networks.filter { it != BeeperNetwork.UNKNOWN }.distinct().take(MAX_STACKED_NETWORKS)
    if (validNetworks.isEmpty()) return

    if (validNetworks.size == 1) {
        BeeperNetworkBadge(network = validNetworks.first(), modifier = modifier, size = badgeSize)
        return
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(OVERLAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        validNetworks.forEachIndexed { index, network ->
            val colorHex = android.graphics.Color.parseColor(network.colorHex)
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .zIndex(index.toFloat())
                    .border(borderWidth, borderColor, CircleShape)
                    .clip(CircleShape)
                    .background(Color(colorHex)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(network.iconResId),
                    contentDescription = network.displayName,
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}
