/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import io.element.android.features.beeperbridge.api.BeeperNetwork

object BeeperNetworkMap {
    private val prefixMap = mapOf(
        "whatsapp" to BeeperNetwork.WHATSAPP,
        "whatsappgo" to BeeperNetwork.WHATSAPP,
        "instagram" to BeeperNetwork.INSTAGRAM,
        "instagramgo" to BeeperNetwork.INSTAGRAM,
        "telegram" to BeeperNetwork.TELEGRAM,
        "telegramgo" to BeeperNetwork.TELEGRAM,
        "signal" to BeeperNetwork.SIGNAL,
        "signalgo" to BeeperNetwork.SIGNAL,
        "discord" to BeeperNetwork.DISCORD,
        "discordgo" to BeeperNetwork.DISCORD,
        "facebook" to BeeperNetwork.FACEBOOK,
        "facebookgo" to BeeperNetwork.FACEBOOK,
        "slack" to BeeperNetwork.SLACK,
        "slackgo" to BeeperNetwork.SLACK,
        "googlechat" to BeeperNetwork.GOOGLECHAT,
        "googlechatgo" to BeeperNetwork.GOOGLECHAT
    )

    fun isBeeperBot(userId: String): Boolean {
        if (!userId.contains("beeper")) return false
        val localpart = userId.substringAfter("@").substringBefore(":")
        return localpart.endsWith("bot")
    }

    fun detectNetwork(userId: String): BeeperNetwork? {
        val localpart = userId.substringAfter("@").substringBefore(":")
        val prefix = localpart.substringBefore("_").substringBefore("bot")

        return prefixMap[prefix.lowercase()]
    }

    fun getBaseNetworkKey(key: String): String {
        return if (key.endsWith("go")) {
            key.substringBeforeLast("go")
        } else {
            key
        }
    }
}
