package com.dualphone.controller.ui.navigation

sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    data object Controller : NavRoutes("controller")
    data object Gaming : NavRoutes("gaming")
    data object Editor : NavRoutes("editor")
    data object Settings : NavRoutes("settings")
}
