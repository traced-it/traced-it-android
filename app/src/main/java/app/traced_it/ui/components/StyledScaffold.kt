package app.traced_it.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traced_it.ui.Message
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.flow.StateFlow

private class MessageSnackbarVisuals(tracedMessage: Message) : SnackbarVisuals {
    override val message = tracedMessage.text
    override val actionLabel = tracedMessage.actionLabel
    override val withDismissAction = tracedMessage.withDismissAction
    override val duration = when (tracedMessage.duration) {
        Message.Duration.SHORT -> SnackbarDuration.Short
        Message.Duration.LONG -> SnackbarDuration.Long
        Message.Duration.INDEFINITE -> SnackbarDuration.Indefinite
    }
    val isError = tracedMessage.type == Message.Type.ERROR
}

@Composable
fun StyledScaffold(
    topBar: @Composable (() -> Unit) = {},
    snackbarHost: @Composable (() -> Unit) = {},
    content: @Composable ((PaddingValues) -> Unit) = {},
) {
    Scaffold(
        Modifier.semantics { testTagsAsResourceId = true },
        topBar = topBar,
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}

@Composable
fun StyledScaffoldWithMessage(
    message: StateFlow<Message?>,
    onDismissMessage: () -> Unit,
    onPerformMessageAction: () -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val message by message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let { message ->
            val result = snackbarHostState.showSnackbar(MessageSnackbarVisuals(message))
            when (result) {
                SnackbarResult.ActionPerformed -> onPerformMessageAction()
                SnackbarResult.Dismissed -> onDismissMessage()
            }
        }
    }

    StyledScaffold(
        topBar = topBar,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = Spacing.bottomButtonHeight + Spacing.small + Spacing.small),
            ) { snackbarData ->
                val isError = (snackbarData.visuals as? MessageSnackbarVisuals)?.isError ?: false
                val containerColor = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.inverseSurface
                }
                val contentColor = contentColorFor(containerColor)
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    dismissActionContentColor = contentColor,
                    actionColor = contentColor,
                )
            }
        },
        content = content,
    )
}
