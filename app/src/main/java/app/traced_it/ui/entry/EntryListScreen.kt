package app.traced_it.ui.entry

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.traced_it.R
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.data.local.database.Entry
import app.traced_it.data.local.database.EntryUnit
import app.traced_it.ui.components.*
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private class MessageSnackbarVisuals(tracedMessage: Message) : SnackbarVisuals {
    override val message = tracedMessage.text
    override val actionLabel = tracedMessage.actionLabel
    override val withDismissAction = tracedMessage.withDismissAction
    override val duration = when (tracedMessage.duration) {
        Message.Duration.SHORT -> SnackbarDuration.Short
        Message.Duration.LONG -> SnackbarDuration.Long
    }
    val isError = tracedMessage.type == Message.Type.ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    onNavigateToAboutScreen: () -> Unit = {},
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val resources = LocalResources.current

    val allEntriesCount by viewModel.allEntriesCount.collectAsStateWithLifecycle()
    val filterExpanded by viewModel.filterExpanded.collectAsStateWithLifecycle()
    val filterQuery by viewModel.filterQuery.collectAsStateWithLifecycle()
    val filteredEntries = viewModel.filteredEntries.collectAsLazyPagingItems()
    val highlightedEntryUidFlow = viewModel.highlightedEntryUid
    val latestEntryUnit by viewModel.latestEntryUnit.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    EntryListScreen(
        allEntriesCount = allEntriesCount,
        filterExpanded = filterExpanded,
        filterQuery = filterQuery,
        filterQuerySanitizedForFilename = viewModel.filterQuerySanitizedForFilename,
        filteredEntries = filteredEntries,
        highlightedEntryUidFlow = highlightedEntryUidFlow,
        latestEntryUnit = latestEntryUnit,
        message = message,
        onDeleteAllEntries = { viewModel.deleteAllEntries(resources) },
        onDeleteEntry = { entry -> viewModel.deleteEntry(resources, entry) },
        onDismissMessage = { viewModel.dismissMessage() },
        onExportAllEntriesCsv = { writer -> viewModel.exportAllEntriesCsv(resources, writer) },
        onExportFilteredEntriesCsv = { writer -> viewModel.exportFilteredEntriesCsv(resources, writer) },
        onFilter = { filterQuery -> viewModel.filter(filterQuery) },
        onImportEntriesCsv = { reader -> viewModel.importEntriesCsv(resources, reader) },
        onInsertEntry = { entry -> viewModel.insertEntry(resources, entry) },
        onNavigateToAboutScreen = onNavigateToAboutScreen,
        onPerformMessageAction = { viewModel.performMessageAction() },
        onSetFilterExpanded = { filterExpanded -> viewModel.setFilterExpanded(filterExpanded) },
        onUnsetHighlightedEntry = { viewModel.setHighlightedEntryUid(null) },
        onUpdateEntry = { entry -> viewModel.updateEntry(resources, entry) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryListScreen(
    allEntriesCount: Int,
    animationsEnabled: Boolean = true,
    filterExpanded: Boolean,
    filterQuery: String,
    filterQuerySanitizedForFilename: String,
    filteredEntries: LazyPagingItems<Entry>,
    highlightedEntryUidFlow: StateFlow<Int?>,
    initialSelectedEntry: Entry? = null,
    latestEntryUnit: EntryUnit?,
    message: Message?,
    onDeleteAllEntries: () -> Unit,
    onDeleteEntry: (entry: Entry) -> Unit,
    onDismissMessage: () -> Unit,
    onExportAllEntriesCsv: suspend (writer: OutputStreamWriter) -> Unit,
    onExportFilteredEntriesCsv: suspend (writer: OutputStreamWriter) -> Unit,
    onFilter: (filterQuery: String) -> Unit,
    onImportEntriesCsv: suspend (reader: InputStreamReader) -> Unit,
    onInsertEntry: (entry: Entry) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onPerformMessageAction: () -> Unit,
    onSetFilterExpanded: (filterExpanded: Boolean) -> Unit,
    onUnsetHighlightedEntry: () -> Unit,
    onUpdateEntry: (entry: Entry) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current

    val (deleteAllEntriesDialogOpen, setDeleteAllEntriesDialogOpen) = remember { mutableStateOf(false) }
    var entryDetailAction by remember { mutableStateOf<EntryDetailAction>(EntryDetailAction.Prefill(Entry())) }
    var entryDetailOpen by remember { mutableStateOf(false) }
    val filterFocusRequester = remember { FocusRequester() }
    var focusedEntry by remember { mutableStateOf<Entry?>(null) }
    val highlightedEntryUid by highlightedEntryUidFlow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val (selectedEntry, setSelectedEntry) = remember { mutableStateOf(initialSelectedEntry) }
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.takeIf { result.resultCode == Activity.RESULT_OK }?.let { uri ->
                coroutineScope.launch {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.reader().use { reader ->
                            onImportEntriesCsv(reader)
                        }
                    }
                }
            }
        }
    val exportAllLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.takeIf { result.resultCode == Activity.RESULT_OK }?.let { uri ->
                coroutineScope.launch {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.writer().use { writer ->
                            onExportAllEntriesCsv(writer)
                        }
                    }
                }
            }
        }
    val exportFilteredLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.takeIf { result.resultCode == Activity.RESULT_OK }?.let { uri ->
                coroutineScope.launch {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.writer().use { writer ->
                            onExportFilteredEntriesCsv(writer)
                        }
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            val result = snackbarHostState.showSnackbar(MessageSnackbarVisuals(message))
            when (result) {
                SnackbarResult.ActionPerformed -> onPerformMessageAction()
                SnackbarResult.Dismissed -> onDismissMessage()
            }
        }
    }

    // Scroll to the inserted, updated or restored item if it's above the currently visible lazy column items
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .filter { firstVisibleItemIndex -> firstVisibleItemIndex > 0 }
            .combine(highlightedEntryUidFlow) { firstVisibleItemIndex, highlightedEntryUid ->
                // To start scrolling only after the list has finished refreshing and the highlighted entry has been
                // set, we combine firstVisibleItemIndex (emitted when finished refreshing) and highlightedEntryUidFlow
                // (emitted when highlighted entry set).
                if (highlightedEntryUid != null) {
                    val highlightedItemIndex = filteredEntries.itemSnapshotList
                        .indexOfFirst { entry -> entry?.uid == highlightedEntryUid }
                    if (highlightedItemIndex in 0..firstVisibleItemIndex) {
                        highlightedItemIndex
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            .filterNotNull()
            .collect { highlightedItemIndex ->
                if (!listState.isScrollInProgress) {
                    listState.animateScrollToItem(highlightedItemIndex)
                }
            }
    }

    BackHandler(entryDetailOpen || selectedEntry != null || filterExpanded) {
        if (entryDetailOpen) {
            entryDetailOpen = false
        } else if (selectedEntry != null) {
            setSelectedEntry(null)
        } else {
            onSetFilterExpanded(false)
            onFilter("")
            coroutineScope.launch {
                listState.scrollToItem(0)
            }
        }
    }

    TracedScaffold(
        topBar = {
            TracedTopAppBar(
                title = {
                    if (selectedEntry != null) {
                        Text(stringResource(R.string.list_selected_title))
                    } else if (filterExpanded) {
                        TracedTextField(
                            value = filterQuery,
                            onValueChange = {
                                onFilter(it)
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                }
                            },
                            modifier = Modifier
                                .focusRequester(filterFocusRequester)
                                .testTag("entryListFilterQueryTextField"),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            placeholder = {
                                Text(stringResource(R.string.list_filter_input_placeholder))
                            },
                            singleLine = true,
                            contentPadding = PaddingValues(0.dp),
                            showTrailingIcon = false,
                            textColor = MaterialTheme.colorScheme.onBackground,
                            placeholderColor = MaterialTheme.colorScheme.onBackground,
                            containerColor = Color.Transparent,
                            indicatorColor = Color.Transparent
                        )
                        LaunchedEffect(null) {
                            filterFocusRequester.requestFocus()
                        }
                    } else {
                        Text(stringResource(R.string.list_title, filteredEntries.itemCount))
                    }
                },
                actions = {
                    if (selectedEntry != null) {
                        IconButton({
                            entryDetailAction = EntryDetailAction.Edit(selectedEntry)
                            entryDetailOpen = true
                        }) {
                            Icon(
                                Icons.Outlined.Edit,
                                stringResource(R.string.list_item_update),
                            )
                        }
                        IconButton({
                            onDeleteEntry(selectedEntry)
                        }) {
                            Icon(
                                Icons.Outlined.Delete,
                                stringResource(R.string.list_item_delete),
                            )
                        }
                        SelectedEntryMenu(
                            modifier = Modifier.padding(end = Spacing.windowPadding - 8.dp),
                            onAddWithSameContent = {
                                entryDetailAction = EntryDetailAction.Prefill(selectedEntry)
                                entryDetailOpen = true
                            },
                            onCopy = {
                                coroutineScope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(
                                            ClipData.newPlainText("note", selectedEntry.format(context))
                                        )
                                    )
                                    val systemHasClipboardEditor =
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                    if (!systemHasClipboardEditor) {
                                        Toast.makeText(
                                            context,
                                            R.string.list_item_copied_to_clipboard,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                            onFilterWithSimilarContent = {
                                onSetFilterExpanded(true)
                                onFilter(selectedEntry.content)
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                }
                                setSelectedEntry(null)
                            },
                        )
                    } else if (filterExpanded) {
                        IconButton({
                            if (filterQuery.isEmpty()) {
                                onSetFilterExpanded(false)
                            } else {
                                onFilter("")
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                }
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Clear,
                                contentDescription = stringResource(
                                    R.string.list_filter_input_clear_content_description
                                ),
                            )
                        }
                    } else {
                        IconButton(
                            { onSetFilterExpanded(true) },
                            Modifier.testTag("entryListFilterExpandButton"),
                            enabled = filteredEntries.itemCount > 0,
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.list_filter_submit),
                            )
                        }
                        EntryListMenu(
                            enabled = filteredEntries.itemCount > 0,
                            modifier = Modifier.padding(end = Spacing.windowPadding - 8.dp),
                            onDeleteAllEntries = {
                                setDeleteAllEntriesDialogOpen(true)
                            },
                            onExportAllEntries = {
                                val filename = resources.getString(
                                    R.string.list_export_all_filename,
                                    resources.getString(R.string.app_name),
                                    Build.MODEL,
                                )
                                exportAllLauncher.launch(
                                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_TITLE, filename)
                                    }
                                )
                            },
                            onImportEntries = {
                                importLauncher.launch(
                                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "text/*"
                                    }
                                )
                            },
                            onNavigateToAboutScreen = onNavigateToAboutScreen,
                        )
                    }
                },
                navigationIcon = {
                    if (selectedEntry != null) {
                        IconButton(
                            onClick = {
                                setSelectedEntry(null)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(R.string.list_selected_back),
                            )
                        }
                    } else if (filterExpanded) {
                        IconButton(
                            onClick = {
                                onSetFilterExpanded(false)
                                onFilter("")
                                coroutineScope.launch {
                                    listState.scrollToItem(0)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(
                                    R.string.list_filter_close_content_description
                                ),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = Spacing.bottomButtonHeight + Spacing.small + Spacing.small),
            ) { snackbarData ->
                val isError = (snackbarData.visuals as? MessageSnackbarVisuals)?.isError ?: false
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.inverseSurface
                    },
                    contentColor = if (isError) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                    dismissActionContentColor = if (isError) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                    actionColor = if (isError) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            if (filterExpanded && filterQuery.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.list_filter_status,
                            filteredEntries.itemCount,
                            filteredEntries.itemCount,
                            allEntriesCount,
                        ),
                        Modifier.padding(horizontal = Spacing.windowPadding),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    TextButton(
                        onClick = {
                            val filename = resources.getString(
                                R.string.list_export_filtered_filename,
                                resources.getString(R.string.app_name),
                                Build.MODEL,
                                filterQuerySanitizedForFilename,
                            )
                            exportFilteredLauncher.launch(
                                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_TITLE, filename)
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = filteredEntries.itemCount > 0,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(
                            stringResource(R.string.list_filter_export),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            HorizontalDivider()
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
            ) {
                items(
                    filteredEntries.itemCount,
                    key = filteredEntries.itemKey { it.uid },
                ) { index ->
                    filteredEntries[index]?.let { entry ->
                        EntryListItem(
                            entry = entry,
                            prevEntry = index.takeIf { index != 0 }?.let {
                                filteredEntries[index - 1]
                            },
                            now = now,
                            highlighted = highlightedEntryUid == entry.uid,
                            odd = index % 2 != 0,
                            selected = selectedEntry == entry,
                            focused = focusedEntry == entry,
                            animationsEnabled = animationsEnabled,
                            onAddWithSameText = {
                                entryDetailAction = EntryDetailAction.Prefill(entry)
                                entryDetailOpen = true
                            },
                            onDelete = {
                                onDeleteEntry(entry)
                            },
                            onToggle = {
                                setSelectedEntry(if (entry == selectedEntry) null else entry)
                            },
                            onFocus = {
                                focusedEntry = entry
                            },
                            onUpdate = {
                                entryDetailAction = EntryDetailAction.Edit(entry)
                                entryDetailOpen = true
                            },
                            onUnsetHighlightedEntry = onUnsetHighlightedEntry,
                        )
                    } ?: Row {}
                }
            }
            HorizontalDivider()
            TracedBottomButton(
                stringResource(R.string.list_add),
                onClick = {
                    entryDetailAction = EntryDetailAction.Prefill(Entry())
                    entryDetailOpen = true
                },
                modifier = Modifier.testTag("entryListNewEntryButton"),
            )
        }
    }

    AnimatedVisibility(
        entryDetailOpen,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        EntryDetailDialog(
            entryDetailAction,
            latestEntryUnit = latestEntryUnit,
            onInsert = {
                entryDetailOpen = false
                setSelectedEntry(null)
                onInsertEntry(it)
                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            },
            onUpdate = {
                entryDetailOpen = false
                setSelectedEntry(null)
                onUpdateEntry(it)
            },
            onDismiss = {
                entryDetailOpen = false
            },
        )
    }

    if (deleteAllEntriesDialogOpen) {
        ConfirmationDialog(
            title = stringResource(R.string.list_delete_all_dialog_title),
            text = stringResource(R.string.list_delete_all_dialog_text),
            confirmText = stringResource(R.string.list_delete_all_dialog_confirm),
            dismissText = stringResource(R.string.list_delete_all_dialog_dismiss),
            onDismissRequest = { setDeleteAllEntriesDialogOpen(false) },
            onConfirmation = {
                setDeleteAllEntriesDialogOpen(false)
                onDeleteAllEntries()
            },
        )
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesCount = defaultFakeEntries.size,
            animationsEnabled = false,
            filterExpanded = false,
            filterQuery = "",
            filterQuerySanitizedForFilename = "",
            filteredEntries = flowOf(PagingData.from(defaultFakeEntries)).collectAsLazyPagingItems(),
            highlightedEntryUidFlow = MutableStateFlow(null),
            latestEntryUnit = defaultFakeEntries.first().amountUnit,
            message = null,
            onDeleteAllEntries = {},
            onDeleteEntry = {},
            onDismissMessage = {},
            onExportAllEntriesCsv = {},
            onExportFilteredEntriesCsv = {},
            onFilter = {},
            onImportEntriesCsv = {},
            onInsertEntry = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onSetFilterExpanded = {},
            onUnsetHighlightedEntry = {},
            onUpdateEntry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesCount = defaultFakeEntries.size,
            animationsEnabled = false,
            filterExpanded = false,
            filterQuery = "",
            filterQuerySanitizedForFilename = "",
            filteredEntries = flowOf(PagingData.from(defaultFakeEntries)).collectAsLazyPagingItems(),
            highlightedEntryUidFlow = MutableStateFlow(null),
            latestEntryUnit = defaultFakeEntries.first().amountUnit,
            message = null,
            onDeleteAllEntries = {},
            onDeleteEntry = {},
            onDismissMessage = {},
            onExportAllEntriesCsv = {},
            onExportFilteredEntriesCsv = {},
            onFilter = {},
            onImportEntriesCsv = {},
            onInsertEntry = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onSetFilterExpanded = {},
            onUnsetHighlightedEntry = {},
            onUpdateEntry = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesCount = defaultFakeEntries.size,
            animationsEnabled = false,
            filterExpanded = true,
            filterQuery = "ee",
            filterQuerySanitizedForFilename = "",
            filteredEntries = flowOf(PagingData.from(defaultFakeEntries)).collectAsLazyPagingItems(),
            highlightedEntryUidFlow = MutableStateFlow(null),
            latestEntryUnit = defaultFakeEntries.first().amountUnit,
            message = null,
            onDeleteAllEntries = {},
            onDeleteEntry = {},
            onDismissMessage = {},
            onExportAllEntriesCsv = {},
            onExportFilteredEntriesCsv = {},
            onFilter = {},
            onImportEntriesCsv = {},
            onInsertEntry = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onSetFilterExpanded = {},
            onUnsetHighlightedEntry = {},
            onUpdateEntry = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectedEntryPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesCount = defaultFakeEntries.size,
            animationsEnabled = false,
            filterExpanded = false,
            filterQuery = "",
            filterQuerySanitizedForFilename = "",
            filteredEntries = flowOf(PagingData.from(defaultFakeEntries)).collectAsLazyPagingItems(),
            highlightedEntryUidFlow = MutableStateFlow(null),
            initialSelectedEntry = defaultFakeEntries[2],
            latestEntryUnit = defaultFakeEntries.first().amountUnit,
            message = null,
            onDeleteAllEntries = {},
            onDeleteEntry = {},
            onDismissMessage = {},
            onExportAllEntriesCsv = {},
            onExportFilteredEntriesCsv = {},
            onFilter = {},
            onImportEntriesCsv = {},
            onInsertEntry = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onSetFilterExpanded = {},
            onUnsetHighlightedEntry = {},
            onUpdateEntry = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesCount = 0,
            animationsEnabled = false,
            filterExpanded = false,
            filterQuery = "",
            filterQuerySanitizedForFilename = "",
            filteredEntries = flowOf(PagingData.empty<Entry>()).collectAsLazyPagingItems(),
            highlightedEntryUidFlow = MutableStateFlow(null),
            latestEntryUnit = defaultFakeEntries.first().amountUnit,
            message = null,
            onDeleteAllEntries = {},
            onDeleteEntry = {},
            onDismissMessage = {},
            onExportAllEntriesCsv = {},
            onExportFilteredEntriesCsv = {},
            onFilter = {},
            onImportEntriesCsv = {},
            onInsertEntry = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onSetFilterExpanded = {},
            onUnsetHighlightedEntry = {},
            onUpdateEntry = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.TABLET)
@Composable
private fun TabletPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesCount = defaultFakeEntries.size,
            animationsEnabled = false,
            filterExpanded = false,
            filterQuery = "",
            filterQuerySanitizedForFilename = "",
            filteredEntries = flowOf(PagingData.from(defaultFakeEntries)).collectAsLazyPagingItems(),
            highlightedEntryUidFlow = MutableStateFlow(null),
            latestEntryUnit = defaultFakeEntries.first().amountUnit,
            message = null,
            onDeleteAllEntries = {},
            onDeleteEntry = {},
            onDismissMessage = {},
            onExportAllEntriesCsv = {},
            onExportFilteredEntriesCsv = {},
            onFilter = {},
            onImportEntriesCsv = {},
            onInsertEntry = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onSetFilterExpanded = {},
            onUnsetHighlightedEntry = {},
            onUpdateEntry = {},
        )
    }
}
