package app.traced_it.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import app.traced_it.R
import app.traced_it.data.database.Entry
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun EntryListFilterHeader(
    entries: Flow<PagingData<Entry>>,
    query: StateFlow<String?>,
    totalCount: StateFlow<Int>,
    onRequestExportFiltered: () -> Unit,
) {
    val entries = entries.collectAsLazyPagingItems()
    val query by query.collectAsStateWithLifecycle()
    val totalCount by totalCount.collectAsStateWithLifecycle()

    if (query != null) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                pluralStringResource(R.plurals.list_filter_status, entries.itemCount, entries.itemCount, totalCount),
                Modifier
                    .padding(horizontal = Spacing.windowPadding)
                    .testTag(TestTags.entryListFilterStatus(entries.itemCount, totalCount)),
                style = MaterialTheme.typography.labelSmall,
            )
            TextButton(
                onClick = onRequestExportFiltered,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag(TestTags.ENTRY_LIST_FILTER_EXPORT),
                enabled = entries.itemCount > 0,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    stringResource(R.string.list_filter_export),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
