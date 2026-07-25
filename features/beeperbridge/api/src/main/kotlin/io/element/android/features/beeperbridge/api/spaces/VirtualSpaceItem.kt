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
