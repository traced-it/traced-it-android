package app.traced_it.ui.entry

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.traced_it.R
import app.traced_it.data.di.FakeEntryRepository
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.data.local.database.Entry
import app.traced_it.ui.components.*
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
    EntryListScreen(
        filteredEntriesFlow = viewModel.filteredEntries,
        filterExpandedFlow = viewModel.filterExpanded,
        filterQueryFlow = viewModel.filterQuery,
        onNavigateToAboutScreen = onNavigateToAboutScreen,
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    filteredEntriesFlow: StateFlow<PagingData<Entry>>,
    onNavigateToAboutScreen: () -> Unit = {},
    filterExpandedFlow: StateFlow<Boolean>,
    filterQueryFlow: StateFlow<String>,
    initialSelectedEntry: Entry? = null,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalResources.current

    val allEntriesCount by viewModel.allEntriesCount.collectAsStateWithLifecycle()
    val filteredEntries = filteredEntriesFlow.collectAsLazyPagingItems()
    val (deleteAllEntriesDialogOpen, setDeleteAllEntriesDialogOpen) = remember { mutableStateOf(false) }
    var entryDetailAction by remember {
        mutableStateOf<EntryDetailAction>(EntryDetailAction.Prefill(Entry()))
    }
    var entryDetailOpen by remember { mutableStateOf(false) }
    val filterExpanded by filterExpandedFlow.collectAsStateWithLifecycle()
    val filterFocusRequester = remember { FocusRequester() }
    val filterQuery by filterQueryFlow.collectAsStateWithLifecycle()
    val highlightedEntryUid by viewModel.highlightedEntryUid.collectAsStateWithLifecycle()
    val latestEntry by viewModel.latestEntry.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val (selectedEntry, setSelectedEntry) = remember { mutableStateOf(initialSelectedEntry) }
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.takeIf { result.resultCode == Activity.RESULT_OK }?.let { uri ->
                coroutineScope.launch {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.reader().use { reader ->
                            viewModel.importEntriesCsv(resources, reader)
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
                            viewModel.exportAllEntriesCsv(resources, writer)
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
                            viewModel.exportFilteredEntriesCsv(resources, writer)
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
        message?.let { message ->
            val result = snackbarHostState.showSnackbar(MessageSnackbarVisuals(message))
            when (result) {
                SnackbarResult.ActionPerformed -> viewModel.performMessageAction()
                SnackbarResult.Dismissed -> viewModel.dismissMessage()
            }
        }
    }

    // Scroll to the inserted, updated or restored item if it's above the
    // currently visible lazy column items.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map {
                filteredEntries.itemSnapshotList.indexOfFirst {
                    it?.uid == highlightedEntryUid
                }
            }
            .filter {
                it != -1 && it <= listState.firstVisibleItemIndex
            }
            .collect {
                listState.animateScrollToItem(it)
            }
    }

    BackHandler(entryDetailOpen || selectedEntry != null || filterExpanded) {
        if (entryDetailOpen) {
            entryDetailOpen = false
        } else if (selectedEntry != null) {
            setSelectedEntry(null)
        } else {
            viewModel.setFilterExpanded(false)
            viewModel.filter("")
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
                                viewModel.filter(it)
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
                            selectedEntry.let {
                                entryDetailAction = EntryDetailAction.Edit(it)
                                entryDetailOpen = true
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Edit,
                                stringResource(R.string.list_item_update),
                            )
                        }
                        IconButton({
                            selectedEntry.let {
                                viewModel.deleteEntry(resources, it)
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Delete,
                                stringResource(R.string.list_item_delete),
                            )
                        }
                        SelectedEntryMenu(
                            modifier = Modifier.padding(end = Spacing.windowPadding - 8.dp),
                            onAddWithSameContent = {
                                selectedEntry.let {
                                    entryDetailAction = EntryDetailAction.Prefill(it)
                                    entryDetailOpen = true
                                }
                            },
                            onCopy = {
                                selectedEntry.let { selectedEntry ->
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
                                }
                            },
                            onFilterWithSimilarContent = {
                                selectedEntry.let {
                                    viewModel.setFilterExpanded(true)
                                    viewModel.filter(it.content)
                                    coroutineScope.launch {
                                        listState.scrollToItem(0)
                                    }
                                    setSelectedEntry(null)
                                }
                            },
                        )
                    } else if (filterExpanded) {
                        IconButton({
                            if (filterQuery.isEmpty()) {
                                viewModel.setFilterExpanded(false)
                            } else {
                                viewModel.filter("")
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
                            { viewModel.setFilterExpanded(true) },
                            modifier = Modifier.testTag("entryListFilterExpandButton"),
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
                                viewModel.setFilterExpanded(false)
                                viewModel.filter("")
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
                                viewModel.filterQuerySanitizedForFilename,
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
                            modifier = Modifier.animateItem(),
                            prevEntry = index.takeIf { index != 0 }?.let {
                                filteredEntries[index - 1]
                            },
                            now = now,
                            highlighted = highlightedEntryUid == entry.uid,
                            odd = index % 2 != 0,
                            selected = selectedEntry == entry,
                            onAddWithSameText = {
                                entryDetailAction = EntryDetailAction.Prefill(entry)
                                entryDetailOpen = true
                            },
                            onDelete = {
                                viewModel.deleteEntry(resources, entry)
                            },
                            onHighlightingFinished = {
                                viewModel.setHighlightedEntryUid(null)
                            },
                            onToggle = {
                                setSelectedEntry(if (entry == selectedEntry) null else entry)
                            },
                            onUpdate = {
                                entryDetailAction = EntryDetailAction.Edit(entry)
                                entryDetailOpen = true
                            },
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
            latestEntryUnit = latestEntry?.amountUnit,
            onInsert = {
                coroutineScope.launch {
                    listState.scrollToItem(0)
                    setSelectedEntry(null)
                    entryDetailOpen = false
                    viewModel.insertEntry(resources, it)
                }
            },
            onUpdate = {
                setSelectedEntry(null)
                entryDetailOpen = false
                viewModel.updateEntry(resources, it)
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
                viewModel.deleteAllEntries(resources)
            },
        )
    }
}

// Previews

@SuppressLint("ViewModelConstructorInComposable")
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        EntryListScreen(
            filteredEntriesFlow = MutableStateFlow(
                PagingData.from(defaultFakeEntries)
            ),
            filterExpandedFlow = MutableStateFlow(false),
            filterQueryFlow = MutableStateFlow(""),
            viewModel = EntryViewModel(
                FakeEntryRepository(),
                SavedStateHandle(),
            ),
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        EntryListScreen(
            filteredEntriesFlow = MutableStateFlow(
                PagingData.from(defaultFakeEntries)
            ),
            filterExpandedFlow = MutableStateFlow(false),
            filterQueryFlow = MutableStateFlow(""),
            viewModel = EntryViewModel(
                FakeEntryRepository(),
                SavedStateHandle(),
            ),
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterPreview() {
    AppTheme {
        EntryListScreen(
            filteredEntriesFlow = MutableStateFlow(
                PagingData.from(defaultFakeEntries)
            ),
            filterExpandedFlow = MutableStateFlow(true),
            filterQueryFlow = MutableStateFlow("ee"),
            viewModel = EntryViewModel(
                FakeEntryRepository(),
                SavedStateHandle(),
            ),
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectedEntryPreview() {
    AppTheme {
        EntryListScreen(
            filteredEntriesFlow = MutableStateFlow(
                PagingData.from(defaultFakeEntries)
            ),
            initialSelectedEntry = defaultFakeEntries[2],
            filterExpandedFlow = MutableStateFlow(false),
            filterQueryFlow = MutableStateFlow(""),
            viewModel = EntryViewModel(
                FakeEntryRepository(),
                SavedStateHandle(),
            ),
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() {
    AppTheme {
        EntryListScreen(
            filteredEntriesFlow = MutableStateFlow(PagingData.empty()),
            filterExpandedFlow = MutableStateFlow(false),
            filterQueryFlow = MutableStateFlow(""),
            viewModel = EntryViewModel(
                FakeEntryRepository(emptyList()),
                SavedStateHandle(),
            ),
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@RequiresApi(Build.VERSION_CODES.O)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 480
)
@Composable
private fun PortraitPreview() {
    AppTheme {
        EntryListScreen(
            filteredEntriesFlow = MutableStateFlow(
                PagingData.from(defaultFakeEntries)
            ),
            filterExpandedFlow = MutableStateFlow(false),
            filterQueryFlow = MutableStateFlow(""),
            viewModel = EntryViewModel(
                FakeEntryRepository(),
                SavedStateHandle(),
            ),
        )
    }
}
