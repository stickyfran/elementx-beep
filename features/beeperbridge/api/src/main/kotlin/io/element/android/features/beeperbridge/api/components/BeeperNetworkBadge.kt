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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.features.beeperbridge.api.BeeperNetwork

@Composable
fun BeeperNetworkBadge(
    network: BeeperNetwork,
    modifier: Modifier = Modifier,
    size: Dp = 9.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.surface,
) {
    if (network == BeeperNetwork.UNKNOWN) return

    val colorHex = android.graphics.Color.parseColor(network.colorHex)

    Box(
        modifier = modifier
            .size(size)
            .border(borderWidth, borderColor, CircleShape)
            .clip(CircleShape)
            .background(Color(colorHex))
    )
}
