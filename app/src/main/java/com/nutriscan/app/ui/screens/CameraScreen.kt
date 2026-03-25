package com.nutriscan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.LoadingState
import com.nutriscan.app.ui.components.ModeChip
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.CameraUiState
import com.nutriscan.app.viewmodel.CameraViewModel
import com.nutriscan.app.viewmodel.ScanState
import com.nutriscan.app.viewmodel.ScannerPanel

@Composable
fun CameraScreen(
    navController: NavController,
    onNavigateToResult: (String) -> Unit,
    onNavigateToNotFound: () -> Unit,
    viewModel: CameraViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.scanState) {
        when (val state = uiState.scanState) {
            is ScanState.Success -> {
                onNavigateToResult(state.barcode)
                viewModel.resetToIdle()
            }
            ScanState.NotFound -> {
                onNavigateToNotFound()
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
            onOpenBarcode = { viewModel.openPanel(ScannerPanel.BARCODE) },
            onOpenManualSearch = { viewModel.openPanel(ScannerPanel.MANUAL) },
            onOpenPhotoInput = { viewModel.openPanel(ScannerPanel.PHOTO) },
            onBarcodeInputChange = viewModel::onBarcodeInputChange,
            onSubmitBarcode = viewModel::submitBarcodeInput,
            onManualSearchQueryChange = viewModel::onManualSearchQueryChange,
            onManualSearch = viewModel::searchManualProducts,
            onManualProductClick = viewModel::selectManualSearchResult,
            onDismissPanel = viewModel::closePanel,
            onDismissError = viewModel::clearError,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CameraScreenContent(
    uiState: CameraUiState,
    onOpenBarcode: () -> Unit,
    onOpenManualSearch: () -> Unit,
    onOpenPhotoInput: () -> Unit,
    onBarcodeInputChange: (String) -> Unit,
    onSubmitBarcode: () -> Unit,
    onManualSearchQueryChange: (String) -> Unit,
    onManualSearch: () -> Unit,
    onManualProductClick: (ProductEntity) -> Unit,
    onDismissPanel: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SectionTitle(
                    title = "Scanner",
                    subtitle = "Use barcode first for the fastest and most reliable product lookup."
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, card_stroke),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(292.dp)
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "Scan product",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Barcode-first by design. Use manual search when the code is missing and photo input as a fallback handoff.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = muted_text,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ModeChip(
                                label = if (uiState.halalModeEnabled) "Halal on" else "Halal",
                                selected = uiState.halalModeEnabled,
                                onClick = {}
                            )
                            ModeChip(
                                label = if (uiState.selectedAllergensCount > 0) {
                                    "Allergies ${uiState.selectedAllergensCount}"
                                } else {
                                    "Allergies"
                                },
                                selected = uiState.selectedAllergensCount > 0,
                                onClick = {}
                            )
                            ModeChip(
                                label = if (uiState.healthyModeEnabled) "Healthy on" else "Healthy mode",
                                selected = uiState.healthyModeEnabled,
                                onClick = {}
                            )
                        }

                        Button(
                            onClick = onOpenBarcode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.size(10.dp))
                            Text("Scan barcode")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onOpenManualSearch,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Manual search")
                            }
                            OutlinedButton(
                                onClick = onOpenPhotoInput,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ImageSearch, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Photo input")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, card_stroke),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Fast decision flow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Barcode search opens the result directly when we know the product. If not, NutriScan keeps the response honest and sends you to manual options.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted_text
                        )
                    }
                }
            }
        }

        when (uiState.scanState) {
            ScanState.Scanning, ScanState.Loading -> {
                LoadingState(
                    title = "Analyzing product",
                    message = "Checking the barcode and preparing the safety summary.",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
                )
            }
            is ScanState.Error -> {
                ErrorState(
                    title = "Unable to continue",
                    message = uiState.errorMessage ?: "Something went wrong while looking up this product.",
                    actionLabel = "Dismiss",
                    onAction = onDismissError,
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
                        title = "Enter barcode",
                        description = "Paste or type the barcode to continue with the barcode-first flow.",
                        value = uiState.barcodeInput,
                        onValueChange = onBarcodeInputChange,
                        onSubmit = onSubmitBarcode,
                        submitLabel = "Find product"
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
            Spacer(modifier = Modifier.height(24.dp))
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
            label = { Text("Barcode") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Manual search",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Search by product or brand when the barcode is missing or unreadable.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted_text
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search products") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Button(
            onClick = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Search")
        }
        if (isLoading) {
            Text(
                text = "Searching products...",
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
        }
        if (!isLoading && results.isEmpty() && query.length >= 2) {
            Text(
                text = "No matching products yet. Try another name or return to barcode search.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
        }
        results.forEach { product ->
            ProductSearchResultCard(
                product = product,
                onClick = { onProductClick(product) }
            )
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Photo input",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Photo-based ingredient review is not connected in this MVP build yet. For reliable results, continue with barcode or manual search.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted_text
        )
        OutlinedButton(
            onClick = onOpenManualSearch,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Use manual search")
        }
    }
}

@Composable
private fun ProductSearchResultCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    text = "Barcode $barcode",
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
            onOpenBarcode = {},
            onOpenManualSearch = {},
            onOpenPhotoInput = {},
            onBarcodeInputChange = {},
            onSubmitBarcode = {},
            onManualSearchQueryChange = {},
            onManualSearch = {},
            onManualProductClick = {},
            onDismissPanel = {},
            onDismissError = {}
        )
    }
}
