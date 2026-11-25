package app.traced_it.ui

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import app.traced_it.R
import app.traced_it.data.database.ActiveEntryUnit
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.Entry
import app.traced_it.data.database.EntryDraft
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.ui.components.ConfirmationDialog
import app.traced_it.ui.components.EntryDetail
import app.traced_it.ui.components.EntryList
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainScreen(
    onNavigateToAboutScreen: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    MainScreen(
        defaultUnit = viewModel.defaultUnit,
        entries = viewModel.entries,
        highlightedUid = viewModel.highlightedUid,
        importExportState = viewModel.importExportState,
        message = viewModel.message,
        navigationState = viewModel.navigationState,
        query = viewModel.query,
        scrollTarget = viewModel.scrollTarget,
        totalCount = viewModel.totalCount,
        onCopyToClipboard = viewModel::copyToClipboard,
        onDelete = viewModel::delete,
        onDeleteAll = viewModel::deleteAll,
        onDismissDraft = viewModel::dismissDraft,
        onDismissMessage = viewModel::dismissMessage,
        onFilter = viewModel::filter,
        onFilterSimilar = viewModel::filterSimilar,
        onFinishDraft = viewModel::finishDraft,
        onFinishHighlight = viewModel::finishHighlight,
        onFinishImportExport = viewModel::finishImportExport,
        onFinishScrollToTarget = viewModel::finishScrollToTarget,
        onInitialize = viewModel::initialize,
        onInsert = viewModel::navigateToInsert,
        onNavigateToAboutScreen = onNavigateToAboutScreen,
        onPerformMessageAction = viewModel::performMessageAction,
        onReceiveImportExportUri = viewModel::receiveImportExportUri,
        onRepeat = viewModel::navigateToRepeat,
        onRequestExportAll = viewModel::requestExportAll,
        onRequestExportFiltered = viewModel::requestExportFiltered,
        onRequestImport = viewModel::requestImport,
        onSetDefaultUnit = viewModel::setDefaultUnit,
        onUpdate = viewModel::navigateToUpdate,
        onUpsertDraft = viewModel::upsertDraft,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun MainScreen(
    defaultUnit: StateFlow<ActiveEntryUnit>,
    entries: Flow<PagingData<Entry>>,
    highlightedUid: StateFlow<Int?>,
    importExportState: StateFlow<ImportExportState?>,
    message: StateFlow<Message?>,
    navigationState: StateFlow<NavigationState>,
    animationsEnabled: Boolean = true,
    initialSelectedUid: Int? = null,
    query: StateFlow<String?>,
    scrollTarget: StateFlow<ScrollTarget?>,
    totalCount: StateFlow<Int>,
    onCopyToClipboard: (uid: Int) -> Unit,
    onDelete: (uid: Int) -> Unit,
    onDeleteAll: () -> Unit,
    onDismissDraft: (draft: EntryDraft) -> Unit,
    onDismissMessage: () -> Unit,
    onFilter: (query: String?) -> Unit,
    onFilterSimilar: (uid: Int) -> Unit,
    onFinishDraft: (draft: EntryDraft) -> Unit,
    onFinishHighlight: () -> Unit,
    onFinishImportExport: () -> Unit,
    onFinishScrollToTarget: () -> Unit,
    onInitialize: () -> Unit,
    onInsert: () -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onPerformMessageAction: () -> Unit,
    onReceiveImportExportUri: (uri: Uri) -> Unit,
    onRepeat: (uid: Int) -> Unit,
    onRequestExportAll: () -> Unit,
    onRequestExportFiltered: () -> Unit,
    onRequestImport: () -> Unit,
    onSetDefaultUnit: (defaultUnit: ActiveEntryUnit) -> Unit,
    onUpdate: (uid: Int) -> Unit,
    onUpsertDraft: (draft: EntryDraft) -> Unit,
) {
    val navigationState by navigationState.collectAsStateWithLifecycle()

    val defaultDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
        .copy(horizontalPartitionSpacerSize = Spacing.small)
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>(
        scaffoldDirective = defaultDirective.copy(horizontalPartitionSpacerSize = Spacing.small),
        initialDestinationHistory = listOf(
            when (navigationState) {
                is NavigationState.Loading,
                is NavigationState.List,
                    -> ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List)

                is NavigationState.Detail -> ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail)
            },
        ),
    )

    LaunchedEffect(navigationState) {
        when (navigationState) {
            is NavigationState.Loading,
            is NavigationState.List,
                -> navigator.navigateTo(ListDetailPaneScaffoldRole.List)

            is NavigationState.Detail -> navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
        }
    }

    if (navigationState is NavigationState.Loading) {
        LaunchedEffect(Unit) {
            onInitialize()
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane(
                Modifier
                    .preferredWidth(0.5f)
                    .imePadding()
            ) {
                var deleteAllDialogOpen by rememberSaveable { mutableStateOf(false) }
                val loading = when (navigationState) {
                    is NavigationState.List, is NavigationState.Detail -> false
                    is NavigationState.Loading -> true
                }
                val readOnly = when (navigationState) {
                    is NavigationState.List -> false
                    is NavigationState.Loading, is NavigationState.Detail -> true
                }

                EntryList(
                    entries = entries,
                    highlightedUid = highlightedUid,
                    message = message,
                    query = query,
                    scrollTarget = scrollTarget,
                    totalCount = totalCount,
                    loading = loading,
                    readOnly = readOnly,
                    animationsEnabled = animationsEnabled,
                    initialSelectedUid = initialSelectedUid,
                    onCopyToClipboard = onCopyToClipboard,
                    onDelete = onDelete,
                    onDeleteAll = { deleteAllDialogOpen = true },
                    onDismissMessage = onDismissMessage,
                    onFilter = onFilter,
                    onFilterSimilar = onFilterSimilar,
                    onFinishHighlight = onFinishHighlight,
                    onFinishScrollToTarget = onFinishScrollToTarget,
                    onInsert = onInsert,
                    onNavigateToAboutScreen = onNavigateToAboutScreen,
                    onPerformMessageAction = onPerformMessageAction,
                    onRepeat = onRepeat,
                    onRequestExportAll = onRequestExportAll,
                    onRequestExportFiltered = onRequestExportFiltered,
                    onRequestImport = onRequestImport,
                    onUpdate = onUpdate,
                )

                if (deleteAllDialogOpen) {
                    ConfirmationDialog(
                        title = stringResource(R.string.list_delete_all_dialog_title),
                        text = stringResource(R.string.list_delete_all_dialog_text),
                        confirmText = stringResource(R.string.list_delete_all_dialog_confirm),
                        dismissText = stringResource(R.string.list_delete_all_dialog_dismiss),
                        onConfirm = {
                            deleteAllDialogOpen = false
                            onDeleteAll()
                        },
                        onDismissRequest = { deleteAllDialogOpen = false },
                    )
                }
            }
        },
        detailPane = {
            AnimatedPane(Modifier.imePadding()) {
                val isListAndDetailVisible =
                    navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded &&
                        navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded

                val initialDraft = when (navigationState) {
                    is NavigationState.Loading -> null

                    is NavigationState.List -> (navigationState as NavigationState.List).lastDraft
                        // On narrow screen, show last draft during the exit animation of the detail pane
                        ?.takeIf { !isListAndDetailVisible }

                    is NavigationState.Detail -> (navigationState as NavigationState.Detail).initialDraft
                }

                if (initialDraft != null) {
                    EntryDetail(
                        defaultUnit = defaultUnit,
                        initialDraft = initialDraft,
                        onDismissDraft = onDismissDraft,
                        onFinishDraft = onFinishDraft,
                        onSetDefaultUnit = onSetDefaultUnit,
                        onUpsertDraft = onUpsertDraft,
                    )
                }
            }
        },
    )

    MainImportExport(
        state = importExportState,
        onFinishImportExport = onFinishImportExport,
        onReceiveImportExportUri = onReceiveImportExportUri,
    )
}

@Composable
private fun MainImportExport(
    state: StateFlow<ImportExportState?>,
    onFinishImportExport: () -> Unit,
    onReceiveImportExportUri: (uri: Uri) -> Unit,
) {
    val state by state.collectAsStateWithLifecycle()

    val selectFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.takeIf { result.resultCode == Activity.RESULT_OK }?.let { uri ->
                onReceiveImportExportUri(uri)
            } ?: onFinishImportExport()
        }

    LaunchedEffect(state) {
        when (state) {
            is ImportExportState.ImportRequested ->
                selectFileLauncher.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/*"
                    }
                )

            is ImportExportState.ExportAllRequested -> (state as? ImportExportState.ExportAllRequested)?.let { exportState ->
                selectFileLauncher.launch(
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/*"
                        putExtra(Intent.EXTRA_TITLE, exportState.filename)
                    }
                )
            }

            is ImportExportState.ExportFilteredRequested -> (state as? ImportExportState.ExportFilteredRequested)?.let { exportState ->
                selectFileLauncher.launch(
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TITLE, exportState.filename)
                    }
                )
            }

            null -> {}
        }
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ListPreview() {
    AppTheme {
        MainScreen(
            defaultUnit = MutableStateFlow(ClothingSizeUnit),
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            importExportState = MutableStateFlow(null),
            message = MutableStateFlow(null),
            navigationState = MutableStateFlow(NavigationState.List()),
            query = MutableStateFlow(null),
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissDraft = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishDraft = {},
            onFinishHighlight = {},
            onFinishImportExport = {},
            onFinishScrollToTarget = {},
            onInitialize = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onReceiveImportExportUri = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onSetDefaultUnit = {},
            onUpdate = {},
            onUpsertDraft = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailPreview() {
    AppTheme {
        MainScreen(
            defaultUnit = MutableStateFlow(ClothingSizeUnit),
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            importExportState = MutableStateFlow(null),
            message = MutableStateFlow(null),
            navigationState = MutableStateFlow(
                NavigationState.Detail(
                    initialDraft = with(defaultFakeEntries[0]) {
                        EntryDraft(
                            amount = amount,
                            amountUnit = amountUnit,
                            content = content,
                            createdAt = createdAt,
                            entryUid = -1,
                            uid = 0,
                        )
                    }
                )
            ),
            query = MutableStateFlow(null),
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissDraft = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishDraft = {},
            onFinishHighlight = {},
            onFinishImportExport = {},
            onInitialize = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onReceiveImportExportUri = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onFinishScrollToTarget = {},
            onSetDefaultUnit = {},
            onUpdate = {},
            onUpsertDraft = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.TABLET)
@Composable
private fun TabletListPreview() {
    AppTheme {
        MainScreen(
            defaultUnit = MutableStateFlow(ClothingSizeUnit),
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            importExportState = MutableStateFlow(null),
            message = MutableStateFlow(null),
            navigationState = MutableStateFlow(NavigationState.List()),
            query = MutableStateFlow(null),
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissDraft = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishDraft = {},
            onFinishHighlight = {},
            onFinishImportExport = {},
            onInitialize = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onReceiveImportExportUri = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onFinishScrollToTarget = {},
            onSetDefaultUnit = {},
            onUpdate = {},
            onUpsertDraft = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.TABLET)
@Composable
private fun TabletDetailPreview() {
    AppTheme {
        MainScreen(
            defaultUnit = MutableStateFlow(ClothingSizeUnit),
            entries = MutableStateFlow(PagingData.from(defaultFakeEntries)),
            highlightedUid = MutableStateFlow(null),
            importExportState = MutableStateFlow(null),
            message = MutableStateFlow(null),
            navigationState = MutableStateFlow(
                NavigationState.Detail(
                    initialDraft = with(defaultFakeEntries[0]) {
                        EntryDraft(
                            amount = amount,
                            amountUnit = amountUnit,
                            content = content,
                            createdAt = createdAt,
                            entryUid = -1,
                            uid = 0,
                        )
                    }
                )
            ),
            query = MutableStateFlow(null),
            scrollTarget = MutableStateFlow(null),
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            animationsEnabled = false,
            initialSelectedUid = null,
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onDismissDraft = {},
            onDismissMessage = {},
            onFilter = {},
            onFilterSimilar = {},
            onFinishDraft = {},
            onFinishHighlight = {},
            onFinishImportExport = {},
            onInitialize = {},
            onInsert = {},
            onNavigateToAboutScreen = {},
            onPerformMessageAction = {},
            onReceiveImportExportUri = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestExportFiltered = {},
            onRequestImport = {},
            onFinishScrollToTarget = {},
            onSetDefaultUnit = {},
            onUpdate = {},
            onUpsertDraft = {},
        )
    }
}
