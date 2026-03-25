package com.nutriscan.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.ui.components.BottomNavigationBar
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.SettingsUiState
import com.nutriscan.app.viewmodel.SettingsViewModel
import com.nutriscan.app.viewmodel.SettingsViewModelFactory

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext as android.app.Application)
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearHistoryDialog,
            title = { Text("Clear history") },
            text = { Text("Remove all scanned products from your device history?") },
            confirmButton = {
                TextButton(onClick = viewModel::clearHistory) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearHistoryDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        SettingsContent(
            uiState = uiState,
            onSetTheme = viewModel::setTheme,
            onSetLanguage = viewModel::setLanguage,
            onToggleAllergen = viewModel::toggleAllergen,
            onSetHalalMode = viewModel::setHalalMode,
            onSetHealthyMode = viewModel::setHealthyMode,
            onSetVeganMode = viewModel::setVeganMode,
            onClearHistory = viewModel::showClearHistoryDialog,
            appVersion = viewModel.getAppVersion(),
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onSetTheme: (String) -> Unit,
    onSetLanguage: (String) -> Unit,
    onToggleAllergen: (Allergen) -> Unit,
    onSetHalalMode: (Boolean) -> Unit,
    onSetHealthyMode: (Boolean) -> Unit,
    onSetVeganMode: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionTitle(
                title = "Settings",
                subtitle = "Adjust preferences, dietary modes, and device-level product behavior."
            )
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Guest mode is active. Subscription and cloud sync can be added later without changing this MVP flow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted_text
                        )
                    }
                }
            }
        }

        item { SectionTitle(title = "Dietary modes") }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.Shield,
                        title = "Halal mode",
                        subtitle = "Elevate halal conflicts in the analysis summary.",
                        checked = uiState.halalModeEnabled,
                        onCheckedChange = onSetHalalMode
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Tune,
                        title = "Healthy mode",
                        subtitle = "Highlight high-sugar and additive-heavy products more clearly.",
                        checked = uiState.healthyModeEnabled,
                        onCheckedChange = onSetHealthyMode
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Star,
                        title = "Vegan mode",
                        subtitle = "Reserve space for plant-based conflicts in future analysis updates.",
                        checked = uiState.veganModeEnabled,
                        onCheckedChange = onSetVeganMode
                    )
                }
            }
        }

        item { SectionTitle(title = "Allergens") }

        item {
            SettingsCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Allergen.values().forEach { allergen ->
                        FilterChip(
                            selected = uiState.selectedAllergens.contains(allergen),
                            onClick = { onToggleAllergen(allergen) },
                            label = {
                                Text(
                                    allergen.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                )
                            }
                        )
                    }
                }
            }
        }

        item { SectionTitle(title = "Language") }

        item {
            SettingsChoiceCard(
                icon = Icons.Default.Language,
                title = "App language",
                selectedValue = uiState.selectedLanguage,
                options = listOf(
                    "system" to "System",
                    "en" to "English",
                    "ru" to "Russian",
                    "uz" to "Uzbek"
                ),
                onSelected = onSetLanguage
            )
        }

        item { SectionTitle(title = "Appearance") }

        item {
            SettingsChoiceCard(
                icon = Icons.Default.Info,
                title = "Theme",
                selectedValue = uiState.selectedTheme,
                options = listOf(
                    "system" to "System",
                    "light" to "Light",
                    "dark" to "Dark"
                ),
                onSelected = onSetTheme
            )
        }

        item { SectionTitle(title = "FAQ") }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FaqItem(
                        question = "How does scanning work?",
                        answer = "Barcode is the primary path. When a product is missing, NutriScan keeps the response honest and offers manual search or photo fallback entry."
                    )
                    FaqItem(
                        question = "What do SAFE, CAUTION, and DANGER mean?",
                        answer = "They summarize how the detected ingredients interact with your active allergens and dietary modes."
                    )
                }
            }
        }

        item { SectionTitle(title = "Policy") }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "NutriScan provides educational product guidance only and does not replace professional medical advice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted_text
                    )
                }
            }
        }

        item { SectionTitle(title = "About") }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "NutriScan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version $appVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted_text
                    )
                    Text(
                        text = "A health-tech assistant for faster, clearer food safety decisions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted_text
                    )
                    Text(
                        text = "${uiState.historyCount} scanned items stored on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted_text
                    )
                    Text(
                        text = "Clear history",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable(onClick = onClearHistory)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = muted_text)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = selectedValue == value,
                    onClick = { onSelected(value) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = question,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = muted_text
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NutriScanTheme {
        SettingsScreen(navController = rememberNavController())
    }
}
