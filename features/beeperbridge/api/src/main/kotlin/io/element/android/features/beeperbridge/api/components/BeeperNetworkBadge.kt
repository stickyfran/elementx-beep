/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.api.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.features.beeperbridge.api.BeeperNetwork

@Composable
fun BeeperNetworkBadge(
    network: BeeperNetwork,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    if (network == BeeperNetwork.UNKNOWN) return

    val colorHex = android.graphics.Color.parseColor(network.colorHex)
    
    Box(
        modifier = modifier
            .size(size)
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
