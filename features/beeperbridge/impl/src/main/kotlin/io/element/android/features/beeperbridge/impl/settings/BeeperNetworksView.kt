package io.element.android.features.beeperbridge.impl.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.element.android.libraries.designsystem.components.preferences.PreferencePage
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.ListItemContent
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.compound.tokens.generated.CompoundIcons

@Composable
fun BeeperNetworksView(
    state: BeeperNetworksState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferencePage(
        modifier = modifier,
        onBackClick = onBackClick,
        title = "Beeper Networks",
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(state.networks) { network ->
                ListItem(
                    headlineContent = { Text(network.displayName) },
                    leadingContent = ListItemContent.Icon(IconSource.Vector(CompoundIcons.Chat())), // Placeholder icon
                    trailingContent = ListItemContent.Text("Connect"), // We'll update this later based on actual status
                    onClick = {
                        // TODO: trigger login flow
                    }
                )
            }
        }
    }
}

@io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
@Composable
internal fun BeeperNetworksViewLightPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(BeeperNetworksStateProvider::class) state: BeeperNetworksState
) = io.element.android.libraries.designsystem.preview.ElementPreviewLight {
    BeeperNetworksView(
        state = state,
        onBackClick = {},
    )
}

@io.element.android.libraries.designsystem.preview.PreviewWithLargeHeight
@Composable
internal fun BeeperNetworksViewDarkPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(BeeperNetworksStateProvider::class) state: BeeperNetworksState
) = io.element.android.libraries.designsystem.preview.ElementPreviewDark {
    BeeperNetworksView(
        state = state,
        onBackClick = {},
    )
}
