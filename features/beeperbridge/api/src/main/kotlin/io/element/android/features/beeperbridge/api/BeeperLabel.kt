package io.element.android.features.beeperbridge.api

data class BeeperLabel(
    val id: String,
    val title: String,
    val emoji: String? = null,
    val roomIds: List<String>,
    val isShownInInbox: Boolean = true,
    val createdAt: Long
)
