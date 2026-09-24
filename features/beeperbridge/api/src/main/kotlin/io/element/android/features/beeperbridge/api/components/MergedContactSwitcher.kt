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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import kotlinx.collections.immutable.ImmutableList

private const val ALPHA_SELECTED = 0.15f
private const val ALPHA_SURFACE = 0.5f

@Composable
fun MergedContactSwitcher(
    channels: ImmutableList<MergedChannelItem>,
    currentRoomId: String,
    onSelectChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (channels.size <= 1) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ALPHA_SURFACE),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            channels.forEach { channel ->
                val isSelected = channel.roomId == currentRoomId
                val network = channel.network
                val colorHex = android.graphics.Color.parseColor(network.colorHex)

                val pillBackground = if (isSelected) {
                    Color(colorHex).copy(alpha = ALPHA_SELECTED)
                } else {
                    MaterialTheme.colorScheme.surface
                }

                val pillBorder = if (isSelected) {
                    Color(colorHex)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = pillBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .background(pillBackground)
                        .clickable { onSelectChannel(channel.roomId) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(34.dp)) {
                        if (channel.avatarData != null) {
                            Avatar(
                                avatarData = channel.avatarData,
                                avatarType = AvatarType.Room(),
                                forcedAvatarSize = 30.dp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(network.iconResId),
                                    contentDescription = network.displayName,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Network badge overlay on bottom right of the avatar
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                        )

                        // Unread count badge on top right
                        if (channel.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (channel.unreadCount > 99) "99+" else channel.unreadCount.toString(),
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = channel.displayName ?: network.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(colorHex) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
