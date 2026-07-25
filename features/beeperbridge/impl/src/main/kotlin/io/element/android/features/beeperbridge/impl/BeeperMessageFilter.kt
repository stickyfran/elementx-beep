package io.element.android.features.beeperbridge.impl

object BeeperMessageFilter {
    private val realEventTypes = setOf(
        "m.room.message",
        "m.room.encrypted",
        "m.sticker"
    )

    fun isRealMessage(eventType: String): Boolean {
        if (eventType.startsWith("com.beeper.")) return false
        return eventType in realEventTypes
    }
}
