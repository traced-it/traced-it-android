package app.traced_it.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.traced_it.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
fun EntryDetailContentControl(
    content: TextFieldState,
    onSetContent: (content: String) -> Unit,
    enabled: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    val isError by remember { derivedStateOf { content.text.isEmpty() } }
    val onCurrentSetContent by rememberUpdatedState(onSetContent)

    LaunchedEffect(content) {
        snapshotFlow { content.text.toString() }
            .drop(1) // Skip initial value
            .collectLatest(onCurrentSetContent)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    EntryDetailControl(label = stringResource(R.string.detail_content_label)) {
        StyledTextField(
            state = content,
            modifier = Modifier
                .testTag(TestTags.ENTRY_DETAIL_CONTENT)
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            enabled = enabled,
            trailingIcon = {
                StyledTextFieldClearButton(
                    content,
                    Modifier.testTag(TestTags.ENTRY_DETAIL_CONTENT_CLEAR),
                    focusRequester,
                )
            },
            isError = isError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}
