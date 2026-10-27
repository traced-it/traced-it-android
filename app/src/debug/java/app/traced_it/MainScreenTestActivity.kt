package app.traced_it

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import app.traced_it.ui.MainScreen
import app.traced_it.ui.components.LocalClock
import app.traced_it.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Activity that contains only [MainScreen] and that can be injected by Hilt and recreated using a scenario.
 */
@AndroidEntryPoint
class MainScreenTestActivity : ComponentActivity() {
    @Inject
    lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                CompositionLocalProvider(LocalClock provides clock) {
                    MainScreen(onNavigateToAboutScreen = {})
                }
            }
        }
    }
}
