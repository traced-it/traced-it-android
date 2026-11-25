package app.traced_it.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import app.traced_it.R
import app.traced_it.data.database.Entry
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.ui.Message
import app.traced_it.ui.ScrollTarget
import app.traced_it.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun EntryList(
    entries: Flow<PagingData<Entry>>,
    highlightedUid: StateFlow<Int?>,
    message: StateFlow<Message?>,
    loading: Boolean,
    query: StateFlow<String?>,
    readOnly: Boolean,
    scrollTarget: StateFlow<ScrollTarget?>,
    totalCount: StateFlow<Int>,
    animationsEnabled: Boolean = true,
    initialSelectedUid: Int? = null,
    onCopyToClipboard: (uid: Int) -> Unit,
    onDelete: (uid: Int) -> Unit,
    onDeleteAll: () -> Unit,
    onDismissMessage: () -> Unit,
    onFilter: (query: String?) -> Unit,
    onFilterSimilar: (uid: Int) -> Unit,
    onFinishHighlight: () -> Unit,
    onFinishScrollToTarget: () -> Unit,
    onInsert: () -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onPerformMessageAction: () -> Unit,
    onRepeat: (uid: Int) -> Unit,
    onRequestExportAll: () -> Unit,
    onRequestExportFiltered: () -> Unit,
    onRequestImport: () -> Unit,
    onUpdate: (uid: Int) -> Unit,
) {
    val selectedUid = rememberSaveable { mutableStateOf(initialSelectedUid) }

    StyledScaffoldWithMessage(
        message = message,
        onDismissMessage = onDismissMessage,
        onPerformMessageAction = onPerformMessageAction,
        topBar = {
            EntryListTopBar(
                query = query,
                selectedUid = selectedUid,
                totalCount = totalCount,
                loading = loading,
                readOnly = readOnly,
                onCopyToClipboard = onCopyToClipboard,
                onDelete = onDelete,
                onDeleteAll = onDeleteAll,
                onFilter = onFilter,
                onFilterSimilar = onFilterSimilar,
                onNavigateToAboutScreen = onNavigateToAboutScreen,
                onRepeat = onRepeat,
                onRequestExportAll = onRequestExportAll,
                onRequestImport = onRequestImport,
                onSelect = { uid -> selectedUid.value = uid },
                onUpdate = onUpdate,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            EntryListFilterHeader(
                entries = entries,
                query = query,
                totalCount = totalCount,
                onRequestExportFiltered = onRequestExportFiltered,
            )
            HorizontalDivider()
            EntryListColumn(
                entries = entries,
                highlightedUid = highlightedUid,
                readOnly = readOnly,
                scrollTarget = scrollTarget,
                selectedUid = selectedUid,
                modifier = Modifier.weight(1f),
                animationsEnabled = animationsEnabled,
                onDelete = onDelete,
                onFinishHighlight = onFinishHighlight,
                onRepeat = onRepeat,
                onFinishScrollToTarget = onFinishScrollToTarget,
                onSelect = { entry -> selectedUid.value = entry },
                onUpdate = onUpdate,
            )
            if (!readOnly) {
                HorizontalDivider()
                LargeButton(
                    stringResource(R.string.list_add),
                    onClick = onInsert,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag(TestTags.ENTRY_LIST_INSERT),
                )
            }
        }
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        EntryList(
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            message = MutableStateFlow(null),
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishHighlight = {},
            onFinishScrollToTarget = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        EntryList(
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            message = MutableStateFlow(null),
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishHighlight = {},
            onFinishScrollToTarget = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReadOnlyPreview() {
    AppTheme {
        EntryList(
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            message = MutableStateFlow(null),
            loading = false,
            query = MutableStateFlow(null),
            readOnly = true,
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishHighlight = {},
            onFinishScrollToTarget = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterPreview() {
    AppTheme {
        EntryList(
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries.filter { it.content.contains("ee") })),
            highlightedUid = MutableStateFlow(null),
            message = MutableStateFlow(null),
            loading = false,
            query = MutableStateFlow("ee"),
            readOnly = false,
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishHighlight = {},
            onFinishScrollToTarget = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectedEntryPreview() {
    AppTheme {
        EntryList(
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            message = MutableStateFlow(null),
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            initialSelectedUid = defaultFakeEntries[2].uid,
            animationsEnabled = false,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishHighlight = {},
            onFinishScrollToTarget = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() {
    AppTheme {
        EntryList(
            entries = MutableStateFlow(PagingData.empty()),
            highlightedUid = MutableStateFlow(null),
            message = MutableStateFlow(null),
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(0),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishHighlight = {},
            onFinishScrollToTarget = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingPreview() {
    AppTheme {
        EntryList(
            entries = MutableStateFlow(PagingData.empty()),
            highlightedUid = MutableStateFlow(null),
            message = MutableStateFlow(null),
            loading = true,
            query = MutableStateFlow(null),
            readOnly = true,
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(0),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishHighlight = {},
            onFinishScrollToTarget = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onUpdate = {},
        )
    }
}
