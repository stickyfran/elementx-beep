/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.features.beeperbridge.api.BeeperNetwork
import org.junit.Test

class BridgedDmDetectorTest {
    private val detector = BridgedDmDetector()

    @Test
    fun `detects valid fake dm without room name`() {
        val result = detector.analyze(
            roomName = null,
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@whatsapp_bot:beeper.local", isLocalUser = false),
                RoomMemberStub("@whatsapp_12345:beeper.local", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isTrue()
        assertThat(result.botMxid).isEqualTo("@whatsapp_bot:beeper.local")
        assertThat(result.contactMxid).isEqualTo("@whatsapp_12345:beeper.local")
        assertThat(result.network).isEqualTo(BeeperNetwork.WHATSAPP)
    }

    @Test
    fun `detects valid fake dm even when room has a contact name`() {
        val result = detector.analyze(
            roomName = "Bruno Musco",
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@whatsappbot:custom.homeserver.org", isLocalUser = false),
                RoomMemberStub("@whatsapp_5491127536793:custom.homeserver.org", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isTrue()
        assertThat(result.botMxid).isEqualTo("@whatsappbot:custom.homeserver.org")
        assertThat(result.contactMxid).isEqualTo("@whatsapp_5491127536793:custom.homeserver.org")
        assertThat(result.network).isEqualTo(BeeperNetwork.WHATSAPP)
    }

    @Test
    fun `detects network for group chats with many members but marks fake dm false`() {
        val result = detector.analyze(
            roomName = "Family Group",
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@whatsappbot:custom.homeserver.org", isLocalUser = false),
                RoomMemberStub("@whatsapp_1111:custom.homeserver.org", isLocalUser = false),
                RoomMemberStub("@whatsapp_2222:custom.homeserver.org", isLocalUser = false),
                RoomMemberStub("@whatsapp_3333:custom.homeserver.org", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isFalse()
        assertThat(result.network).isEqualTo(BeeperNetwork.WHATSAPP)
    }

    @Test
    fun `detects instagram fake dm`() {
        val result = detector.analyze(
            roomName = "Friend",
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@metabot:custom.homeserver.org", isLocalUser = false),
                RoomMemberStub("@instagram_user:custom.homeserver.org", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isTrue()
        assertThat(result.network).isEqualTo(BeeperNetwork.INSTAGRAM)
        assertThat(result.contactMxid).isEqualTo("@instagram_user:custom.homeserver.org")
    }

    @Test
    fun `detects incomplete fake dm if contact is missing`() {
        val result = detector.analyze(
            roomName = null,
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@whatsappbot:custom.homeserver.org", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isFalse()
        assertThat(result.isIncomplete).isTrue()
        assertThat(result.botMxid).isEqualTo("@whatsappbot:custom.homeserver.org")
        assertThat(result.network).isEqualTo(BeeperNetwork.WHATSAPP)
    }
}
