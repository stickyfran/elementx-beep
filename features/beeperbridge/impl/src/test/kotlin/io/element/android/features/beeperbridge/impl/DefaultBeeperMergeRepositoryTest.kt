/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.impl

import com.google.common.truth.Truth.assertThat
import io.element.android.features.beeperbridge.api.MergedContact
import io.element.android.libraries.matrix.test.AN_AVATAR_URL
import io.element.android.libraries.matrix.test.A_ROOM_ID
import io.element.android.libraries.matrix.test.A_ROOM_ID_2
import io.element.android.libraries.preferences.test.FakePreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultBeeperMergeRepositoryTest {
    @Test
    fun `save, lookup, and sibling queries work correctly`() = runTest {
        val inMemoryDataStoreFactory = FakePreferenceDataStoreFactory()

        // We test with a fake account data service
        val accountDataService = FakeMatrixAccountDataService()
        val repository = DefaultBeeperMergeRepository(
            matrixAccountDataService = accountDataService,
            preferenceDataStoreFactory = inMemoryDataStoreFactory,
            sessionCoroutineScope = this,
        )

        val contact = MergedContact(
            id = "merge-1",
            displayName = "Bruno Musco (+5491127536793)",
            avatarMxc = AN_AVATAR_URL,
            roomIds = listOf(A_ROOM_ID.value, A_ROOM_ID_2.value),
            customWhatsAppPhone = "+5491127536793",
            customInstagramHandle = "brunomusc",
        )

        repository.saveMergedContact(contact)

        // Lookup by room should find the contact with sanitized name
        val found = repository.getMergeForRoom(A_ROOM_ID.value)
        assertThat(found).isNotNull()
        assertThat(found?.id).isEqualTo("merge-1")
        assertThat(found?.displayName).isEqualTo("Bruno Musco") // Phone suffix sanitized!

        // Sibling query
        val siblings = repository.getSiblingRoomIds(A_ROOM_ID.value)
        assertThat(siblings).containsExactly(A_ROOM_ID_2.value)

        // Add third room
        repository.addRoomToMerge("merge-1", "!room3:server")
        val updated = repository.getMergeForRoom("!room3:server")
        assertThat(updated?.roomIds).containsExactly(A_ROOM_ID.value, A_ROOM_ID_2.value, "!room3:server")

        // Remove room
        repository.removeRoomFromMerge("merge-1", "!room3:server")
        val afterRemove = repository.getMergeForRoom("!room3:server")
        assertThat(afterRemove).isNull()

        // Delete merge
        repository.deleteMergedContact("merge-1")
        assertThat(repository.getMergeForRoom(A_ROOM_ID.value)).isNull()
    }

    @Test
    fun `syncFromRemote parses FluffyBeep standard contacts dictionary correctly`() = runTest {
        val inMemoryDataStoreFactory = FakePreferenceDataStoreFactory()

        val remoteJson = """
            {
                "contacts": {
                    "uuid-abc": {
                        "displayName": "Mom (+5491100000000)",
                        "avatarMxc": "mxc://example.com/avatar123",
                        "roomIds": ["!wa:example.com", "!ig:example.com"],
                        "customWhatsAppPhone": "+5491100000000",
                        "createdAt": 1720000000
                    }
                }
            }
        """.trimIndent()

        val accountDataService = FakeMatrixAccountDataService(initialData = mapOf("com.beeper.merged_contacts" to remoteJson))
        val repository = DefaultBeeperMergeRepository(
            matrixAccountDataService = accountDataService,
            preferenceDataStoreFactory = inMemoryDataStoreFactory,
            sessionCoroutineScope = this,
        )

        repository.syncFromRemote()

        val contact = repository.getMergeForRoom("!wa:example.com")
        assertThat(contact).isNotNull()
        assertThat(contact?.displayName).isEqualTo("Mom")
        assertThat(contact?.customWhatsAppPhone).isEqualTo("+5491100000000")
        assertThat(contact?.roomIds).containsExactly("!wa:example.com", "!ig:example.com")
    }

    @Test
    fun `syncFromRemote falls back to m_fluffybeep_merges array format`() = runTest {
        val inMemoryDataStoreFactory = FakePreferenceDataStoreFactory()

        val fallbackJson = """
            {
                "merges": [
                    {
                        "id": "uuid-xyz",
                        "displayName": "Toto",
                        "roomIds": ["!wa:toto", "!tg:toto"]
                    }
                ]
            }
        """.trimIndent()

        val accountDataService = FakeMatrixAccountDataService(initialData = mapOf("m.fluffybeep.merges" to fallbackJson))
        val repository = DefaultBeeperMergeRepository(
            matrixAccountDataService = accountDataService,
            preferenceDataStoreFactory = inMemoryDataStoreFactory,
            sessionCoroutineScope = this,
        )

        repository.syncFromRemote()

        val contact = repository.getMergeForRoom("!wa:toto")
        assertThat(contact).isNotNull()
        assertThat(contact?.id).isEqualTo("uuid-xyz")
        assertThat(contact?.displayName).isEqualTo("Toto")
        assertThat(contact?.roomIds).containsExactly("!wa:toto", "!tg:toto")
    }

    @Test
    fun `saveMergedContact serializes with contacts wrapper for FluffyBeep compatibility`() = runTest {
        val inMemoryDataStoreFactory = FakePreferenceDataStoreFactory()

        val accountDataService = FakeMatrixAccountDataService()
        val repository = DefaultBeeperMergeRepository(
            matrixAccountDataService = accountDataService,
            preferenceDataStoreFactory = inMemoryDataStoreFactory,
            sessionCoroutineScope = this,
        )

        val contact = MergedContact(
            id = "merge-123",
            displayName = "Alice",
            roomIds = listOf("!r1:server", "!r2:server"),
        )
        val result = repository.saveMergedContact(contact)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: AssertionError("saveMergedContact failed")
        }

        val savedJson = accountDataService.data["com.beeper.merged_contacts"]
        assertThat(savedJson).isNotNull()
        val root = org.json.JSONObject(savedJson!!)
        assertThat(root.has("contacts")).isTrue()
        val contactsObj = root.getJSONObject("contacts")
        assertThat(contactsObj.has("merge-123")).isTrue()
        val savedContactObj = contactsObj.getJSONObject("merge-123")
        assertThat(savedContactObj.getString("displayName")).isEqualTo("Alice")
    }

    private class FakeMatrixAccountDataService(
        initialData: Map<String, String> = emptyMap()
    ) : MatrixAccountDataService(
        matrixClient = io.element.android.libraries.matrix.test.FakeMatrixClient(),
        sessionStore = io.element.android.libraries.sessionstorage.test.InMemorySessionStore(),
        okHttpClient = okhttp3.OkHttpClient()
    ) {
        val data = initialData.toMutableMap()

        override suspend fun getAccountData(type: String): Result<String?> {
            return Result.success(data[type])
        }

        override suspend fun setAccountData(type: String, contentJson: String): Result<Unit> {
            data[type] = contentJson
            return Result.success(Unit)
        }
    }
}
