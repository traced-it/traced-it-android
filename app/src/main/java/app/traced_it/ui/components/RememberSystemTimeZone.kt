package app.traced_it.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traced_it.data.extensions.systemTimeZoneFlow
import kotlinx.datetime.TimeZone

@Composable
fun rememberSystemTimeZone(): TimeZone {
    val context = LocalContext.current
    val flow = remember(context) { context.systemTimeZoneFlow() }
    return flow.collectAsStateWithLifecycle(initialValue = TimeZone.currentSystemDefault()).value
}
