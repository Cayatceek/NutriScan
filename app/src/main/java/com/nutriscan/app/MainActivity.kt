package com.nutriscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.localization.LocaleManager
import com.nutriscan.app.navigation.Screen
import com.nutriscan.app.ui.screens.CameraScreen
import com.nutriscan.app.ui.screens.FirstAidScreen
import com.nutriscan.app.ui.screens.HistoryScreen
import com.nutriscan.app.ui.screens.NotFoundScreen
import com.nutriscan.app.ui.screens.OnboardingScreen
import com.nutriscan.app.ui.screens.ResultScreen
import com.nutriscan.app.ui.screens.SettingsScreen
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.ThemeManager
import com.nutriscan.app.viewmodel.CameraViewModel
import com.nutriscan.app.viewmodel.ScannerPanel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsDataStore = SettingsDataStore(this)

        runBlocking {
            val savedTheme = settingsDataStore.theme.first()
            ThemeManager.applyTheme(savedTheme)

            val savedLanguage = settingsDataStore.language.first()
            val hasManualLanguageSelection = settingsDataStore.isLanguageManuallySelected.first()
            val resolvedLanguage = LocaleManager.resolveStartupLanguage(
                savedLanguageCode = savedLanguage,
                hasManualSelection = hasManualLanguageSelection
            )

            LocaleManager.applyLanguage(resolvedLanguage)

            if (!hasManualLanguageSelection && savedLanguage != resolvedLanguage.code) {
                settingsDataStore.setResolvedLanguage(resolvedLanguage.code)
            }
        }

        enableEdgeToEdge()
        setContent {
            val theme by settingsDataStore.theme.collectAsState(initial = "system")
            val onboardingCompleted by settingsDataStore.hasCompletedOnboarding
                .map { it as Boolean? }
                .collectAsState(initial = null)
            val isDarkTheme = when (theme) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            NutriScanTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                if (onboardingCompleted == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_preparing),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    val navController = rememberNavController()
                    val cameraViewModel: CameraViewModel = viewModel()
                    val scope = rememberCoroutineScope()
                    val startDestination = if (onboardingCompleted == true) {
                        Screen.Camera.route
                    } else {
                        Screen.Onboarding.route
                    }

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Onboarding.route) {
                                OnboardingScreen(
                                    onContinue = {
                                        scope.launch {
                                            settingsDataStore.setOnboardingCompleted(true)
                                            navController.navigate(Screen.Camera.route) {
                                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            composable(Screen.Camera.route) {
                                CameraScreen(
                                    navController = navController,
                                    viewModel = cameraViewModel,
                                    onNavigateToResult = { barcode ->
                                        navController.navigate(Screen.Result.createRoute(barcode))
                                    },
                                    onNavigateToNotFound = {
                                        navController.navigate(Screen.NotFound.route)
                                    }
                                )
                            }

                            composable(Screen.Result.route) { backStackEntry ->
                                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                                ResultScreen(
                                    productId = barcode,
                                    onNavigateBack = { navController.popBackStack() },
                                    onScanAgain = {
                                        cameraViewModel.resetToIdle()
                                        navController.navigate(Screen.Camera.route) {
                                            popUpTo(Screen.Camera.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.NotFound.route) {
                                NotFoundScreen(
                                    viewModel = cameraViewModel,
                                    onScanAgain = {
                                        cameraViewModel.resetToIdle()
                                        navController.navigate(Screen.Camera.route) {
                                            popUpTo(Screen.Camera.route) { inclusive = true }
                                        }
                                    },
                                    onManualSearch = {
                                        cameraViewModel.openPanel(ScannerPanel.MANUAL)
                                        navController.navigate(Screen.Camera.route) {
                                            popUpTo(Screen.Camera.route) { inclusive = true }
                                        }
                                    },
                                    onPhotoInput = {
                                        cameraViewModel.openPanel(ScannerPanel.PHOTO)
                                        navController.navigate(Screen.Camera.route) {
                                            popUpTo(Screen.Camera.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Screen.History.route) {
                                HistoryScreen(
                                    navController = navController,
                                    onProductClick = { barcode ->
                                        navController.navigate(Screen.Result.createRoute(barcode))
                                    }
                                )
                            }

                            composable(Screen.FirstAid.route) {
                                FirstAidScreen(navController = navController)
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }
}
