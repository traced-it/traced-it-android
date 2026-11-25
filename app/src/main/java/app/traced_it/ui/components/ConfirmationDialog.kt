package app.traced_it.ui.components

import android.annotation.SuppressLint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.ui.theme.AppTheme

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = { Text(title) },
        text = { Text(text) },
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onConfirm,
                modifier = Modifier.testTag(TestTags.CONFIRMATION_DIALOG_CONFIRM),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondary),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onDismissRequest,
                modifier = Modifier.testTag(TestTags.CONFIRMATION_DIALOG_DISMISS),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondary),
            ) {
                Text(dismissText)
            }
        },
        containerColor = MaterialTheme.colorScheme.secondary,
        textContentColor = MaterialTheme.colorScheme.onSecondary,
    )
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
        Scaffold {
            ConfirmationDialog(
                title = "Delete all entries",
                text = "You are about to delete all entries. This action cannot be undone.",
                confirmText = "Delete all",
                dismissText = "Dismiss",
                onConfirm = {},
                onDismissRequest = {},
            )
        }
    }
}
