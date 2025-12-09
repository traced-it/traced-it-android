package app.traced_it.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.ui.theme.AppTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmText: String,
    dismissText: String,
    onDismissRequest: () -> Unit = {},
    onConfirmation: () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
) {
    AlertDialog(
        title = { Text(title) },
        text = { Text(text) },
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                { onConfirmation() },
                modifier = Modifier.testTag("confirmationDialogConfirmButton"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = contentColor,
                ),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                { onDismissRequest() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = contentColor,
                ),
            ) {
                Text(dismissText)
            }
        },
        containerColor = containerColor,
        textContentColor = contentColor,
    )
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        ConfirmationDialog(
            title = "Delete all entries",
            text = "You are about to delete all entries. This action cannot be undone.",
            confirmText = "Delete all",
            dismissText = "Dismiss",
        )
    }
}
