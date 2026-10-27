package app.traced_it.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.layoutBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.R
import app.traced_it.data.database.ActiveEntryUnit
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.EntryDraft
import app.traced_it.data.database.NoneUnit
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.topAppBarColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun EntryDetail(
    defaultUnit: StateFlow<ActiveEntryUnit>,
    initialDraft: EntryDraft,
    onDismissDraft: (draft: EntryDraft) -> Unit,
    onFinishDraft: (draft: EntryDraft) -> Unit,
    onSetDefaultUnit: (defaultUnit: ActiveEntryUnit) -> Unit,
    onUpsertDraft: (draft: EntryDraft) -> Unit,
) {
    // Remember all fields until a draft for a different entry is loaded, so that a filled fields are not replaced with
    // initial values when rotating the screen. Use retain, because rememberSaveable doesn't refresh when entryUid
    // changes for some reason.
    val initialAmount = retain(initialDraft.entryUid) { initialDraft.amount }
    val initialAmountUnit = retain(initialDraft.entryUid) { initialDraft.amountUnit }
    val initialContent = retain(initialDraft.entryUid) { initialDraft.content }
    val initialCreatedAt = retain(initialDraft.entryUid) { initialDraft.createdAt }

    /**
     * Current state of the draft, which will be saved in the database.
     */
    var draft by retain(initialDraft.entryUid) { mutableStateOf(initialDraft) }

    /**
     * Content state is hoisted here, because the save button depends on it.
     */
    val content = retain(initialContent) {
        TextFieldState(
            initialText = initialContent,
            initialSelection = TextRange(initialContent.length), // Put cursor at the end of the text
        )
    }

    val isInsert = initialDraft.entryUid == -1

    BackHandler(true) {
        onDismissDraft(draft)
    }

    StyledScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isInsert) {
                                R.string.detail_add_title
                            } else {
                                R.string.detail_update_title
                            }
                        )
                    )
                },
                actions = {
                    IconButton(
                        { onDismissDraft(draft) },
                        Modifier.testTag(TestTags.ENTRY_DETAIL_DISMISS),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.detail_cancel),
                        )
                    }
                },
                colors = topAppBarColors(),
            )
        },
    ) { innerPadding ->
        val changeInProgress = remember { mutableStateOf(false) }

        // Layout bounds of the scrollable column, so that the date time picker knows whether it's visible
        val viewportBounds = remember { LayoutBoundsHolder() }

        Column(
            Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .layoutBounds(viewportBounds)
                    .verticalScroll(rememberScrollState())
            ) {
                EntryDetailContentControl(
                    content = content,
                    onSetContent = { content ->
                        draft = draft.copy(content = content)
                        onUpsertDraft(draft)
                    },
                )
                EntryDetailAmountControl(
                    defaultUnit = defaultUnit,
                    initialAmount = initialAmount,
                    initialAmountUnit = initialAmountUnit,
                    onSetAmountAndUnit = { amount, amountUnit ->
                        draft = draft.copy(amount = amount, amountUnit = amountUnit)
                        onUpsertDraft(draft)
                    },
                    onSetDefaultUnit = onSetDefaultUnit,
                )
                EntryDetailCreatedAtControl(
                    initialCreatedAt = initialCreatedAt,
                    viewportBounds = viewportBounds,
                    onChangeInProgress = { changeInProgress.value = it },
                    onSetCreatedAt = { createdAt ->
                        draft = draft.copy(createdAt = createdAt)
                        onUpsertDraft(draft)
                    },
                )
            }
            HorizontalDivider()
            EntryDetailSaveButton(
                changeInProgress = changeInProgress,
                content = content,
                isInsert = isInsert,
                onFinishDraft = { onFinishDraft(draft) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            EntryDetail(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialDraft = EntryDraft(
                    amount = 0.0,
                    amountUnit = NoneUnit,
                    content = "",
                    createdAt = System.currentTimeMillis(),
                    entryUid = -1,
                    uid = 0,
                ),
                onDismissDraft = {},
                onFinishDraft = {},
                onSetDefaultUnit = {},
                onUpsertDraft = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        Surface {
            EntryDetail(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialDraft = EntryDraft(
                    amount = 0.0,
                    amountUnit = NoneUnit,
                    content = "",
                    createdAt = System.currentTimeMillis(),
                    entryUid = -1,
                    uid = 0,
                ),
                onDismissDraft = {},
                onFinishDraft = {},
                onSetDefaultUnit = {},
                onUpsertDraft = {},
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RepeatPreview() {
    AppTheme {
        Surface {
            EntryDetail(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialDraft = with(defaultFakeEntries[0]) {
                    EntryDraft(
                        amount = amount,
                        amountUnit = amountUnit,
                        content = content,
                        createdAt = createdAt,
                        entryUid = -1,
                        uid = 0,
                    )
                },
                onDismissDraft = {},
                onFinishDraft = {},
                onSetDefaultUnit = {},
                onUpsertDraft = {},
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpdatePreview() {
    AppTheme {
        Surface {
            EntryDetail(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialDraft = with(defaultFakeEntries[0]) {
                    EntryDraft(
                        amount = amount,
                        amountUnit = amountUnit,
                        content = content,
                        createdAt = createdAt,
                        entryUid = uid,
                        uid = 0,
                    )
                },
                onDismissDraft = {},
                onFinishDraft = {},
                onSetDefaultUnit = {},
                onUpsertDraft = {},
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingPreview() {
    AppTheme {
        Surface {
            EntryDetail(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialDraft = with(defaultFakeEntries[0]) {
                    EntryDraft(
                        amount = amount,
                        amountUnit = amountUnit,
                        content = content,
                        createdAt = createdAt,
                        entryUid = uid,
                        uid = 0,
                    )
                },
                onDismissDraft = {},
                onFinishDraft = {},
                onSetDefaultUnit = {},
                onUpsertDraft = {},
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_5)
@Composable
private fun SmallPreview() {
    AppTheme {
        Surface {
            EntryDetail(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialDraft = with(defaultFakeEntries[0]) {
                    EntryDraft(
                        amount = amount,
                        amountUnit = amountUnit,
                        content = content,
                        createdAt = createdAt,
                        entryUid = uid,
                        uid = 0,
                    )
                },
                onDismissDraft = {},
                onFinishDraft = {},
                onSetDefaultUnit = {},
                onUpsertDraft = {},
            )
        }
    }
}
