package app.traced_it.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.traced_it.R

@Composable
fun SelectedEntryMenu(
    modifier: Modifier = Modifier,
    onAddWithSameText: () -> Unit = {},
    onCopy: () -> Unit = {},
    onFindWithSameText: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton({ expanded = true }, modifier) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(
                    R.string.list_menu
                ),
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
                    onAddWithSameText()
                },
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_item_find_with_same_text)) },
                {
                    expanded = false
                    onFindWithSameText()
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
