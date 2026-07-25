package io.element.android.features.beeperbridge.impl.settings

import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.beeperbridge.api.settings.BeeperNetworksEntryPoint
import io.element.android.libraries.architecture.createNode

@ContributesBinding(AppScope::class)
class DefaultBeeperNetworksEntryPoint : BeeperNetworksEntryPoint {
    override fun createNode(
        parentNode: Node,
        buildContext: BuildContext,
    ): Node {
        return parentNode.createNode<BeeperNetworksNode>(buildContext)
    }
}
