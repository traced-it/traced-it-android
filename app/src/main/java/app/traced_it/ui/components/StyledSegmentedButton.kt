package app.traced_it.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing

@Composable
fun SingleChoiceSegmentedButtonRowScope.StyledSegmentedButton(
    index: Int,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    SegmentedButton(
        selected,
        onClick = {
            focusRequester.requestFocus()
            onClick()
        },
        shape = SegmentedButtonDefaults.itemShape(
            index = index,
            count = count,
            baseShape = MaterialTheme.shapes.extraSmall,
        ),
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .widthIn(min = Spacing.inputHeight)
            .height(Spacing.inputHeight),
        enabled = enabled,
        colors = SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            activeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            activeBorderColor = MaterialTheme.colorScheme.tertiaryContainer,
            inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            inactiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            inactiveBorderColor = MaterialTheme.colorScheme.outline,
            disabledActiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledActiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledActiveBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledInactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledInactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledInactiveBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        icon = {},
        label = content,
    )
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        SingleChoiceSegmentedButtonRow {
            StyledSegmentedButton(
                index = 0,
                count = 2,
                selected = false,
                onClick = {},
            ) {
                Text("1x", style = MaterialTheme.typography.labelLarge)
            }
            StyledSegmentedButton(
                index = 1,
                count = 2,
                selected = true,
                onClick = {},
            ) {
                Text("2x", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DisabledPreview() {
    AppTheme {
        SingleChoiceSegmentedButtonRow {
            StyledSegmentedButton(
                index = 0,
                count = 2,
                selected = false,
                onClick = {},
                enabled = false,
            ) {
                Text("1x", style = MaterialTheme.typography.labelLarge)
            }
            StyledSegmentedButton(
                index = 1,
                count = 2,
                selected = true,
                onClick = {},
                enabled = false,
            ) {
                Text("2x", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
