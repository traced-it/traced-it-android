package app.traced_it.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.traced_it.R
import app.traced_it.ui.theme.AppTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.time.Instant

@Composable
fun EntryDetailCreatedAtControl(
    initialCreatedAt: Long,
    viewportBounds: LayoutBoundsHolder?,
    onChangeInProgress: (changeInProgress: Boolean) -> Unit,
    onSetCreatedAt: (createdAt: Long) -> Unit,
    enabled: Boolean = true,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val timeZone = rememberSystemTimeZone()

    val onCurrentChangeInProgress by rememberUpdatedState(onChangeInProgress)
    val onCurrentSetCreatedAt by rememberUpdatedState(onSetCreatedAt)

    val state = rememberDateTimePickerState(initialCreatedAt, timeZone)

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .drop(1) // Skip initial value
            .collectLatest(onCurrentChangeInProgress)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.timeInMillis }
            .drop(1) // Skip initial value
            .collectLatest(onCurrentSetCreatedAt)
    }

    EntryDetailControl(
        label = stringResource(R.string.detail_created_at_label),
        labelButton = {
            TextButton(
                onClick = {
                    state.reset()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                modifier = Modifier.padding(end = 7.dp), // Align with UnitSelect's dropdown
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    stringResource(R.string.detail_created_at_reset),
                    modifier = Modifier.testTag(TestTags.ENTRY_DETAIL_CREATED_AT_RESET),
                    fontWeight = FontWeight.Normal,
                )
            }
        },
    ) {
        DateTimePicker(
            state = state,
            viewportBounds = viewportBounds,
            enabled = enabled,
        )
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            EntryDetailCreatedAtControl(
                initialCreatedAt = Instant.parse("2023-01-02T22:35:01+01:00").toEpochMilliseconds(),
                viewportBounds = null,
                onChangeInProgress = {},
                onSetCreatedAt = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DisabledPreview() {
    AppTheme {
        Surface {
            EntryDetailCreatedAtControl(
                initialCreatedAt = Instant.parse("2023-01-02T22:35:01+01:00").toEpochMilliseconds(),
                viewportBounds = null,
                onChangeInProgress = {},
                onSetCreatedAt = {},
                enabled = false,
            )
        }
    }
}
