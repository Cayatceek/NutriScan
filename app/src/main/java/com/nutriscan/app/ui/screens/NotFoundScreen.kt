package com.nutriscan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nutriscan.app.R
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.CameraViewModel

@Composable
fun NotFoundScreen(
    viewModel: CameraViewModel,
    onScanAgain: () -> Unit,
    onManualSearch: () -> Unit,
    onPhotoInput: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = UiTokens.SpaceM, vertical = UiTokens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(UiTokens.LargeRadius),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, card_stroke),
                elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
            ) {
                Column(
                    modifier = Modifier.padding(UiTokens.SpaceL),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.not_found_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.not_found_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = muted_text
                    )
                    Button(
                        onClick = onScanAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(UiTokens.ButtonHeight),
                        shape = RoundedCornerShape(UiTokens.Radius)
                    ) {
                        Text(stringResource(R.string.result_action_scan_again))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)) {
                        OutlinedButton(
                            onClick = onManualSearch,
                            modifier = Modifier
                                .weight(1f)
                                .height(UiTokens.ButtonHeight),
                            shape = RoundedCornerShape(UiTokens.Radius)
                        ) {
                            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                            Text(stringResource(R.string.scanner_sheet_manual_title))
                        }
                        OutlinedButton(
                            onClick = onPhotoInput,
                            modifier = Modifier
                                .weight(1f)
                                .height(UiTokens.ButtonHeight),
                            shape = RoundedCornerShape(UiTokens.Radius)
                        ) {
                            Icon(imageVector = Icons.Outlined.ImageSearch, contentDescription = null)
                            Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                            Text(stringResource(R.string.scanner_sheet_photo_title))
                        }
                    }
                }
            }
        }

        if (uiState.alternatives.isNotEmpty()) {
            item {
                SectionTitle(
                    title = stringResource(R.string.result_alternatives_title),
                    subtitle = stringResource(R.string.not_found_alternatives_subtitle)
                )
            }
            items(uiState.alternatives) { product ->
                AlternativeSuggestionCard(product = product)
            }
        }
    }
}

@Composable
private fun AlternativeSuggestionCard(
    product: ProductEntity
) {
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
