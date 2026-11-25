package app.traced_it.ui.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.traced_it.R
import kotlinx.coroutines.flow.first

@Composable
fun EntryDetailSaveButton(
    changeInProgress: State<Boolean>,
    content: TextFieldState,
    isInsert: Boolean,
    onFinishDraft: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val enabled by remember { derivedStateOf { enabled && content.text.isNotEmpty() } }
    val onCurrentFinishDraft by rememberUpdatedState(onFinishDraft)
    var saveRequested by remember { mutableStateOf(false) }

    // Call event when clicking the button, but wait for the form to not be in the change-in-progress state
    LaunchedEffect(saveRequested) {
        if (saveRequested) {
            snapshotFlow { changeInProgress.value }
                .first { changeInProgress -> !changeInProgress }
                .let {
                    onCurrentFinishDraft()
                    saveRequested = false
                }
        }
    }

    LargeButton(
        text = stringResource(if (isInsert) R.string.detail_add_save else R.string.detail_update_save),
        onClick = { saveRequested = true },
        modifier = modifier.testTag(if (isInsert) TestTags.ENTRY_DETAIL_SAVE_INSERT else TestTags.ENTRY_DETAIL_SAVE_UPDATE),
        enabled = enabled,
    )
}
