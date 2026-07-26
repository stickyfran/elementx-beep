/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
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
