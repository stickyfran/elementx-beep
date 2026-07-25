package io.element.android.features.beeperbridge.api

import androidx.annotation.DrawableRes

enum class BeeperNetwork(
    val displayName: String,
    val colorHex: String,
    @DrawableRes val iconResId: Int // Placeholder, we will use a generic one if not available
) {
    WHATSAPP("WhatsApp", "#25D366", android.R.drawable.ic_menu_help),
    INSTAGRAM("Instagram", "#E1306C", android.R.drawable.ic_menu_help),
    TELEGRAM("Telegram", "#2AABEE", android.R.drawable.ic_menu_help),
    SIGNAL("Signal", "#3A76F0", android.R.drawable.ic_menu_help),
    DISCORD("Discord", "#5865F2", android.R.drawable.ic_menu_help),
    FACEBOOK("Facebook Messenger", "#00B2FF", android.R.drawable.ic_menu_help),
    SLACK("Slack", "#4A154B", android.R.drawable.ic_menu_help),
    GOOGLECHAT("Google Chat", "#34A853", android.R.drawable.ic_menu_help),
    UNKNOWN("Unknown", "#808080", android.R.drawable.ic_menu_help)
}
