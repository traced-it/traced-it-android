package app.traced_it.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import app.traced_it.ui.components.LocalClock
import app.traced_it.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Clock

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AppTheme {
                CompositionLocalProvider(LocalClock provides clock) {
                    MainNavigation()
                }
            }
        }
    }
}
