package io.element.android.features.beeperbridge.api

import kotlinx.coroutines.flow.Flow

interface BeeperLabelsRepository {
    suspend fun getLabels(): List<BeeperLabel>
    fun getLabelsFlow(): Flow<List<BeeperLabel>>
    suspend fun saveLabel(label: BeeperLabel)
    suspend fun deleteLabel(labelId: String)
    suspend fun getHiddenNetworks(): Set<String>
    suspend fun setHiddenNetworks(networks: Set<String>)
}
