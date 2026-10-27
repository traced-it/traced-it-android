package app.traced_it.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import app.traced_it.ui.theme.Spacing

@Composable
fun EntryDetailControl(
    label: String,
    labelMenu: (@Composable () -> Unit)? = null,
    labelButton: (@Composable (paddingValues: PaddingValues) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val smallWindow = !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.windowPadding)
            .padding(top = if (smallWindow) Spacing.small else Spacing.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .run {
                    if (labelMenu == null && labelButton == null) {
                        this.padding(bottom = Spacing.small)
                    } else {
                        this
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (labelMenu != null) {
                labelMenu()
            }
            if (labelButton != null) {
                labelButton(
                    PaddingValues(end = 7.dp) // Align with unit selection menu
                )
            }
        }
        content()
    }
}
