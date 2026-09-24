/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */
package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.BeeperMergeRepository
import io.element.android.features.beeperbridge.api.MergedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBeeperMergeRepository : BeeperMergeRepository {
    private val contactsFlow = MutableStateFlow<Map<String, MergedContact>>(emptyMap())
    override val mergedContactsFlow: StateFlow<Map<String, MergedContact>> = contactsFlow.asStateFlow()

    override suspend fun getMergedContacts(): Map<String, MergedContact> = contactsFlow.value

    override suspend fun getMergeForRoom(roomId: String): MergedContact? {
        return contactsFlow.value.values.find { it.roomIds.contains(roomId) }
    }

    override suspend fun getSiblingRoomIds(roomId: String): List<String> {
        val contact = getMergeForRoom(roomId) ?: return emptyList()
        return contact.roomIds.filter { it != roomId }
    }

    override suspend fun saveMergedContact(contact: MergedContact): Result<Unit> {
        val updated = contactsFlow.value.toMutableMap()
        updated[contact.id] = contact
        contactsFlow.value = updated
        return Result.success(Unit)
    }

    override suspend fun deleteMergedContact(mergeId: String): Result<Unit> {
        val updated = contactsFlow.value.toMutableMap()
        updated.remove(mergeId)
        contactsFlow.value = updated
        return Result.success(Unit)
    }

    override suspend fun addRoomToMerge(mergeId: String, roomId: String): Result<Unit> {
        val contact = contactsFlow.value[mergeId] ?: return Result.failure(Exception("Not found"))
        if (!contact.roomIds.contains(roomId)) {
            val updated = contactsFlow.value.toMutableMap()
            updated[mergeId] = contact.copy(roomIds = contact.roomIds + roomId)
            contactsFlow.value = updated
        }
        return Result.success(Unit)
    }

    override suspend fun removeRoomFromMerge(mergeId: String, roomId: String): Result<Unit> {
        val contact = contactsFlow.value[mergeId] ?: return Result.success(Unit)
        val updatedRoomIds = contact.roomIds.filter { it != roomId }
        val updated = contactsFlow.value.toMutableMap()
        if (updatedRoomIds.isEmpty()) {
            updated.remove(mergeId)
        } else {
            updated[mergeId] = contact.copy(roomIds = updatedRoomIds)
        }
        contactsFlow.value = updated
        return Result.success(Unit)
    }

    override suspend fun syncFromRemote(): Result<Unit> = Result.success(Unit)

    fun emitContacts(contacts: Map<String, MergedContact>) {
        contactsFlow.value = contacts
    }
}
