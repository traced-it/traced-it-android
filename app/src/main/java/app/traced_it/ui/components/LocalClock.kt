package app.traced_it.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.Clock

val LocalClock = staticCompositionLocalOf<Clock> { Clock.System }
