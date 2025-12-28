package app.traced_it.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.traced_it.R

@Composable
fun SelectedEntryMenu(
    modifier: Modifier = Modifier,
    onAddWithSameContent: () -> Unit = {},
    onCopy: () -> Unit = {},
    onFilterWithSimilarContent: () -> Unit = {},
) {
    var expanded by retain { mutableStateOf(false) }

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
                    onAddWithSameContent()
                },
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_item_find_with_similar_content)) },
                {
                    expanded = false
                    onFilterWithSimilarContent()
                },
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_item_copy_to_clipboard)) },
                {
                    expanded = false
                    onCopy()
                },
            )
        }
    }
}
