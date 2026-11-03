package com.dualphone.controller.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dualphone.controller.ui.controller.ControllerScreen
import com.dualphone.controller.ui.editor.LayoutEditorScreen
import com.dualphone.controller.ui.gaming.GamingModeScreen
import com.dualphone.controller.ui.home.HomeScreen
import com.dualphone.controller.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route,
        modifier = modifier
    ) {
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onNavigateController = { navController.navigate(NavRoutes.Controller.route) },
                onNavigateGaming = { navController.navigate(NavRoutes.Gaming.route) },
                onNavigateEditor = { navController.navigate(NavRoutes.Editor.route) },
                onNavigateSettings = { navController.navigate(NavRoutes.Settings.route) }
            )
        }
        composable(NavRoutes.Controller.route) {
            ControllerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.Gaming.route) {
            GamingModeScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.Editor.route) {
            LayoutEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
