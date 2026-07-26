package io.element.android.features.beeperbridge.test

import io.element.android.features.beeperbridge.api.BeeperLabel
import io.element.android.features.beeperbridge.api.BeeperLabelsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBeeperLabelsRepository : BeeperLabelsRepository {
    private val labelsFlow = MutableStateFlow<List<BeeperLabel>>(emptyList())
    private var hiddenNetworks = emptySet<String>()

    override suspend fun getLabels(): List<BeeperLabel> = labelsFlow.value

    override fun getLabelsFlow(): Flow<List<BeeperLabel>> = labelsFlow.asStateFlow()

    override suspend fun saveLabel(label: BeeperLabel) {
        labelsFlow.value = labelsFlow.value.filter { it.id != label.id } + label
    }

    override suspend fun deleteLabel(labelId: String) {
        labelsFlow.value = labelsFlow.value.filter { it.id != labelId }
    }

    override suspend fun getHiddenNetworks(): Set<String> = hiddenNetworks

    override suspend fun setHiddenNetworks(networks: Set<String>) {
        hiddenNetworks = networks
    }
    
    fun emitLabels(labels: List<BeeperLabel>) {
        labelsFlow.value = labels
    }
}
