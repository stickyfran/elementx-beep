package io.element.android.features.beeperbridge.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.features.beeperbridge.api.BeeperNetwork
import org.junit.Test

class FakeDmDetectorTest {

    private val detector = FakeDmDetector()

    @Test
    fun `detects valid fake dm`() {
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
    fun `returns false if room has a name`() {
        val result = detector.analyze(
            roomName = "My Group",
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@whatsapp_bot:beeper.local", isLocalUser = false),
                RoomMemberStub("@whatsapp_12345:beeper.local", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isFalse()
    }

    @Test
    fun `returns false if room has too many members`() {
        val result = detector.analyze(
            roomName = null,
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@whatsapp_bot:beeper.local", isLocalUser = false),
                RoomMemberStub("@whatsapp_12345:beeper.local", isLocalUser = false),
                RoomMemberStub("@whatsapp_67890:beeper.local", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isFalse()
    }

    @Test
    fun `detects incomplete fake dm if contact is missing`() {
        val result = detector.analyze(
            roomName = null,
            members = listOf(
                RoomMemberStub("@local:example.com", isLocalUser = true),
                RoomMemberStub("@whatsapp_bot:beeper.local", isLocalUser = false),
            )
        )
        assertThat(result.isFakeDm).isFalse()
        assertThat(result.isIncomplete).isTrue()
        assertThat(result.botMxid).isEqualTo("@whatsapp_bot:beeper.local")
    }
}
