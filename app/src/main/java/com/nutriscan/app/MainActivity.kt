package com.nutriscan.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.localization.LocalLocalizedAppContext
import com.nutriscan.app.localization.LocaleManager
import com.nutriscan.app.localization.asStringOrNull
import com.nutriscan.app.navigation.BottomNavItem
import com.nutriscan.app.navigation.Screen
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.StartupBootstrapScreen
import com.nutriscan.app.ui.screens.AdditivesEncyclopediaScreen
import com.nutriscan.app.ui.screens.AuthChoiceScreen
import com.nutriscan.app.ui.screens.CameraScreen
import com.nutriscan.app.ui.screens.FirstAidScreen
import com.nutriscan.app.ui.screens.HelpCareScreen
import com.nutriscan.app.ui.screens.HistoryScreen
import com.nutriscan.app.ui.screens.LoginScreen
import com.nutriscan.app.ui.screens.NotFoundScreen
import com.nutriscan.app.ui.screens.OnboardingScreen
import com.nutriscan.app.ui.screens.RegisterScreen
import com.nutriscan.app.ui.screens.ResultScreen
import com.nutriscan.app.ui.screens.SettingsScreen
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.viewmodel.AppBootstrapViewModel
import com.nutriscan.app.viewmodel.AppShellViewModel
import com.nutriscan.app.viewmodel.BootstrapState
import com.nutriscan.app.viewmodel.CameraViewModel
import com.nutriscan.app.viewmodel.ScannerPanel
import com.nutriscan.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private const val SHELL_SELECTED_TAB_ROUTE_KEY = "shell_selected_tab_route"

class MainActivity : AppCompatActivity() {

    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsDataStore = SettingsDataStore(this)
        enableEdgeToEdge()
        setContent {
            val bootstrapViewModel: AppBootstrapViewModel = viewModel()
            val bootstrapState by bootstrapViewModel.uiState.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current
            val baseConfiguration = LocalConfiguration.current
            val persistedLanguageCode by settingsDataStore.language.collectAsStateWithLifecycle(
                initialValue = bootstrapState.languageCode
            )
            val runtimeLanguageCode = when (bootstrapState.bootstrapState) {
                BootstrapState.Ready -> persistedLanguageCode
                else -> bootstrapState.languageCode
            }
            val localizedConfiguration = remember(baseConfiguration, runtimeLanguageCode) {
                LocaleManager.createLocalizedConfiguration(baseConfiguration, runtimeLanguageCode)
            }
            val localizedContext = remember(baseContext, baseConfiguration, runtimeLanguageCode) {
                LocaleManager.createLocalizedContext(
                    baseContext = baseContext,
                    baseConfiguration = baseConfiguration,
                    languageCode = runtimeLanguageCode
                )
            }

            LaunchedEffect(Unit) {
                bootstrapViewModel.loadIfNeeded()
            }

            val bootstrapTheme = bootstrapState.theme.toDarkTheme()

            CompositionLocalProvider(
                LocalLocalizedAppContext provides localizedContext,
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration
            ) {
                when (bootstrapState.bootstrapState) {
                    BootstrapState.Loading -> {
                        NutriScanTheme(darkTheme = bootstrapTheme, dynamicColor = false) {
                            StartupBootstrapScreen()
                        }
                    }

                    BootstrapState.Error -> {
                        NutriScanTheme(darkTheme = bootstrapTheme, dynamicColor = false) {
                            ErrorState(
                                title = stringResource(R.string.result_error_title),
                                message = bootstrapState.errorMessage.asStringOrNull() ?: stringResource(R.string.error_generic),
                                actionLabel = stringResource(R.string.retry),
                                onAction = bootstrapViewModel::loadIfNeeded
                            )
                        }
                    }

                    BootstrapState.Ready -> {
                        NutriScanAppShell(
                            initialTheme = bootstrapState.theme,
                            onboardingCompleted = bootstrapState.onboardingCompleted,
                            isAuthenticated = bootstrapState.isAuthenticated,
                            settingsDataStore = settingsDataStore
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NutriScanAppShell(
    initialTheme: String,
    onboardingCompleted: Boolean,
    isAuthenticated: Boolean,
    settingsDataStore: SettingsDataStore
) {
    val shellViewModel: AppShellViewModel = viewModel()
    val shellState by shellViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val resolvedTheme = if (shellState.theme == "system") initialTheme else shellState.theme
    val startDestination = when {
        !onboardingCompleted -> Screen.Onboarding.route
        isAuthenticated -> Screen.ScannerRoot.route
        else -> Screen.AuthChoice.route
    }
    val returnToScanner: () -> Unit = {
        navController.setShellSelectedTab(BottomNavItem.Home.route)
        val restored = navController.safePopToRoute(Screen.ScannerRoot.route)
        if (!restored) {
            navController.navigateToScannerRoot()
        }
    }

    LaunchedEffect(initialTheme) {
        shellViewModel.start(initialTheme)
    }

    NutriScanTheme(darkTheme = resolvedTheme.toDarkTheme(), dynamicColor = false) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onContinue = {
                        scope.launch {
                            settingsDataStore.setOnboardingCompleted(true)
                            navController.navigate(Screen.AuthChoice.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.AuthChoice.route) {
                AuthChoiceScreen(
                    onLogin = { navController.navigate(Screen.Login.route) },
                    onRegister = { navController.navigate(Screen.Register.route) },
                    onLater = {
                        navController.navigate(Screen.ScannerRoot.route) {
                            popUpTo(Screen.AuthChoice.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.ScannerRoot.route) {
                            popUpTo(Screen.AuthChoice.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.ScannerRoot.route) {
                            popUpTo(Screen.AuthChoice.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ScannerRoot.route) { backStackEntry ->
                val cameraViewModel: CameraViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry
                )
                MainTabShell(
                    navController = navController,
                    shellBackStackEntry = backStackEntry,
                    cameraViewModel = cameraViewModel
                )
            }

            composable(Screen.NotFound.route) { backStackEntry ->
                val scannerGraphEntry = androidx.compose.runtime.remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.ScannerRoot.route)
                }
                val cameraViewModel: CameraViewModel = viewModel(
                    viewModelStoreOwner = scannerGraphEntry
                )
                NotFoundScreen(
                    viewModel = cameraViewModel,
                    onScanAgain = {
                        cameraViewModel.resetToIdle()
                        navController.popBackStack()
                    },
                    onManualSearch = {
                        cameraViewModel.openPanel(ScannerPanel.MANUAL)
                        navController.popBackStack()
                    },
                    onPhotoInput = {
                        cameraViewModel.openPanel(ScannerPanel.PHOTO)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Result.route) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                ResultScreen(
                    productId = barcode,
                    onNavigateBack = { navController.popBackStack() },
                    onScanAgain = returnToScanner,
                    onOpenAlternative = { alternativeId ->
                        navController.navigate(Screen.Result.createRoute(alternativeId))
                    }
                )
            }

            composable(Screen.FirstAid.route) {
                FirstAidScreen(navController = navController)
            }

            composable(Screen.Additives.route) {
                AdditivesEncyclopediaScreen(navController = navController)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MainTabShell(
    navController: NavController,
    shellBackStackEntry: androidx.navigation.NavBackStackEntry,
    cameraViewModel: CameraViewModel
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = shellBackStackEntry
    )
    val selectedTabRoute by shellBackStackEntry.savedStateHandle
        .getStateFlow(SHELL_SELECTED_TAB_ROUTE_KEY, BottomNavItem.Home.route)
        .collectAsStateWithLifecycle()
    var visitedTabRoutes by rememberSaveable {
        mutableStateOf(listOf<String>(BottomNavItem.Home.route))
    }

    LaunchedEffect(selectedTabRoute) {
        if (selectedTabRoute !in visitedTabRoutes) {
            visitedTabRoutes = visitedTabRoutes + selectedTabRoute
        }
    }

    BackHandler(enabled = selectedTabRoute != BottomNavItem.Home.route) {
        shellBackStackEntry.savedStateHandle[SHELL_SELECTED_TAB_ROUTE_KEY] = BottomNavItem.Home.route
    }

    androidx.compose.material3.Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(
                selectedRoute = selectedTabRoute,
                onItemSelected = { item ->
                    shellBackStackEntry.savedStateHandle[SHELL_SELECTED_TAB_ROUTE_KEY] = item.route
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PersistentTabPane(visible = selectedTabRoute == BottomNavItem.Home.route) {
                CameraScreen(
                    navController = navController,
                    viewModel = cameraViewModel,
                    onNavigateToResult = { barcode ->
                        navController.navigate(Screen.Result.createRoute(barcode))
                    },
                    onNavigateToNotFound = {
                        navController.navigate(Screen.NotFound.route)
                    },
                    isTabVisible = selectedTabRoute == BottomNavItem.Home.route,
                    showBottomBar = false
                )
            }

            if (BottomNavItem.History.route in visitedTabRoutes) {
                PersistentTabPane(visible = selectedTabRoute == BottomNavItem.History.route) {
                    HistoryScreen(
                        navController = navController,
                        onProductClick = { barcode ->
                            navController.navigate(Screen.Result.createRoute(barcode))
                        },
                        showBottomBar = false
                    )
                }
            }

            if (BottomNavItem.Help.route in visitedTabRoutes) {
                PersistentTabPane(visible = selectedTabRoute == BottomNavItem.Help.route) {
                    HelpCareScreen(
                        navController = navController,
                        showBottomBar = false
                    )
                }
            }

            if (BottomNavItem.Settings.route in visitedTabRoutes) {
                PersistentTabPane(visible = selectedTabRoute == BottomNavItem.Settings.route) {
                    SettingsScreen(
                        navController = navController,
                        viewModel = settingsViewModel,
                        showBottomBar = false
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PersistentTabPane(
    visible: Boolean,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (visible) 1f else 0f)
            .graphicsLayer {
                alpha = if (visible) 1f else 0f
            }
    ) {
        content()
    }
}

@androidx.compose.runtime.Composable
private fun String.toDarkTheme(): Boolean {
    return when (this) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
}

private fun NavController.navigateToScannerRoot() {
    navigate(Screen.ScannerRoot.route) {
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavController.setShellSelectedTab(route: String) {
    runCatching {
        getBackStackEntry(Screen.ScannerRoot.route).savedStateHandle[SHELL_SELECTED_TAB_ROUTE_KEY] = route
    }
}

private fun NavController.safePopToRoute(route: String, inclusive: Boolean = false): Boolean {
    val hasTarget = runCatching { getBackStackEntry(route) }.isSuccess
    return if (hasTarget) popBackStack(route, inclusive) else false
}
