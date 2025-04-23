package app.traced_it.ui.entry

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    onNavigateToAboutScreen: () -> Unit = {},
    viewModel: EntryViewModel = hiltViewModel(),
) {
    EntryListScreen(
        allEntriesFlow = viewModel.allEntries,
        filterExpandedFlow = viewModel.filterExpanded,
        filterQueryFlow = viewModel.filterQuery,
        onNavigateToAboutScreen = onNavigateToAboutScreen,
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    allEntriesFlow: StateFlow<PagingData<Entry>>,
    onNavigateToAboutScreen: () -> Unit = {},
    filterExpandedFlow: StateFlow<Boolean>,
    filterQueryFlow: StateFlow<String>,
    initialSelectedEntry: Entry? = null,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val appName = stringResource(R.string.app_name)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val allEntries = allEntriesFlow.collectAsLazyPagingItems()
    var deleteAllEntriesDialogOpen by remember { mutableStateOf(false) }
    var entryDetailAction by remember {
        mutableStateOf<EntryDetailAction>(EntryDetailAction.Prefill(Entry()))
    }
    var entryDetailOpen by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<Entry?>(null) }
    val filterExpanded by filterExpandedFlow.collectAsStateWithLifecycle()
    val filterFocusRequester = remember { FocusRequester() }
    val filterQuery by filterQueryFlow.collectAsStateWithLifecycle()
    val highlightedEntryUid by viewModel.highlightedEntryUid.collectAsStateWithLifecycle()
    val latestEntry by viewModel.latestEntry.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedEntry by remember { mutableStateOf(initialSelectedEntry) }
    val snackbarErrorHostState = remember { SnackbarHostState() }
    val snackbarSuccessHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.importEntries(context, it)
    }
    val exportAllLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.exportAllEntries(context, it)
    }
    val exportFoundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.exportFoundEntries(context, it)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            (message as Message).let {
                val snackbarHostState = if (it.type == Message.Type.SUCCESS) {
                    snackbarSuccessHostState
                } else {
                    snackbarErrorHostState
                }
                val result = snackbarHostState.showSnackbar(
                    message = it.text,
                    actionLabel = it.actionLabel,
                    withDismissAction = it.withDismissAction,
                    duration = it.duration,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.performMessageAction()
                }
                if (result == SnackbarResult.Dismissed) {
                    viewModel.dismissMessage()
                }
            }
        }
    }

    // Scroll to the inserted, updated or restored item if it's above the
    // currently visible lazy column items.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map {
                allEntries.itemSnapshotList.indexOfFirst {
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
            selectedEntry = null
        } else {
            viewModel.setFilterExpanded(false)
            viewModel.filter("")
            scope.launch {
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
                                scope.launch {
                                    listState.scrollToItem(0)
                                }
                            },
                            modifier = Modifier
                                .focusRequester(filterFocusRequester)
                                .testTag("entryListFilterQueryTextField"),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            placeholder = {
                                Text(
                                    stringResource(
                                        R.string.list_filter_input_placeholder
                                    )
                                )
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
                        Text(
                            stringResource(
                                R.string.list_title,
                                allEntries.itemCount,
                            )
                        )
                    }
                },
                actions = {
                    if (selectedEntry != null) {
                        IconButton({
                            selectedEntry?.let {
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
                            selectedEntry?.let {
                                entryToDelete = it
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Delete,
                                stringResource(R.string.list_item_delete),
                            )
                        }
                        SelectedEntryMenu(
                            modifier = Modifier.padding(
                                end = Spacing.windowPadding - 8.dp
                            ),
                            onAddWithSameText = {
                                selectedEntry?.let {
                                    entryDetailAction =
                                        EntryDetailAction.Prefill(it)
                                    entryDetailOpen = true
                                }
                            },
                            onCopy = {
                                selectedEntry?.let {
                                    clipboardManager.setText(
                                        AnnotatedString(it.format(context))
                                    )
                                    // Only show a toast for Android 12 and lower.
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                        Toast.makeText(
                                            context,
                                            R.string.list_item_copied_to_clipboard,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                            onFindWithSameText = {
                                selectedEntry?.let {
                                    viewModel.setFilterExpanded(true)
                                    viewModel.filter(it.content)
                                    scope.launch {
                                        listState.scrollToItem(0)
                                    }
                                    selectedEntry = null
                                }
                            },
                        )
                    } else if (filterExpanded) {
                        IconButton({
                            if (filterQuery.isEmpty()) {
                                viewModel.setFilterExpanded(false)
                            } else {
                                viewModel.filter("")
                                scope.launch {
                                    listState.scrollToItem(0)
                                }
                            }
                        }) {
                            Icon(
                                Icons.Outlined.Clear,
                                contentDescription = stringResource(
                                    R.string.list_filter_input_clear_content_description
                                )
                            )
                        }
                    } else {
                        IconButton(
                            { viewModel.setFilterExpanded(true) },
                            modifier = Modifier
                                .testTag("entryListFilterExpandButton"),
                            enabled = allEntries.itemCount > 0,
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(
                                    R.string.list_filter_submit
                                )
                            )
                        }
                        EntryListMenu(
                            enabled = allEntries.itemCount > 0,
                            modifier = Modifier.padding(
                                end = Spacing.windowPadding - 8.dp
                            ),
                            onDeleteAllEntries = {
                                deleteAllEntriesDialogOpen = true
                            },
                            onExportAllEntries = {
                                viewModel.launchExportEntries(
                                    exportAllLauncher,
                                    context.resources.getString(
                                        R.string.list_export_all_filename,
                                        appName,
                                        Build.MODEL,
                                    )
                                )
                            },
                            onImportEntries = {
                                viewModel.launchImportEntries(importLauncher)
                            },
                            onNavigateToAboutScreen = onNavigateToAboutScreen,
                        )
                    }
                },
                navigationIcon = {
                    if (filterExpanded) {
                        IconButton(
                            onClick = {
                                viewModel.setFilterExpanded(false)
                                viewModel.filter("")
                                scope.launch {
                                    listState.scrollToItem(0)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(
                                    R.string.list_filter_close_content_description
                                )
                            )
                        }
                    } else if (selectedEntry != null) {
                        IconButton(
                            onClick = {
                                selectedEntry = null
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = stringResource(
                                    R.string.list_selected_back
                                )
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarSuccessHostState,
                modifier = Modifier
                    .padding(bottom = Spacing.bottomButtonHeight + Spacing.small + Spacing.small),
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        dismissActionContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                },
            )
            SnackbarHost(
                hostState = snackbarErrorHostState,
                modifier = Modifier
                    .padding(bottom = Spacing.bottomButtonHeight + Spacing.small + Spacing.small),
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        dismissActionContentColor = MaterialTheme.colorScheme.onError,
                        actionColor = MaterialTheme.colorScheme.onError,
                    )
                },
            )
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.list_filter_found,
                            allEntries.itemCount,
                            allEntries.itemCount,
                        ),
                        Modifier.padding(horizontal = Spacing.windowPadding),
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextButton(
                        onClick = {
                            viewModel.launchExportEntries(
                                exportFoundLauncher,
                                context.resources.getString(
                                    R.string.list_export_found_filename,
                                    appName,
                                    Build.MODEL,
                                )
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = allEntries.itemCount > 0,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            stringResource(R.string.list_filter_export),
                            style = MaterialTheme.typography.labelSmall
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
                    allEntries.itemCount,
                    key = allEntries.itemKey { it.uid },
                ) { index ->
                    allEntries[index]?.let { entry ->
                        EntryListItem(
                            entry = entry,
                            modifier = Modifier.animateItem(),
                            prevEntry = index.takeIf { index != 0 }?.let {
                                allEntries[index - 1]
                            },
                            now = now,
                            highlighted = highlightedEntryUid == entry.uid,
                            odd = index % 2 != 0,
                            selected = selectedEntry == entry,
                            onAddWithSameText = {
                                entryDetailAction =
                                    EntryDetailAction.Prefill(entry)
                                entryDetailOpen = true
                            },
                            onDelete = {
                                entryToDelete = entry
                            },
                            onHighlightingFinished = {
                                viewModel.setHighlightedEntryUid(null)
                            },
                            onToggle = {
                                selectedEntry = entry.takeIf {
                                    selectedEntry != entry
                                }
                            },
                            onUpdate = {
                                entryDetailAction =
                                    EntryDetailAction.Edit(entry)
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
                scope.launch {
                    listState.scrollToItem(0)
                    selectedEntry = null
                    entryDetailOpen = false
                    viewModel.insertEntry(context, it)
                }
            },
            onUpdate = {
                selectedEntry = null
                entryDetailOpen = false
                viewModel.updateEntry(context, it)
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
            onDismissRequest = { deleteAllEntriesDialogOpen = false },
            onConfirmation = {
                deleteAllEntriesDialogOpen = false
                viewModel.deleteAllEntries(context)
            },
        )
    }

    if (entryToDelete != null) {
        // Copy `entryToDelete`, so that the dialog remains working, even if
        // `entryToDelete` state changes while the dialog is open.
        val entryToDeleteCopy = entryToDelete!!
        ConfirmationDialog(
            title = stringResource(R.string.list_delete_dialog_title),
            text = stringResource(
                R.string.list_delete_dialog_text,
                entryToDeleteCopy.content
            ),
            confirmText = stringResource(R.string.list_delete_dialog_confirm),
            dismissText = stringResource(R.string.list_delete_dialog_dismiss),
            onDismissRequest = { entryToDelete = null },
            onConfirmation = {
                entryToDelete = null
                selectedEntry = null
                viewModel.deleteEntry(context, entryToDeleteCopy)
            },
        )
    }
}

// Previews

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesFlow = MutableStateFlow(
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesFlow = MutableStateFlow(
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesFlow = MutableStateFlow(
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectedEntryPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesFlow = MutableStateFlow(
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() {
    AppTheme {
        EntryListScreen(
            allEntriesFlow = MutableStateFlow(PagingData.empty()),
            filterExpandedFlow = MutableStateFlow(false),
            filterQueryFlow = MutableStateFlow(""),
            viewModel = EntryViewModel(
                FakeEntryRepository(emptyList()),
                SavedStateHandle(),
            ),
        )
    }
}

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
            allEntriesFlow = MutableStateFlow(
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
