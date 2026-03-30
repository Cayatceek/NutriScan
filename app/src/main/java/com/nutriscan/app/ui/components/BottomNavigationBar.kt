package com.nutriscan.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nutriscan.app.navigation.BottomNavItem
import androidx.compose.ui.unit.dp

private val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.History,
    BottomNavItem.Help,
    BottomNavItem.Settings
)

@Composable
fun BottomNavigationBar(
    selectedRoute: String,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BottomNavigationBarContent(
        selectedRoute = selectedRoute,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination
    val selectedRoute = remember(currentDestination) {
        bottomNavItems.firstOrNull { item ->
            currentDestination?.hierarchy?.any { destination ->
                destination.route == item.route
            } == true
        }?.route ?: BottomNavItem.Home.route
    }

    BottomNavigationBarContent(
        selectedRoute = selectedRoute,
        onItemSelected = { item ->
            if (selectedRoute != item.route) {
                val restored = navController.safePopToRoute(item.navigateRoute)
                if (!restored) {
                    navController.navigate(item.navigateRoute) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun BottomNavigationBarContent(
    selectedRoute: String,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        NavigationBar(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            bottomNavItems.forEach { item ->
                val selected = item.route == selectedRoute
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = when (item) {
                                is BottomNavItem.Home -> Icons.Outlined.Home
                                is BottomNavItem.History -> Icons.Outlined.History
                                is BottomNavItem.Help -> Icons.Outlined.LocalHospital
                                is BottomNavItem.Settings -> Icons.Outlined.Settings
                            },
                            contentDescription = stringResource(item.titleRes)
                        )
                    },
                    label = { Text(stringResource(item.titleRes)) },
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            onItemSelected(item)
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

object NavigationBarDefaults {
    val Elevation = 8.dp
}

private fun NavController.safePopToRoute(route: String, inclusive: Boolean = false): Boolean {
    val hasTarget = runCatching { getBackStackEntry(route) }.isSuccess
    return if (hasTarget) popBackStack(route, inclusive) else false
}
