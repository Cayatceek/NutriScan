package com.nutriscan.app.ui.screens

import android.Manifest
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.nutriscan.app.BuildConfig
import com.nutriscan.app.R
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.localization.UiText
import com.nutriscan.app.localization.asString
import com.nutriscan.app.localization.asStringOrNull
import com.nutriscan.app.localization.localizedDemoScenarioLabel
import com.nutriscan.app.localization.uiText
import com.nutriscan.app.ui.components.BarcodeCameraPreview
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.EmptyStateCard
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.LoadingState
import com.nutriscan.app.ui.components.ModeChip
import com.nutriscan.app.ui.components.ScannerRuntime
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.CameraUiState
import com.nutriscan.app.viewmodel.CameraViewModel
import com.nutriscan.app.viewmodel.DemoScenario
import com.nutriscan.app.viewmodel.ScanState
import com.nutriscan.app.viewmodel.ScannerPanel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    onNavigateToResult: (String) -> Unit,
    onNavigateToNotFound: () -> Unit,
    viewModel: CameraViewModel,
    isTabVisible: Boolean = true,
    showBottomBar: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scannerRuntime = remember(viewModel) { viewModel.scannerRuntime }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var cameraErrorMessage by remember { mutableStateOf<UiText?>(null) }
    var scanLocked by rememberSaveable { mutableStateOf(false) }
    var lastDetectedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var lastDetectionTimestamp by rememberSaveable { mutableLongStateOf(0L) }
    var showScannerHelp by rememberSaveable { mutableStateOf(false) }
    val hasCameraPermission = cameraPermissionState.status is PermissionStatus.Granted
    val cameraSessionActive by remember(isTabVisible, hasCameraPermission, cameraErrorMessage) {
        derivedStateOf { isTabVisible && hasCameraPermission && cameraErrorMessage == null }
    }
    val scannerEnabled by remember(
        cameraSessionActive,
        uiState.activePanel,
        uiState.scanState
    ) {
        derivedStateOf {
            cameraSessionActive &&
                uiState.activePanel == ScannerPanel.NONE &&
                uiState.scanState == ScanState.Idle
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedPermission) {
            hasRequestedPermission = true
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.prepareScannerRuntime()
    }

    LaunchedEffect(viewModel, isTabVisible) {
        viewModel.onScannerVisibilityChanged(isTabVisible)
    }

    LaunchedEffect(uiState.scanState, uiState.activePanel) {
        if (uiState.scanState == ScanState.Idle && uiState.activePanel == ScannerPanel.NONE) {
            scanLocked = false
        }
    }

    LaunchedEffect(uiState.scanState, isTabVisible) {
        if (!isTabVisible) return@LaunchedEffect
        when (val state = uiState.scanState) {
            is ScanState.Success -> {
                onNavigateToResult(state.barcode)
                viewModel.resetToIdle()
            }

            ScanState.NotFound -> {
                onNavigateToNotFound()
                viewModel.resetToIdle()
            }

            else -> Unit
        }
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        CameraScreenContent(
            uiState = uiState,
            scannerRuntime = scannerRuntime,
            hasCameraPermission = hasCameraPermission,
            showPermissionDeniedState = cameraPermissionState.status is PermissionStatus.Denied && hasRequestedPermission,
            cameraErrorMessage = cameraErrorMessage,
            isCameraSessionActive = cameraSessionActive,
            isScannerEnabled = scannerEnabled,
            onRequestCameraPermission = {
                hasRequestedPermission = true
                cameraPermissionState.launchPermissionRequest()
            },
            onBarcodeDetected = { barcode ->
                val now = SystemClock.elapsedRealtime()
                if (
                    !scannerEnabled ||
                    !ScannerDetectionGate.shouldAcceptDetectedBarcode(
                        snapshot = ScannerDetectionSnapshot(
                            isLocked = scanLocked,
                            lastBarcode = lastDetectedBarcode,
                            lastDetectionTimestampMillis = lastDetectionTimestamp
                        ),
                        barcode = barcode,
                        nowMillis = now
                    )
                ) {
                    return@CameraScreenContent
                }
                scanLocked = true
                lastDetectedBarcode = barcode
                lastDetectionTimestamp = now
                cameraErrorMessage = null
                viewModel.onBarcodeDetected(barcode)
            },
            onScannerError = {
                cameraErrorMessage = uiText(R.string.scanner_camera_unavailable)
            },
            onOpenManualBarcode = { viewModel.openPanel(ScannerPanel.BARCODE) },
            onOpenManualSearch = { viewModel.openPanel(ScannerPanel.MANUAL) },
            onOpenPhotoInput = { viewModel.openPanel(ScannerPanel.PHOTO) },
            onBarcodeInputChange = viewModel::onBarcodeInputChange,
            onSubmitBarcode = viewModel::submitBarcodeInput,
            onManualSearchQueryChange = viewModel::onManualSearchQueryChange,
            onManualSearch = viewModel::searchManualProducts,
            onManualProductClick = viewModel::selectManualSearchResult,
            onDismissPanel = viewModel::closePanel,
            onRetryLookup = viewModel::retryLastLookup,
            onRetryScanner = { cameraErrorMessage = null },
            onOpenHelp = { showScannerHelp = true },
            onRunDemoScenario = viewModel::runDemoScenario,
            modifier = Modifier.padding(paddingValues)
        )
    }

    if (showScannerHelp) {
        ModalBottomSheet(onDismissRequest = { showScannerHelp = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiTokens.SpaceL),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                Text(
                    text = stringResource(R.string.scanner_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.scanner_help_tip_scan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text
                )
                Text(
                    text = stringResource(R.string.scanner_help_tip_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text
                )
                TextButton(
                    onClick = { showScannerHelp = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.got_it))
                }
                Spacer(modifier = Modifier.height(UiTokens.SpaceM))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CameraScreenContent(
    uiState: CameraUiState,
    scannerRuntime: ScannerRuntime,
    hasCameraPermission: Boolean,
    showPermissionDeniedState: Boolean,
    cameraErrorMessage: UiText?,
    isCameraSessionActive: Boolean,
    isScannerEnabled: Boolean,
    onRequestCameraPermission: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onScannerError: (Throwable) -> Unit,
    onOpenManualBarcode: () -> Unit,
    onOpenManualSearch: () -> Unit,
    onOpenPhotoInput: () -> Unit,
    onBarcodeInputChange: (String) -> Unit,
    onSubmitBarcode: () -> Unit,
    onManualSearchQueryChange: (String) -> Unit,
    onManualSearch: () -> Unit,
    onManualProductClick: (ProductEntity) -> Unit,
    onDismissPanel: () -> Unit,
    onRetryLookup: () -> Unit,
    onRetryScanner: () -> Unit,
    onOpenHelp: () -> Unit,
    onRunDemoScenario: (DemoScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ScannerPreviewArea(
            runtime = scannerRuntime,
            hasCameraPermission = hasCameraPermission,
            isCameraSessionActive = isCameraSessionActive,
            isScannerEnabled = isScannerEnabled,
            onBarcodeDetected = onBarcodeDetected,
            onScannerError = onScannerError
        )

        ScannerChrome(
            uiState = uiState,
            onOpenHelp = onOpenHelp,
            onRunDemoScenario = onRunDemoScenario,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (!hasCameraPermission || cameraErrorMessage != null) {
            ScannerCenterFallback(
                hasCameraPermission = hasCameraPermission,
                showPermissionDeniedState = showPermissionDeniedState,
                cameraErrorMessage = cameraErrorMessage,
                onRequestCameraPermission = onRequestCameraPermission,
                onRetryScanner = onRetryScanner,
                onOpenManualBarcode = onOpenManualBarcode,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            ScannerOverlay(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp)
            )
        }

        ScannerBottomDock(
            onOpenManualBarcode = onOpenManualBarcode,
            onOpenPhotoInput = onOpenPhotoInput,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = UiTokens.SpaceM, vertical = UiTokens.SpaceM)
        )

        when (uiState.scanState) {
            ScanState.Scanning, ScanState.Loading -> {
                ScannerLookupOverlay(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                )
            }

            is ScanState.Error -> {
                ErrorState(
                    title = stringResource(R.string.scanner_error_title),
                    message = uiState.errorMessage.asStringOrNull() ?: stringResource(R.string.error_generic),
                    actionLabel = stringResource(R.string.retry),
                    onAction = onRetryLookup,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f))
                )
            }

            else -> Unit
        }
    }

    if (uiState.activePanel != ScannerPanel.NONE) {
        ModalBottomSheet(onDismissRequest = onDismissPanel) {
            when (uiState.activePanel) {
                ScannerPanel.BARCODE -> {
                    ScannerEntrySheet(
                        title = stringResource(R.string.scanner_sheet_barcode_title),
                        description = stringResource(R.string.scanner_sheet_barcode_description),
                        value = uiState.barcodeInput,
                        onValueChange = onBarcodeInputChange,
                        onSubmit = onSubmitBarcode,
                        submitLabel = stringResource(R.string.scanner_sheet_barcode_submit),
                        secondaryActionLabel = stringResource(R.string.scanner_button_manual),
                        onSecondaryAction = {
                            onDismissPanel()
                            onOpenManualSearch()
                        }
                    )
                }

                ScannerPanel.MANUAL -> {
                    ManualSearchSheet(
                        query = uiState.manualSearchQuery,
                        results = uiState.manualSearchResults,
                        isLoading = uiState.isManualSearchLoading,
                        onQueryChange = onManualSearchQueryChange,
                        onSearch = onManualSearch,
                        onProductClick = onManualProductClick
                    )
                }

                ScannerPanel.PHOTO -> {
                    PhotoInputSheet(onOpenManualSearch = {
                        onDismissPanel()
                        onOpenManualSearch()
                    })
                }

                ScannerPanel.NONE -> Unit
            }
            Spacer(modifier = Modifier.height(UiTokens.SpaceM))
        }
    }
}

@Composable
private fun ScannerPreviewArea(
    runtime: ScannerRuntime,
    hasCameraPermission: Boolean,
    isCameraSessionActive: Boolean,
    isScannerEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onScannerError: (Throwable) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF111827),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            BarcodeCameraPreview(
                runtime = runtime,
                modifier = Modifier.fillMaxSize(),
                cameraActive = isCameraSessionActive,
                scanningEnabled = isScannerEnabled,
                onBarcodeDetected = onBarcodeDetected,
                onScannerError = onScannerError
            )
        }
    }
}

@Composable
private fun ScannerChrome(
    uiState: CameraUiState,
    onOpenHelp: () -> Unit,
    onRunDemoScenario: (DemoScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.54f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = UiTokens.SpaceM, vertical = UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    onClick = onOpenHelp,
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    shadowElevation = 8.dp
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = stringResource(R.string.scanner_help_title),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                DemoTray(
                    scenarios = uiState.demoScenarios,
                    activeScenarioId = uiState.activeDemoScenarioId,
                    onScenarioClick = onRunDemoScenario
                )
            }
        }
    }
}

@Composable
private fun ScannerLookupOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = UiTokens.CardElevationMedium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ScannerOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(168.dp)
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.64f),
                    shape = RoundedCornerShape(28.dp)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
        )
        Text(
            text = stringResource(R.string.scanner_overlay_hint),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = UiTokens.SpaceS)
                .background(
                    color = Color.Black.copy(alpha = 0.34f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ScannerCenterFallback(
    hasCameraPermission: Boolean,
    showPermissionDeniedState: Boolean,
    cameraErrorMessage: UiText?,
    onRequestCameraPermission: () -> Unit,
    onRetryScanner: () -> Unit,
    onOpenManualBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 24.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = if (!hasCameraPermission && showPermissionDeniedState) {
                    stringResource(R.string.scanner_camera_permission_denied_title)
                } else if (cameraErrorMessage != null) {
                    stringResource(R.string.scanner_camera_error_title)
                } else {
                    stringResource(R.string.scanner_hero_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = when {
                    !hasCameraPermission && showPermissionDeniedState -> {
                        stringResource(R.string.scanner_camera_permission_denied_message)
                    }

                    cameraErrorMessage != null -> cameraErrorMessage.asString()
                    else -> stringResource(R.string.scanner_hero_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = if (!hasCameraPermission) onRequestCameraPermission else onRetryScanner,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiTokens.ButtonHeight),
                shape = RoundedCornerShape(UiTokens.Radius)
            ) {
                Text(
                    if (!hasCameraPermission) {
                        stringResource(R.string.scanner_camera_permission_action)
                    } else {
                        stringResource(R.string.retry)
                    }
                )
            }
            OutlinedButton(
                onClick = onOpenManualBarcode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiTokens.ButtonHeight),
                shape = RoundedCornerShape(UiTokens.Radius)
            ) {
                Text(stringResource(R.string.scanner_button_enter_barcode))
            }
        }
    }
}

@Composable
private fun ScannerBottomDock(
    onOpenManualBarcode: () -> Unit,
    onOpenPhotoInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                Button(
                    onClick = onOpenManualBarcode,
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                    Text(stringResource(R.string.scanner_button_enter_barcode))
                }
                OutlinedButton(
                    onClick = onOpenPhotoInput,
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.ImageSearch, contentDescription = null)
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                    Text(stringResource(R.string.scanner_button_photo))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoTray(
    scenarios: List<DemoScenario>,
    activeScenarioId: String?,
    onScenarioClick: (DemoScenario) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.scanner_demo_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(R.string.scanner_demo_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = muted_text
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
            ) {
                scenarios.forEach { scenario ->
                    ModeChip(
                        label = localizedDemoScenarioLabel(scenario.id),
                        selected = activeScenarioId == scenario.id,
                        onClick = { onScenarioClick(scenario) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerEntrySheet(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    submitLabel: String,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = muted_text
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.barcode_label)) },
            singleLine = true,
            shape = RoundedCornerShape(UiTokens.Radius)
        )
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.ButtonHeight),
            shape = RoundedCornerShape(UiTokens.Radius)
        ) {
            Text(submitLabel)
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(secondaryActionLabel)
            }
        }
    }
}

@Composable
private fun ManualSearchSheet(
    query: String,
    results: List<ProductEntity>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onProductClick: (ProductEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
    ) {
        Text(
            text = stringResource(R.string.scanner_sheet_manual_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.scanner_sheet_manual_description),
            style = MaterialTheme.typography.bodyMedium,
            color = muted_text
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.scanner_sheet_manual_query_label)) },
            singleLine = true,
            shape = RoundedCornerShape(UiTokens.Radius)
        )
        Button(
            onClick = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.ButtonHeight),
            shape = RoundedCornerShape(UiTokens.Radius)
        ) {
            Text(stringResource(R.string.scanner_sheet_manual_search_button))
        }
        when {
            isLoading -> {
                    LoadingState(
                    title = stringResource(R.string.scanner_sheet_manual_loading_title),
                    message = stringResource(R.string.scanner_sheet_manual_loading_message)
                )
            }

            !isLoading && results.isEmpty() && query.length >= 2 -> {
                EmptyStateCard(
                    title = stringResource(R.string.scanner_sheet_manual_empty_title),
                    message = stringResource(R.string.scanner_sheet_manual_empty_message),
                    icon = Icons.Outlined.Search
                )
            }

            else -> {
                results.forEach { product ->
                    ProductSearchResultCard(
                        product = product,
                        onClick = { onProductClick(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoInputSheet(
    onOpenManualSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiTokens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
    ) {
        Text(
            text = stringResource(R.string.scanner_sheet_photo_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.scanner_sheet_photo_description),
            style = MaterialTheme.typography.bodyMedium,
            color = muted_text
        )
        EmptyStateCard(
            title = stringResource(R.string.scanner_sheet_photo_empty_title),
            message = stringResource(R.string.scanner_sheet_photo_empty_message),
            icon = Icons.Outlined.BrokenImage,
            actionLabel = stringResource(R.string.scanner_sheet_photo_empty_action),
            onAction = onOpenManualSearch
        )
    }
}

@Composable
private fun ProductSearchResultCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = product.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
            product.barcode?.let { barcode ->
                Text(
                    text = stringResource(R.string.barcode_value, barcode),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted_text
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraScreenPreview() {
    NutriScanTheme {
        CameraScreenContent(
            uiState = CameraUiState(),
            scannerRuntime = ScannerRuntime(androidx.compose.ui.platform.LocalContext.current.applicationContext),
            hasCameraPermission = true,
            showPermissionDeniedState = false,
            cameraErrorMessage = null,
            isCameraSessionActive = true,
            isScannerEnabled = true,
            onRequestCameraPermission = {},
            onBarcodeDetected = {},
            onScannerError = {},
            onOpenManualBarcode = {},
            onOpenManualSearch = {},
            onOpenPhotoInput = {},
            onBarcodeInputChange = {},
            onSubmitBarcode = {},
            onManualSearchQueryChange = {},
            onManualSearch = {},
            onManualProductClick = {},
            onDismissPanel = {},
            onRetryLookup = {},
            onRetryScanner = {},
            onOpenHelp = {},
            onRunDemoScenario = {}
        )
    }
}
