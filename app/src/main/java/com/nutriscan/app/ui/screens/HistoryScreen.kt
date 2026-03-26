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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.R
import com.nutriscan.app.localization.localizedStatusLabel
import com.nutriscan.app.data.local.SettingsDataStore
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.data.local.entity.ScanHistoryWithProduct
import com.nutriscan.app.domain.analysis.AnalysisEngine
import com.nutriscan.app.domain.analysis.AnalysisSettings
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.model.SafetyStatus
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.EmptyStateCard
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.LoadingState
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.components.statusColors
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
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
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    title = stringResource(R.string.history_loading_title),
                    message = stringResource(R.string.history_loading_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    title = stringResource(R.string.history_error_title),
                    message = uiState.error ?: stringResource(R.string.history_error_message),
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
                        .padding(horizontal = UiTokens.SpaceM, vertical = UiTokens.SpaceL)
                ) {
                    SectionTitle(
                        title = stringResource(R.string.history_title),
                        subtitle = stringResource(R.string.history_subtitle)
                    )
                    Spacer(modifier = Modifier.height(UiTokens.SpaceM))

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::searchHistory,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(UiTokens.Radius),
                        placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(UiTokens.SpaceS))

                    Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)) {
                        FilterChip(
                            selected = uiState.selectedFilter == HistoryFilter.ALL,
                            onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                            label = { Text(stringResource(R.string.history_filter_all)) }
                        )
                        FilterChip(
                            selected = uiState.selectedFilter == HistoryFilter.FAVORITES,
                            onClick = { viewModel.setFilter(HistoryFilter.FAVORITES) },
                            label = { Text(stringResource(R.string.history_filter_favorites)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(UiTokens.SpaceM))

                    if (filteredHistory.isEmpty()) {
                        EmptyHistoryState()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = UiTokens.SpaceM),
                            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
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
        EmptyStateCard(
            title = stringResource(R.string.history_empty_title),
            message = stringResource(R.string.history_empty_message),
            icon = Icons.Outlined.History
        )
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
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
            ) {
                Box(
                    modifier = Modifier
                        .size(UiTokens.SpaceS)
                        .background(contentColor, CircleShape)
                )
                Text(
                    text = localizedStatusLabel(status),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                if (item.product.isFavorite) {
                    Text(
                        text = stringResource(R.string.history_badge_favorite),
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
                text = stringResource(R.string.history_scanned_at, date),
                style = MaterialTheme.typography.bodySmall,
                color = muted_text
            )
            Card(
                shape = RoundedCornerShape(UiTokens.Radius),
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(UiTokens.SpaceS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_latest_verdict),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                    Text(
                        text = localizedStatusLabel(status),
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
