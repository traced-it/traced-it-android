package app.traced_it.ui.entry

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.layoutBounds
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.R
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.data.local.database.*
import app.traced_it.ui.components.*
import app.traced_it.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface EntryDetailAction {
    class New : EntryDetailAction
    data class Edit(val entry: Entry) : EntryDetailAction
    data class Prefill(val entry: Entry) : EntryDetailAction
}

@Composable
fun EntryDetailDialog(
    action: EntryDetailAction,
    latestEntryUnitFlow: StateFlow<EntryUnit?>,
    onInsert: (Entry) -> Unit,
    onUpdate: (Entry) -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    val viewportBounds = remember { LayoutBoundsHolder() }

    val contentFocusRequester = remember { FocusRequester() }
    var contentFieldValue by retain {
        mutableStateOf(
            when (action) {
                is EntryDetailAction.New -> ""
                is EntryDetailAction.Edit -> action.entry.content
                is EntryDetailAction.Prefill -> action.entry.content
            }.let { content ->
                TextFieldValue(
                    text = content,
                    selection = TextRange(content.length),
                )
            }
        )
    }
    var createdAt by retain {
        mutableLongStateOf(
            when (action) {
                is EntryDetailAction.New -> System.currentTimeMillis()
                is EntryDetailAction.Edit -> action.entry.createdAt
                is EntryDetailAction.Prefill -> System.currentTimeMillis()
            }
        )
    }
    var unit by retain {
        mutableStateOf(
            when (action) {
                is EntryDetailAction.New -> null
                is EntryDetailAction.Edit -> action.entry
                is EntryDetailAction.Prefill -> action.entry
            }.let { entry ->
                if (entry == null) {
                    noneUnit
                } else if (entry.amountUnit in visibleUnits) {
                    entry.amountUnit
                } else if (entry.amount != 0.0) {
                    // If we're editing or prefilling an entry that has a deprecated unit (such as smallNumbersChoiceUnit),
                    // convert the unit into doubleUnit.
                    doubleUnit
                } else {
                    noneUnit
                }
            }
        )
    }
    var amountRaw by retain {
        mutableStateOf(
            unit.format(
                resources,
                when (action) {
                    is EntryDetailAction.New -> 0.0
                    is EntryDetailAction.Edit -> action.entry.amount
                    is EntryDetailAction.Prefill -> action.entry.amount
                }
            )
        )
    }
    var saveRequested by remember { mutableStateOf(false) }
    var changeInProgress by remember { mutableStateOf(false) }

    // Focus the content text field after opening the detail dialog
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }

    // Save the entry when clicking the save button, once the dialog is not in the change-in-progress state
    if (saveRequested && !changeInProgress) {
        val amount = unit.parse(resources, amountRaw)
        when (action) {
            is EntryDetailAction.Edit ->
                onUpdate(
                    action.entry.copy(
                        amount = amount,
                        amountUnit = unit,
                        content = contentFieldValue.text,
                        createdAt = createdAt,
                    )
                )

            else ->
                onInsert(
                    Entry(
                        amount = amount,
                        amountUnit = unit,
                        content = contentFieldValue.text,
                        createdAt = createdAt,
                    )
                )
        }
        @Suppress("AssignedValueIsNeverRead")
        saveRequested = false
    }

    TracedScaffold(
        topBar = {
            TracedTopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (action is EntryDetailAction.Edit) {
                                R.string.detail_update_title
                            } else {
                                R.string.detail_add_title
                            }
                        )
                    )
                },
                actions = {
                    IconButton({ onDismiss() }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.detail_cancel),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .layoutBounds(viewportBounds)
                    .verticalScroll(rememberScrollState())
            ) {
                TracedControl(
                    label = stringResource(R.string.detail_content_label),
                ) {
                    TracedTextField(
                        value = contentFieldValue,
                        onValueChange = { contentFieldValue = it },
                        modifier = Modifier
                            .testTag("entryDetailContentTextField")
                            .focusRequester(contentFocusRequester)
                            .fillMaxWidth(),
                        isError = contentFieldValue.text.isEmpty(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                    )
                }
                UnitControl(
                    amountRaw = amountRaw,
                    selectedUnit = unit,
                    latestEntryUnitFlow = latestEntryUnitFlow,
                    onAmountRawChange = { amountRaw = it },
                    onUnitChange = { unit = it },
                    onVisibleUnitChange = {
                        unit = noneUnit
                        amountRaw = ""
                    },
                )
                CreatedAtControl(
                    action = action,
                    onValueChange = {
                        @Suppress("AssignedValueIsNeverRead")
                        createdAt = it
                    },
                    onChangeInProgress = {
                        @Suppress("AssignedValueIsNeverRead")
                        changeInProgress = it
                    },
                    viewportBounds = viewportBounds,
                )
            }
            HorizontalDivider()
            TracedBottomButton(
                text = stringResource(
                    when (action) {
                        is EntryDetailAction.Edit -> R.string.detail_update_save
                        else -> R.string.detail_add_save
                    }
                ),
                onClick = {
                    @Suppress("AssignedValueIsNeverRead")
                    saveRequested = true
                },
                modifier = Modifier.testTag("entryDetailSaveButton"),
                enabled = contentFieldValue.text.isNotEmpty(),
            )
        }
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        EntryDetailDialog(
            action = EntryDetailAction.New(),
            latestEntryUnitFlow = MutableStateFlow(null),
            onInsert = {},
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        EntryDetailDialog(
            action = EntryDetailAction.New(),
            latestEntryUnitFlow = MutableStateFlow(null),
            onInsert = {},
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrefilledPreview() {
    AppTheme {
        EntryDetailDialog(
            action = EntryDetailAction.Prefill(defaultFakeEntries[0]),
            latestEntryUnitFlow = MutableStateFlow(null),
            onInsert = {},
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditPreview() {
    AppTheme {
        EntryDetailDialog(
            action = EntryDetailAction.Edit(defaultFakeEntries[0]),
            latestEntryUnitFlow = MutableStateFlow(null),
            onInsert = {},
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InvisibleUnitPreview() {
    AppTheme {
        EntryDetailDialog(
            action = EntryDetailAction.Edit(
                Entry(
                    content = "Small numbers choice",
                    amount = 2.0,
                    amountUnit = smallNumbersChoiceUnit,
                )
            ),
            latestEntryUnitFlow = MutableStateFlow(null),
            onInsert = {},
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "id:Nexus 5",
)
@Composable
private fun SmallPreview() {
    AppTheme {
        EntryDetailDialog(
            action = EntryDetailAction.Prefill(defaultFakeEntries[0]),
            latestEntryUnitFlow = MutableStateFlow(null),
            onInsert = {},
            onUpdate = {},
            onDismiss = {},
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=1280dp,height=800dp,dpi=240",
)
@Composable
private fun TabletPreview() {
    AppTheme {
        EntryDetailDialog(
            action = EntryDetailAction.Prefill(defaultFakeEntries[0]),
            latestEntryUnitFlow = MutableStateFlow(null),
            onInsert = {},
            onUpdate = {},
            onDismiss = {},
        )
    }
}
