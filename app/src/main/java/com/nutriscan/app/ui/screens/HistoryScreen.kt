package com.nutriscan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.local.entity.ScanHistoryWithProduct
import com.nutriscan.app.domain.analysis.AnalysisEngine
import com.nutriscan.app.domain.analysis.AnalysisSettings
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.model.SafetyStatus
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.LoadingState
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.components.statusColors
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.HistoryFilter
import com.nutriscan.app.viewmodel.HistoryViewModel
import com.nutriscan.app.viewmodel.HistoryViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    navController: NavController,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(context.applicationContext as android.app.Application)
    )
    val uiState by viewModel.uiState.collectAsState()
    val filteredHistory = viewModel.getFilteredHistory()
    val settingsDataStore = remember(context.applicationContext) {
        SettingsDataStore(context.applicationContext)
    }
    val selectedAllergens by settingsDataStore.selectedAllergens.collectAsState(initial = emptySet())
    val halalModeEnabled by settingsDataStore.halalMode.collectAsState(initial = false)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    title = "Loading history",
                    message = "Preparing your most recent product checks.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            uiState.error != null -> {
                ErrorState(
                    title = "History unavailable",
                    message = uiState.error ?: "We could not load your saved scans.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    SectionTitle(
                        title = "History",
                        subtitle = "Review scanned products and filter quickly by favorites."
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::searchHistory,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Search scans") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = uiState.selectedFilter == HistoryFilter.ALL,
                            onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = uiState.selectedFilter == HistoryFilter.FAVORITES,
                            onClick = { viewModel.setFilter(HistoryFilter.FAVORITES) },
                            label = { Text("Favorites") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredHistory.isEmpty()) {
                        EmptyHistoryState()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredHistory, key = { it.scanHistory.id }) { item ->
                                val status = historyStatus(
                                    product = item.product,
                                    selectedAllergens = selectedAllergens,
                                    halalModeEnabled = halalModeEnabled
                                )
                                HistoryRecordCard(
                                    item = item,
                                    status = status,
                                    onClick = { onProductClick(item.product.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, card_stroke)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "No scans yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Scanned products will appear here once you start using NutriScan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text
                )
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    item: ScanHistoryWithProduct,
    status: SafetyStatus,
    onClick: () -> Unit
) {
    val (containerColor, contentColor) = statusColors(status)
    val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.scanHistory.scannedAt))

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(contentColor, CircleShape)
                )
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                if (item.product.isFavorite) {
                    Text(
                        text = "Favorite",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.product.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
            Text(
                text = "Scanned $date",
                style = MaterialTheme.typography.bodySmall,
                color = muted_text
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latest safety status",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun historyStatus(
    product: ProductEntity,
    selectedAllergens: Set<Allergen>,
    halalModeEnabled: Boolean
): SafetyStatus {
    val mappedAllergens = selectedAllergens.mapNotNull { allergen ->
        when (allergen) {
            Allergen.MILK -> "dairy"
            Allergen.GLUTEN -> "gluten"
            Allergen.PEANUTS, Allergen.TREE_NUTS -> "nuts"
            else -> null
        }
    }.toSet()

    return AnalysisEngine.analyze(
        ingredients = product.ingredients.map { it.name },
        settings = AnalysisSettings(
            selectedAllergens = mappedAllergens,
            halalModeEnabled = halalModeEnabled
        )
    ).safetyStatus
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    NutriScanTheme {
        HistoryScreen(
            navController = rememberNavController(),
            onProductClick = {}
        )
    }
}
