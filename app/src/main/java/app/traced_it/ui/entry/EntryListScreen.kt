package app.traced_it.ui.entry

import android.content.res.Configuration
import android.os.Build
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import app.traced_it.R
import app.traced_it.data.di.FakeEntryRepository
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.data.local.database.Entry
import app.traced_it.ui.components.ConfirmationDialog
import app.traced_it.ui.components.TracedBottomButton
import app.traced_it.ui.components.TracedScaffold
import app.traced_it.ui.components.TracedTopAppBar
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun EntryListScreen(
    onNavigateToAboutScreen: () -> Unit = {},
    viewModel: EntryViewModel = hiltViewModel(),
) {
    EntryListScreen(
        allEntriesFlow = viewModel.allEntries,
        onNavigateToAboutScreen = onNavigateToAboutScreen,
        viewModel = viewModel,
    )
}

@Composable
fun EntryListScreen(
    allEntriesFlow: StateFlow<PagingData<Entry>>,
    initialSelectedEntry: Entry? = null,
    onNavigateToAboutScreen: () -> Unit = {},
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val appName = stringResource(R.string.app_name)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allEntries = allEntriesFlow.collectAsLazyPagingItems()
    var deleteAllEntriesDialogOpen by remember { mutableStateOf(false) }
    var entryDetailAction by remember {
        mutableStateOf<EntryDetailAction>(EntryDetailAction.Prefill(Entry()))
    }
    var entryDetailOpen by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<Entry?>(null) }
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

    BackHandler(entryDetailOpen || selectedEntry != null) {
        if (entryDetailOpen) {
            entryDetailOpen = false
        } else {
            selectedEntry = null
        }
    }

    TracedScaffold(
        topBar = {
            TracedTopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.list_title,
                            allEntries.itemCount,
                        )
                    )
                },
                actions = {
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
                                    Build.MODEL
                                )
                            )
                        },
                        onImportEntries = {
                            viewModel.launchImportEntries(importLauncher)
                        },
                        onNavigateToAboutScreen = onNavigateToAboutScreen,
                    )
                },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarSuccessHostState,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = Spacing.bottomButtonHeight),
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
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = Spacing.bottomButtonHeight),
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
                modifier = Modifier
                    .testTag("entryListNewEntryButton")
                    .windowInsetsPadding(WindowInsets.navigationBars),
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
            viewModel = EntryViewModel(FakeEntryRepository()),
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
            viewModel = EntryViewModel(FakeEntryRepository()),
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
            viewModel = EntryViewModel(FakeEntryRepository()),
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
            viewModel = EntryViewModel(FakeEntryRepository(emptyList())),
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
            viewModel = EntryViewModel(FakeEntryRepository()),
        )
    }
}
