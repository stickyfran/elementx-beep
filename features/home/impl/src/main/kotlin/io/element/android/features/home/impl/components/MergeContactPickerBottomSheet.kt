/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.beeperbridge.api.components.BeeperNetworkBadge
import io.element.android.features.home.impl.model.RoomListRoomSummary
import io.element.android.features.home.impl.roomlist.RoomListState
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.TextField
import io.element.android.libraries.matrix.api.core.RoomId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeContactPickerBottomSheet(
    state: RoomListState.MergePickerMenu.Shown,
    onDismiss: () -> Unit,
    onMergeSelected: (siblingRoomId: RoomId, contactName: String) -> Unit,
    onUnmergeRoom: (roomId: RoomId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var customDisplayName by remember { mutableStateOf(state.existingMergedContact?.displayName ?: state.primaryRoomName) }

    val filteredCandidates = remember(state.candidateRooms, searchQuery) {
        if (searchQuery.isBlank()) {
            state.candidateRooms
        } else {
            state.candidateRooms.filter {
                it.name?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        scrollable = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Fusionar contacto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Une múltiples chats (WhatsApp, Instagram, etc.) de una misma persona en una sola conversación unificada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TextField(
                value = customDisplayName,
                onValueChange = { customDisplayName = it },
                label = "Nombre unificado",
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            val existingContact = state.existingMergedContact
            if (existingContact != null && existingContact.roomIds.size > 1) {
                Text(
                    text = "Chats vinculados actualmente (${existingContact.roomIds.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                existingContact.roomIds.forEach { linkedId ->
                    val isPrimary = linkedId == state.primaryRoomId.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isPrimary) "${state.primaryRoomName} (Actual)" else linkedId,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isPrimary) {
                            IconButton(
                                onClick = { onUnmergeRoom(RoomId(linkedId)) }
                            ) {
                                Icon(
                                    imageVector = CompoundIcons.Delete(),
                                    contentDescription = "Desvincular",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Seleccionar otro chat para fusionar:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Buscar conversación...",
                leadingIcon = {
                    Icon(imageVector = CompoundIcons.Search(), contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                if (filteredCandidates.isEmpty()) {
                    item {
                        Text(
                            text = "No se encontraron otros chats directos disponibles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(filteredCandidates, key = { it.id }) { candidate ->
                        CandidateRoomRow(
                            room = candidate,
                            onClick = {
                                onMergeSelected(candidate.roomId, customDisplayName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateRoomRow(
    room: RoomListRoomSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Avatar(
                avatarData = room.avatarData,
                avatarType = AvatarType.Room(
                    heroes = room.heroes,
                    isTombstoned = room.isTombstoned,
                ),
            )
            val network = room.beeperData?.network
            if (network != null) {
                BeeperNetworkBadge(
                    network = network,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = room.name ?: "Chat",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            val networkName = room.beeperData?.network?.displayName
            if (networkName != null) {
                Text(
                    text = networkName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Button(
            text = "Fusionar",
            onClick = onClick
        )
    }
}
