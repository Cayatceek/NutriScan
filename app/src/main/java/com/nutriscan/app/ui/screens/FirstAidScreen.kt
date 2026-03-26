package com.nutriscan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.R
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text

@Composable
fun FirstAidScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomNavigationBar(navController = navController) }
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
                    title = stringResource(R.string.first_aid_title),
                    subtitle = stringResource(R.string.first_aid_subtitle)
                )
            }
            item {
                FirstAidCard(
                    title = stringResource(R.string.first_aid_emergency_title),
                    body = stringResource(R.string.first_aid_emergency_body),
                    icon = Icons.Outlined.Emergency
                )
            }
            item {
                FirstAidCard(
                    title = stringResource(R.string.first_aid_symptoms_title),
                    body = stringResource(R.string.first_aid_symptoms_body),
                    icon = Icons.Outlined.HealthAndSafety
                )
            }
            item {
                FirstAidCard(
                    title = stringResource(R.string.first_aid_urgent_title),
                    body = stringResource(R.string.first_aid_urgent_body),
                    icon = Icons.Outlined.ErrorOutline
                )
            }
            item {
                FirstAidCard(
                    title = stringResource(R.string.first_aid_additives_title),
                    body = stringResource(R.string.first_aid_additives_body),
                    icon = Icons.Outlined.AutoAwesome
                )
            }
        }
    }
}

@Composable
private fun FirstAidCard(
    title: String,
    body: String,
    icon: ImageVector
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
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UiTokens.SpaceL)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FirstAidScreenPreview() {
    NutriScanTheme {
        FirstAidScreen(navController = rememberNavController())
    }
}
