package com.nutriscan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nutriscan.app.R
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text

private data class AdditiveEntry(
    val code: String,
    val titleRes: Int,
    val bodyRes: Int
)

@Composable
fun AdditivesEncyclopediaScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        AdditiveEntry("E100", R.string.additives_item_1_title, R.string.additives_item_1_body),
        AdditiveEntry("E202", R.string.additives_item_2_title, R.string.additives_item_2_body),
        AdditiveEntry("E621", R.string.additives_item_3_title, R.string.additives_item_3_body),
        AdditiveEntry("E950", R.string.additives_item_4_title, R.string.additives_item_4_body)
    )

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = UiTokens.SpaceM, vertical = UiTokens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
        ) {
            item {
                SectionTitle(
                    title = stringResource(R.string.additives_title),
                    subtitle = stringResource(R.string.additives_subtitle)
                )
            }
            items(entries) { entry ->
                Card(
                    shape = RoundedCornerShape(UiTokens.LargeRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, card_stroke),
                    elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(UiTokens.SpaceM),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entry.code,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(entry.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(entry.bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted_text
                        )
                    }
                }
            }
        }
    }
}
