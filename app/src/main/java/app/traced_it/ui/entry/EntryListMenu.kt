package app.traced_it.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.traced_it.R

@Composable
fun EntryListMenu(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onDeleteAllEntries: () -> Unit = {},
    onExportAllEntries: () -> Unit = {},
    onImportEntries: () -> Unit = {},
    onNavigateToAboutScreen: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

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
                    onImportEntries()
                },
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_menu_export_all)) },
                {
                    expanded = false
                    onExportAllEntries()
                },
                enabled = enabled,
            )
            DropdownMenuItem(
                { Text(stringResource(R.string.list_menu_delete_all)) },
                {
                    expanded = false
                    onDeleteAllEntries()
                },
                enabled = enabled,
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
