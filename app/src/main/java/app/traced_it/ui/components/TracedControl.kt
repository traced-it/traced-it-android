package app.traced_it.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import app.traced_it.ui.theme.Spacing

@Composable
fun TracedControl(
    label: String,
    labelMenu: (@Composable () -> Unit)? = null,
    labelButton: (@Composable (paddingValues: PaddingValues) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val smallWindow = !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)
    val topPadding = if (smallWindow) Spacing.small else Spacing.medium

    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = Spacing.windowPadding, top = topPadding, end = Spacing.windowPadding)
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
            verticalAlignment = Alignment.CenterVertically
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
                    PaddingValues(end = 7.dp) // Align with UnitSelect's dropdown
                )
            }
        }
        content()
    }
}
