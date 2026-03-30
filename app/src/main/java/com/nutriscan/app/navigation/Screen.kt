package com.nutriscan.app.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object AuthChoice : Screen("auth_choice")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ScannerRoot : Screen("scanner_root")
    data object Camera : Screen("scanner")
    data object Result : Screen("result/{barcode}") {
        fun createRoute(barcode: String) = "result/$barcode"
    }
    data object NotFound : Screen("not_found")
    data object History : Screen("history")
    data object Help : Screen("help")
    data object FirstAid : Screen("first_aid")
    data object Additives : Screen("additives")
    data object Settings : Screen("settings")
}

sealed class BottomNavItem(
    val route: String,
    val navigateRoute: String = route,
    val titleRes: Int,
    val icon: String
) {
    data object Home : BottomNavItem(
        route = Screen.ScannerRoot.route,
        navigateRoute = Screen.Camera.route,
        titleRes = com.nutriscan.app.R.string.nav_home,
        icon = "home"
    )
    data object History : BottomNavItem("history", titleRes = com.nutriscan.app.R.string.nav_history, icon = "history")
    data object Help : BottomNavItem(Screen.Help.route, titleRes = com.nutriscan.app.R.string.nav_help, icon = "help")
    data object Settings : BottomNavItem("settings", titleRes = com.nutriscan.app.R.string.nav_settings, icon = "settings")
}
