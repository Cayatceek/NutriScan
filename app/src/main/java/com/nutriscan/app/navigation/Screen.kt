package com.nutriscan.app.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Camera : Screen("scanner")
    data object Result : Screen("result/{barcode}") {
        fun createRoute(barcode: String) = "result/$barcode"
    }
    data object NotFound : Screen("not_found")
    data object History : Screen("history")
    data object FirstAid : Screen("first_aid")
    data object Settings : Screen("settings")
}

sealed class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val icon: String
) {
    data object Home : BottomNavItem(Screen.Camera.route, com.nutriscan.app.R.string.nav_home, "home")
    data object History : BottomNavItem("history", com.nutriscan.app.R.string.nav_history, "history")
    data object FirstAid : BottomNavItem("first_aid", com.nutriscan.app.R.string.first_aid_title, "first_aid")
    data object Settings : BottomNavItem("settings", com.nutriscan.app.R.string.nav_settings, "settings")
}
