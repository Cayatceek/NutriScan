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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.R
import com.nutriscan.app.localization.asStringOrNull
import com.nutriscan.app.localization.localizedStatusLabel
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.EmptyStateCard
import com.nutriscan.app.ui.components.InlineErrorCard
import com.nutriscan.app.ui.components.PlaceholderBlock
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.components.statusColors
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.HistoryFilter
import com.nutriscan.app.viewmodel.HistoryRecordUiState
import com.nutriscan.app.viewmodel.HistoryViewModel
import com.nutriscan.app.viewmodel.HistoryViewModelFactory

@Composable
fun HistoryScreen(
    navController: NavController,
    onProductClick: (String) -> Unit,
    showBottomBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(context.applicationContext as android.app.Application)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = UiTokens.SpaceM, vertical = UiTokens.SpaceL),
            contentPadding = PaddingValues(bottom = UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.history_title),
                    subtitle = stringResource(R.string.history_subtitle)
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::searchHistory,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                    singleLine = true,
                    enabled = !uiState.isLoading
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)) {
                    FilterChip(
                        selected = uiState.selectedFilter == HistoryFilter.ALL,
                        onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                        label = { Text(stringResource(R.string.history_filter_all)) },
                        enabled = !uiState.isLoading
                    )
                    FilterChip(
                        selected = uiState.selectedFilter == HistoryFilter.FAVORITES,
                        onClick = { viewModel.setFilter(HistoryFilter.FAVORITES) },
                        label = { Text(stringResource(R.string.history_filter_favorites)) },
                        enabled = !uiState.isLoading
                    )
                }
            }
            when {
                uiState.isLoading -> {
                    items(4) {
                        HistoryItemSkeleton()
                    }
                }

                uiState.error != null -> {
                    item {
                        InlineErrorCard(
                            title = stringResource(R.string.history_error_title),
                            message = uiState.error.asStringOrNull()
                                ?: stringResource(R.string.history_error_message),
                            actionLabel = stringResource(R.string.retry),
                            onAction = viewModel::retryLoad
                        )
                    }
                }

                uiState.items.isEmpty() -> {
                    item {
                        EmptyHistoryState()
                    }
                }

                else -> {
                    items(uiState.items, key = { it.id }) { item ->
                        HistoryRecordCard(
                            item = item,
                            onClick = { onProductClick(item.productId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    EmptyStateCard(
        title = stringResource(R.string.history_empty_title),
        message = stringResource(R.string.history_empty_message),
        icon = Icons.Outlined.History
    )
}

@Composable
private fun HistoryItemSkeleton() {
    Card(
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
                PlaceholderBlock(
                    modifier = Modifier.size(UiTokens.SpaceS),
                    shape = RoundedCornerShape(999.dp)
                )
                PlaceholderBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.28f)
                        .height(12.dp)
                )
            }
            PlaceholderBlock(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(18.dp)
            )
            PlaceholderBlock(
                modifier = Modifier
                    .fillMaxWidth(0.36f)
                    .height(12.dp)
            )
            PlaceholderBlock(
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .height(12.dp)
            )
            PlaceholderBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(UiTokens.Radius)
            )
        }
    }
}

@Composable
private fun HistoryRecordCard(
    item: HistoryRecordUiState,
    onClick: () -> Unit
) {
    val (containerColor, contentColor) = statusColors(item.status)

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
                    text = localizedStatusLabel(item.status),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                if (item.isFavorite) {
                    Text(
                        text = stringResource(R.string.history_badge_favorite),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = item.productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
            Text(
                text = stringResource(R.string.history_scanned_at, item.scannedAtLabel),
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
                        text = localizedStatusLabel(item.status),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
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
