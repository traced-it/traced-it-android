package app.traced_it.ui.entry

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import app.traced_it.ui.components.TracedBottomButton
import app.traced_it.ui.components.TracedScaffold
import app.traced_it.ui.components.TracedTextField
import app.traced_it.ui.components.TracedTimePicker
import app.traced_it.ui.components.TracedTopAppBar
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing

sealed class EntryDetailAction(val entry: Entry) {
    class Edit(entry: Entry) : EntryDetailAction(entry)
    class Prefill(entry: Entry) : EntryDetailAction(entry)
}

@Composable
fun EntryDetailDialog(
    action: EntryDetailAction,
    latestEntryUnit: EntryUnit? = null,
    onInsert: (Entry) -> Unit = {},
    onUpdate: (Entry) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val resources = LocalResources.current

    var contentFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = action.entry.content,
                selection = TextRange(action.entry.content.length),
            )
        )
    }
    val contentFocusRequester = remember { FocusRequester() }
    var unit by remember {
        mutableStateOf(
            if (action.entry.amountUnit in visibleUnits) {
                action.entry.amountUnit
            } else if (action.entry.amount != 0.0) {
                // If we're editing or prefilling an entry that has a deprecated unit (such as smallNumbersChoiceUnit),
                // convert the unit into doubleUnit.
                doubleUnit
            } else {
                noneUnit
            }
        )
    }
    var amountRaw by remember { mutableStateOf(unit.format(resources, action.entry.amount)) }
    var visibleUnit by remember {
        mutableStateOf(
            unit.takeIf { it in visibleUnits }
                ?: latestEntryUnit.takeIf { it in visibleUnits }
                ?: defaultVisibleUnit
        )
    }

    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.detail_content_label),
                    Modifier
                        .padding(horizontal = Spacing.windowPadding)
                        .padding(top = Spacing.medium, bottom = Spacing.small),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TracedTextField(
                    value = contentFieldValue,
                    onValueChange = { contentFieldValue = it },
                    modifier = Modifier
                        .testTag("entryDetailContentTextField")
                        .focusRequester(contentFocusRequester)
                        .padding(horizontal = Spacing.windowPadding)
                        .fillMaxWidth(),
                    isError = contentFieldValue.text.isEmpty(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                )
                UnitSelect(
                    amountRaw = amountRaw,
                    selectedUnit = unit,
                    visibleUnit = visibleUnit,
                    modifier = Modifier.padding(top = Spacing.medium * 2),
                    onAmountRawChange = { amountRaw = it },
                    onUnitChange = { unit = it },
                    onVisibleUnitChange = { newVisibleUnit ->
                        unit = noneUnit
                        visibleUnit = newVisibleUnit
                        amountRaw = ""
                    },
                )
                TracedTimePicker(
                    value = System.currentTimeMillis(), // TODO
                    onValueChange = {}, // TODO
                    modifier = Modifier.padding(top = Spacing.medium * 2),
                )
            }
            HorizontalDivider()
            TracedBottomButton(
                text = stringResource(
                    if (action is EntryDetailAction.Edit) {
                        R.string.detail_update_save
                    } else {
                        R.string.detail_add_save
                    }
                ),
                onClick = {
                    val amount = unit.parse(resources, amountRaw)
                    if (action is EntryDetailAction.Edit) {
                        onUpdate(
                            action.entry.copy(
                                amount = amount,
                                amountUnit = unit,
                                content = contentFieldValue.text,
                            )
                        )
                    } else {
                        onInsert(
                            Entry(
                                amount = amount,
                                amountUnit = unit,
                                content = contentFieldValue.text,
                            )
                        )
                    }
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
        EntryDetailDialog(EntryDetailAction.Prefill(Entry()))
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        EntryDetailDialog(EntryDetailAction.Prefill(Entry()))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrefilledPreview() {
    AppTheme {
        EntryDetailDialog(EntryDetailAction.Prefill(defaultFakeEntries[0]))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditPreview() {
    AppTheme {
        EntryDetailDialog(EntryDetailAction.Edit(defaultFakeEntries[0]))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InvisibleUnitPreview() {
    AppTheme {
        EntryDetailDialog(
            EntryDetailAction.Edit(
                Entry(
                    content = "Small numbers choice",
                    amount = 2.0,
                    amountUnit = smallNumbersChoiceUnit,
                )
            )
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 1024,
    heightDp = 768,
)
@Composable
private fun PortraitPreview() {
    AppTheme {
        EntryDetailDialog(EntryDetailAction.Prefill(defaultFakeEntries[0]))
    }
}
