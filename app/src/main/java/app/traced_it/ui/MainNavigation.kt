package app.traced_it.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

@Serializable
data object AboutRoute

@Serializable
data object MainRoute

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MainRoute) {
        composable<AboutRoute> {
            AboutScreen(
                onNavigateToMainScreen = { navController.navigate(MainRoute) },
            )
        }
        composable<MainRoute> {
            MainScreen(
                onNavigateToAboutScreen = { navController.navigate(AboutRoute) },
            )
        }
    }
}
