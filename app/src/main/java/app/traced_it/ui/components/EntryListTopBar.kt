package app.traced_it.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traced_it.R
import app.traced_it.data.di.defaultFakeEntries
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Darker
import app.traced_it.ui.theme.Gray
import app.traced_it.ui.theme.Spacing
import app.traced_it.ui.theme.textFieldColors
import app.traced_it.ui.theme.topAppBarColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

@Composable
fun EntryListTopBar(
    query: StateFlow<String?>,
    loading: Boolean,
    readOnly: Boolean,
    selectedUid: State<Int?>,
    totalCount: StateFlow<Int>,
    onCopyToClipboard: (uid: Int) -> Unit,
    onDelete: (uid: Int) -> Unit,
    onDeleteAll: () -> Unit,
    onFilter: (query: String?) -> Unit,
    onFilterSimilar: (uid: Int) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onRepeat: (uid: Int) -> Unit,
    onRequestExportAll: () -> Unit,
    onRequestImport: () -> Unit,
    onSelect: (uid: Int?) -> Unit,
    onUpdate: (uid: Int) -> Unit,
) {
    val query by query.collectAsStateWithLifecycle()

    selectedUid.value?.let {
        SelectedEntryListTopBar(
            selectedUid = selectedUid,
            readOnly = readOnly,
            onCopyToClipboard = onCopyToClipboard,
            onDelete = onDelete,
            onFilterSimilar = onFilterSimilar,
            onRepeat = onRepeat,
            onSelect = onSelect,
            onUpdate = onUpdate,
        )
    } ?: query?.let { query ->
        FilterEntryListTopBar(
            query = query,
            onFilter = onFilter,
        )
    } ?: run {
        DefaultEntryListTopBar(
            loading = loading,
            readOnly = readOnly,
            totalCount = totalCount,
            onDeleteAll = onDeleteAll,
            onFilter = onFilter,
            onNavigateToAboutScreen = onNavigateToAboutScreen,
            onRequestExportAll = onRequestExportAll,
            onRequestImport = onRequestImport,
        )
    }
}

@Composable
private fun DefaultEntryListTopBar(
    totalCount: StateFlow<Int>,
    loading: Boolean = false,
    readOnly: Boolean = false,
    onDeleteAll: () -> Unit,
    onFilter: (query: String?) -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onRequestExportAll: () -> Unit,
    onRequestImport: () -> Unit,
) {
    val totalCount by totalCount.collectAsStateWithLifecycle()

    TopAppBar(
        title = {
            if (!loading) {
                Text(
                    stringResource(R.string.list_title, totalCount),
                    Modifier.testTag(TestTags.ENTRY_LIST_TITLE),
                )
            }
        },
        actions = {
            IconButton(
                { onFilter("") },
                Modifier.testTag(TestTags.ENTRY_LIST_FILTER_EXPAND),
                enabled = totalCount > 0,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.list_filter_submit),
                )
            }
            DefaultEntryListMenu(
                hasEntries = totalCount > 0,
                modifier = Modifier
                    .padding(end = Spacing.windowPadding - 8.dp)
                    .testTag(TestTags.ENTRY_LIST_MENU),
                readOnly = readOnly,
                onDeleteAll = onDeleteAll,
                onRequestExportAll = onRequestExportAll,
                onRequestImport = onRequestImport,
                onNavigateToAboutScreen = onNavigateToAboutScreen,
            )
        },
        colors = topAppBarColors(),
    )
}

@Composable
private fun DefaultEntryListMenu(
    hasEntries: Boolean,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onDeleteAll: () -> Unit,
    onNavigateToAboutScreen: () -> Unit,
    onRequestExportAll: () -> Unit,
    onRequestImport: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton({ expanded = true }, modifier) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.list_menu),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                { Text(stringResource(R.string.list_menu_import)) },
                {
                    expanded = false
                    onRequestImport()
                },
                Modifier.testTag(TestTags.ENTRY_LIST_MENU_IMPORT),
                enabled = !readOnly,
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_menu_export_all)) },
                {
                    expanded = false
                    onRequestExportAll()
                },
                Modifier.testTag(TestTags.ENTRY_LIST_MENU_EXPORT_ALL),
                enabled = hasEntries,
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_menu_delete_all)) },
                {
                    expanded = false
                    onDeleteAll()
                },
                Modifier.testTag(TestTags.ENTRY_LIST_MENU_DELETE_ALL),
                enabled = !readOnly && hasEntries,
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.about_title)) },
                {
                    expanded = false
                    onNavigateToAboutScreen()
                },
            )
        }
    }
}

@Composable
private fun FilterEntryListTopBar(
    query: String,
    onFilter: (query: String?) -> Unit,
) {
    val filterFocusRequester = remember { FocusRequester() }
    val state = rememberTextFieldState()

    LaunchedEffect(query) {
        if (query != state.text) {
            state.setTextAndPlaceCursorAtEnd(query)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1) // Skip initial value
            .collectLatest(onFilter)
    }

    LaunchedEffect(Unit) {
        filterFocusRequester.requestFocus()
    }

    BackHandler(true) {
        onFilter(if (state.text.isNotEmpty()) "" else null)
    }

    TopAppBar(
        title = {
            TextField(
                state = state,
                modifier = Modifier
                    .focusRequester(filterFocusRequester)
                    .testTag(TestTags.ENTRY_LIST_FILTER_QUERY),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text(stringResource(R.string.list_filter_input_placeholder)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = textFieldColors(
                    containerColor = Color.Transparent,
                    indicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    placeholderColor = if (isSystemInDarkTheme()) Gray else Darker,
                    textColor = MaterialTheme.colorScheme.onBackground,
                ),
                contentPadding = PaddingValues.Zero,
            )
        },
        actions = {
            IconButton(
                {
                    if (state.text.isNotEmpty()) {
                        state.clearText()
                    } else {
                        onFilter(null)
                    }
                },
                Modifier.testTag(TestTags.ENTRY_LIST_FILTER_CLEAR),
            ) {
                Icon(
                    Icons.Outlined.Clear,
                    contentDescription = stringResource(R.string.list_filter_input_clear_content_description),
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = { onFilter(null) },
                modifier = Modifier.testTag(TestTags.ENTRY_LIST_FILTER_BACK),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.list_filter_close_content_description),
                )
            }
        },
        colors = topAppBarColors(),
    )
}

@Composable
private fun SelectedEntryListTopBar(
    selectedUid: State<Int?>,
    readOnly: Boolean = false,
    onCopyToClipboard: (uid: Int) -> Unit,
    onDelete: (uid: Int) -> Unit,
    onFilterSimilar: (uid: Int) -> Unit,
    onRepeat: (uid: Int) -> Unit,
    onSelect: (uid: Int?) -> Unit,
    onUpdate: (uid: Int) -> Unit,
) {
    BackHandler(true) {
        onSelect(null)
    }

    TopAppBar(
        title = {
            Text(
                stringResource(R.string.list_selected_title),
                Modifier.testTag(TestTags.ENTRY_LIST_SELECTED_TITLE),
            )
        },
        actions = {
            IconButton(
                { selectedUid.value?.let { onUpdate(it) } },
                Modifier.testTag(TestTags.ENTRY_LIST_SELECTED_UPDATE),
                enabled = !readOnly,
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    stringResource(R.string.list_item_update),
                )
            }
            IconButton(
                {
                    selectedUid.value?.let { onDelete(it) }
                    onSelect(null)
                },
                Modifier.testTag(TestTags.ENTRY_LIST_SELECTED_DELETE),
                enabled = !readOnly,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    stringResource(R.string.list_item_delete),
                )
            }
            SelectedEntryListMenu(
                modifier = Modifier
                    .padding(end = Spacing.windowPadding - 8.dp)
                    .testTag(TestTags.ENTRY_LIST_SELECTED_MENU),
                readOnly = readOnly,
                onCopy = { selectedUid.value?.let { onCopyToClipboard(it) } },
                onFilter = {
                    selectedUid.value?.let { onFilterSimilar(it) }
                    onSelect(null)
                },
                onRepeat = { selectedUid.value?.let { onRepeat(it) } },
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { onSelect(null) },
                modifier = Modifier.testTag(TestTags.ENTRY_LIST_SELECTED_BACK),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.list_selected_back),
                )
            }
        },
        colors = topAppBarColors(),
    )
}

@Composable
private fun SelectedEntryListMenu(
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onCopy: () -> Unit,
    onFilter: () -> Unit,
    onRepeat: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton({ expanded = true }, modifier) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.list_menu),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                { Text(stringResource(R.string.list_item_add)) },
                {
                    expanded = false
                    onRepeat()
                },
                Modifier.testTag(TestTags.ENTRY_LIST_SELECTED_MENU_REPEAT),
                enabled = !readOnly,
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_item_find_with_similar_content)) },
                {
                    expanded = false
                    onFilter()
                },
                Modifier.testTag(TestTags.ENTRY_LIST_SELECTED_MENU_FILTER_SIMILAR),
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_item_copy_to_clipboard)) },
                {
                    expanded = false
                    onCopy()
                },
                Modifier.testTag(TestTags.ENTRY_LIST_SELECTED_MENU_COPY),
            )
        }
    }
}

// Previews

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPreview() {
    AppTheme {
        EntryListTopBar(
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            selectedUid = remember { mutableStateOf(null) },
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LightPreview() {
    AppTheme {
        EntryListTopBar(
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            selectedUid = remember { mutableStateOf(null) },
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReadOnlyPreview() {
    AppTheme {
        EntryListTopBar(
            loading = false,
            query = MutableStateFlow(null),
            readOnly = true,
            selectedUid = remember { mutableStateOf(null) },
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilterPreview() {
    AppTheme {
        EntryListTopBar(
            loading = false,
            query = MutableStateFlow("ee"),
            readOnly = false,
            selectedUid = remember { mutableStateOf(null) },
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LightFilterPreview() {
    AppTheme {
        EntryListTopBar(
            loading = false,
            query = MutableStateFlow("ee"),
            readOnly = false,
            selectedUid = remember { mutableStateOf(null) },
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectedEntryPreview() {
    AppTheme {
        EntryListTopBar(
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            selectedUid = remember { mutableIntStateOf(defaultFakeEntries[2].uid) },
            totalCount = MutableStateFlow(defaultFakeEntries.size),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyPreview() {
    AppTheme {
        EntryListTopBar(
            loading = false,
            query = MutableStateFlow(null),
            readOnly = false,
            selectedUid = remember { mutableStateOf(null) },
            totalCount = MutableStateFlow(0),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingPreview() {
    AppTheme {
        EntryListTopBar(
            loading = true,
            query = MutableStateFlow(null),
            readOnly = true,
            selectedUid = remember { mutableStateOf(null) },
            totalCount = MutableStateFlow(0),
            onCopyToClipboard = {},
            onDelete = {},
            onDeleteAll = {},
            onFilter = {},
            onFilterSimilar = {},
            onNavigateToAboutScreen = {},
            onRepeat = {},
            onRequestExportAll = {},
            onRequestImport = {},
            onSelect = {},
            onUpdate = {},
        )
    }
}
