package app.traced_it.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SingleChoiceSegmentedButtonRowScope.TracedSegmentedButton(
    index: Int,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
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
            baseShape = MaterialTheme.shapes.extraSmall
        ),
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .widthIn(min = Spacing.inputHeight)
            .height(Spacing.inputHeight),
        colors = SegmentedButtonDefaults.colors(
            inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            inactiveBorderColor = MaterialTheme.colorScheme.outline,
            inactiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            activeContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            activeBorderColor = MaterialTheme.colorScheme.tertiaryContainer,
            activeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        icon = {},
    ) {
        content()
    }
}

// Previews

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        SingleChoiceSegmentedButtonRow {
            TracedSegmentedButton(
                index = 0,
                count = 2,
                selected = false,
                onClick = {},
            ) {
                Text("1x", style = MaterialTheme.typography.labelLarge)
            }
            TracedSegmentedButton(
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
