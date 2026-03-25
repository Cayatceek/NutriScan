package com.nutriscan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.theme.NutriScanTheme
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionTitle(
                    title = "First Aid",
                    subtitle = "Quick steps for allergy symptoms, urgent reactions, and additive awareness."
                )
            }
            item {
                FirstAidCard(
                    title = "Emergency steps",
                    body = "Stop eating immediately, check breathing, use prescribed emergency medication if you have it, and call emergency services when symptoms escalate quickly."
                )
            }
            item {
                FirstAidCard(
                    title = "Common symptoms",
                    body = "Watch for itching, swelling, hives, stomach pain, wheezing, vomiting, throat tightness, or sudden weakness after eating."
                )
            }
            item {
                FirstAidCard(
                    title = "When it is urgent",
                    body = "Treat throat swelling, shortness of breath, fainting, confusion, and rapidly spreading reactions as medical emergencies."
                )
            }
            item {
                FirstAidCard(
                    title = "Additives encyclopedia",
                    body = "E102 is a coloring additive and E621 is a flavor enhancer. Sensitive users may want to review them more carefully before regular consumption."
                )
            }
        }
    }
}

@Composable
private fun FirstAidCard(
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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
