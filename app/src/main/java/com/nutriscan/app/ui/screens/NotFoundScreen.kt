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
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nutriscan.app.data.local.entity.ProductEntity
import com.nutriscan.app.ui.components.SectionTitle
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, card_stroke)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Product not found",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try scanning again, search manually, or continue with photo input when barcode lookup is not enough.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = muted_text
                    )
                    Button(
                        onClick = onScanAgain,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Scan again")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onManualSearch,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manual search")
                        }
                        OutlinedButton(
                            onClick = onPhotoInput,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ImageSearch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Photo input")
                        }
                    }
                }
            }
        }

        if (uiState.alternatives.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Alternatives",
                    subtitle = "Here are a few products you can review right away while you try another scan."
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
