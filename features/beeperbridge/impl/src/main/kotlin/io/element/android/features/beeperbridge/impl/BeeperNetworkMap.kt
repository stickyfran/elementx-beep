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

    private val knownBridgeBots = setOf(
        "whatsappbot",
        "instagrambot",
        "telegrambot",
        "signalbot",
        "discordbot",
        "slackbot",
        "facebookbot",
        "googlechatbot",
        "imessagebot",
        "twitterbot",
        "linkedinbot",
        "bridgebot",
        "mautrixbot",
        "meta_bot",
        "metabot",
        "bbot"
    )

    fun isBeeperBot(userId: String): Boolean {
        if (userId.isEmpty()) return false
        val localpart = userId.substringAfter("@").substringBefore(":").lowercase()
        if (knownBridgeBots.contains(localpart)) return true
        if (localpart.endsWith("bot") || localpart.endsWith("gobot")) return true
        if (userId.contains("beeper") && localpart.endsWith("bot")) return true
        return false
    }

    fun detectNetwork(userId: String): BeeperNetwork? {
        val localpart = userId.substringAfter("@").substringBefore(":").lowercase()
        val rawPrefix = localpart.replaceFirst(Regex("^_+"), "")

        for ((key, network) in prefixMap) {
            if (matchesNetworkPrefix(rawPrefix, key)) {
                return network
            }
        }
        return null
    }

    private fun matchesNetworkPrefix(rawPrefix: String, key: String): Boolean {
        if (rawPrefix.startsWith("${key}_")) return true
        if (rawPrefix == "${key}bot" || rawPrefix == "${key}gobot") return true
        return rawPrefix.startsWith(key)
    }

    fun detectNetworkFromIdentifier(identifier: String): BeeperNetwork? {
        val lower = identifier.lowercase()
        for ((key, network) in prefixMap) {
            if (lower.contains(key)) {
                return network
            }
        }
        return null
    }

    fun getBaseNetworkKey(key: String): String {
        return if (key.endsWith("go")) {
            key.substringBeforeLast("go")
        } else {
            key
        }
    }
}
