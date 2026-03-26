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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.nutriscan.app.BuildConfig
import com.nutriscan.app.R
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.localization.localizedDemoScenarioLabel
import com.nutriscan.app.ui.components.BarcodeCameraPreview
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.EmptyStateCard
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.LoadingState
import com.nutriscan.app.ui.components.ModeChip
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.CameraUiState
import com.nutriscan.app.viewmodel.CameraViewModel
import com.nutriscan.app.viewmodel.DemoScenario
import com.nutriscan.app.viewmodel.ScanState
import com.nutriscan.app.viewmodel.ScannerPanel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    navController: NavController,
    onNavigateToResult: (String) -> Unit,
    onNavigateToNotFound: () -> Unit,
    viewModel: CameraViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var cameraErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var scanLocked by rememberSaveable { mutableStateOf(false) }
    var lastDetectedBarcode by rememberSaveable { mutableStateOf<String?>(null) }
    var lastDetectionTimestamp by rememberSaveable { mutableLongStateOf(0L) }
    val hasCameraPermission = cameraPermissionState.status is PermissionStatus.Granted
    val scannerEnabled = hasCameraPermission &&
        uiState.activePanel == ScannerPanel.NONE &&
        uiState.scanState == ScanState.Idle

    LaunchedEffect(Unit) {
        if (!hasRequestedPermission) {
            hasRequestedPermission = true
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState.scanState, uiState.activePanel) {
        if (uiState.scanState == ScanState.Idle && uiState.activePanel == ScannerPanel.NONE) {
            scanLocked = false
        }
    }

    LaunchedEffect(uiState.scanState) {
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        CameraScreenContent(
            uiState = uiState,
            hasCameraPermission = hasCameraPermission,
            showPermissionDeniedState = cameraPermissionState.status is PermissionStatus.Denied && hasRequestedPermission,
            cameraErrorMessage = cameraErrorMessage,
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
                cameraErrorMessage = context.getString(R.string.scanner_camera_unavailable)
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
            onRunDemoScenario = viewModel::runDemoScenario,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CameraScreenContent(
    uiState: CameraUiState,
    hasCameraPermission: Boolean,
    showPermissionDeniedState: Boolean,
    cameraErrorMessage: String?,
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
    onRunDemoScenario: (DemoScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = UiTokens.SpaceM, vertical = UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.scanner_title),
                    subtitle = stringResource(R.string.scanner_subtitle)
                )
            }

            if (BuildConfig.DEBUG) {
                item {
                    DemoTray(
                        scenarios = uiState.demoScenarios,
                        activeScenarioId = uiState.activeDemoScenarioId,
                        onScenarioClick = onRunDemoScenario
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, card_stroke),
                    elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
                    ) {
                        ScannerPreviewArea(
                            hasCameraPermission = hasCameraPermission,
                            showPermissionDeniedState = showPermissionDeniedState,
                            cameraErrorMessage = cameraErrorMessage,
                            isScannerEnabled = isScannerEnabled,
                            onRequestCameraPermission = onRequestCameraPermission,
                            onRetryScanner = onRetryScanner,
                            onBarcodeDetected = onBarcodeDetected,
                            onScannerError = onScannerError,
                            onOpenManualBarcode = onOpenManualBarcode
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS),
                            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
                        ) {
                            ModeChip(
                                label = if (uiState.halalModeEnabled) {
                                    stringResource(R.string.scanner_chip_halal_on)
                                } else {
                                    stringResource(R.string.scanner_chip_halal)
                                },
                                selected = uiState.halalModeEnabled,
                                onClick = {}
                            )
                            ModeChip(
                                label = if (uiState.selectedAllergensCount > 0) {
                                    stringResource(
                                        R.string.scanner_chip_allergies_count,
                                        uiState.selectedAllergensCount
                                    )
                                } else {
                                    stringResource(R.string.scanner_chip_allergies)
                                },
                                selected = uiState.selectedAllergensCount > 0,
                                onClick = {}
                            )
                            ModeChip(
                                label = if (uiState.healthyModeEnabled) {
                                    stringResource(R.string.scanner_chip_healthy_on)
                                } else {
                                    stringResource(R.string.scanner_chip_healthy)
                                },
                                selected = uiState.healthyModeEnabled,
                                onClick = {}
                            )
                        }

                        if (!hasCameraPermission) {
                            Button(
                                onClick = onRequestCameraPermission,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(UiTokens.ButtonHeight),
                                shape = RoundedCornerShape(UiTokens.Radius)
                            ) {
                                Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                                Text(stringResource(R.string.scanner_camera_permission_action))
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.scanner_live_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = muted_text,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
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

                        Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)) {
                            OutlinedButton(
                                onClick = onOpenManualSearch,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(UiTokens.ButtonHeight),
                                shape = RoundedCornerShape(UiTokens.Radius)
                            ) {
                                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                                Text(stringResource(R.string.scanner_button_manual))
                            }
                            OutlinedButton(
                                onClick = onOpenPhotoInput,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(UiTokens.ButtonHeight),
                                shape = RoundedCornerShape(UiTokens.Radius)
                            ) {
                                Icon(imageVector = Icons.Outlined.ImageSearch, contentDescription = null)
                                Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                                Text(stringResource(R.string.scanner_button_photo))
                            }
                        }
                    }
                }
            }
        }

        when (uiState.scanState) {
            ScanState.Scanning, ScanState.Loading -> {
                LoadingState(
                    title = stringResource(R.string.scanner_loading_title),
                    message = stringResource(R.string.scanner_loading_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
                )
            }

            is ScanState.Error -> {
                ErrorState(
                    title = stringResource(R.string.scanner_error_title),
                    message = uiState.errorMessage ?: stringResource(R.string.error_generic),
                    actionLabel = stringResource(R.string.retry),
                    onAction = onRetryLookup,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
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
                        submitLabel = stringResource(R.string.scanner_sheet_barcode_submit)
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
    hasCameraPermission: Boolean,
    showPermissionDeniedState: Boolean,
    cameraErrorMessage: String?,
    isScannerEnabled: Boolean,
    onRequestCameraPermission: () -> Unit,
    onRetryScanner: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onScannerError: (Throwable) -> Unit,
    onOpenManualBarcode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(284.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            !hasCameraPermission -> {
                ScannerFallbackContent(
                    title = if (showPermissionDeniedState) {
                        stringResource(R.string.scanner_camera_permission_denied_title)
                    } else {
                        stringResource(R.string.scanner_hero_title)
                    },
                    message = if (showPermissionDeniedState) {
                        stringResource(R.string.scanner_camera_permission_denied_message)
                    } else {
                        stringResource(R.string.scanner_hero_subtitle)
                    },
                    primaryLabel = stringResource(R.string.scanner_camera_permission_action),
                    onPrimaryAction = onRequestCameraPermission,
                    secondaryLabel = stringResource(R.string.scanner_button_enter_barcode),
                    onSecondaryAction = onOpenManualBarcode
                )
            }

            cameraErrorMessage != null -> {
                ScannerFallbackContent(
                    title = stringResource(R.string.scanner_camera_error_title),
                    message = cameraErrorMessage,
                    primaryLabel = stringResource(R.string.retry),
                    onPrimaryAction = onRetryScanner,
                    secondaryLabel = stringResource(R.string.scanner_button_enter_barcode),
                    onSecondaryAction = onOpenManualBarcode
                )
            }

            else -> {
                BarcodeCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    enabled = isScannerEnabled,
                    onBarcodeDetected = onBarcodeDetected,
                    onScannerError = onScannerError
                )
                ScannerOverlay()
            }
        }
    }
}

@Composable
private fun ScannerOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(156.dp)
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp)
                )
        )
        Text(
            text = stringResource(R.string.scanner_overlay_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = UiTokens.SpaceS)
                .background(
                    color = Color.Black.copy(alpha = 0.36f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ScannerFallbackContent(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryLabel: String,
    onSecondaryAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS),
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = muted_text,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.ButtonHeight),
            shape = RoundedCornerShape(UiTokens.Radius)
        ) {
            Text(primaryLabel)
        }
        OutlinedButton(
            onClick = onSecondaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.ButtonHeight),
            shape = RoundedCornerShape(UiTokens.Radius)
        ) {
            Text(secondaryLabel)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
    submitLabel: String
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
            hasCameraPermission = true,
            showPermissionDeniedState = false,
            cameraErrorMessage = null,
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
            onRunDemoScenario = {}
        )
    }
}
