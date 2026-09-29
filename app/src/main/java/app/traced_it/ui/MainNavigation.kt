package app.traced_it.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.traced_it.ui.entry.EntryListScreen
import app.traced_it.ui.entry.EntryViewModel
import kotlinx.serialization.Serializable

@Serializable
object AboutRoute

@Serializable
object EntriesRoute

@Composable
fun MainNavigation(viewModel: EntryViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = EntriesRoute) {
        composable<AboutRoute> {
            AboutScreen(
                onNavigateToEntries = { navController.navigate(EntriesRoute) }
            )
        }
        composable<EntriesRoute> {
            EntryListScreen(
                onNavigateToAboutScreen = { navController.navigate(AboutRoute) },
                viewModel = viewModel,
            )
        }
    }
}
