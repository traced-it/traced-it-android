package app.traced_it.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.traced_it.ui.entry.EntryListScreen
import app.traced_it.ui.entry.EntryViewModel

@Composable
fun MainNavigation(viewModel: EntryViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "entries") {
        composable("about") {
            AboutScreen(
                onNavigateToEntries = { navController.navigate("entries") }
            )
        }
        composable("entries") {
            EntryListScreen(
                onNavigateToAboutScreen = { navController.navigate("about") },
                viewModel = viewModel,
            )
        }
    }
}
