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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nutriscan.app.R
import com.nutriscan.app.domain.analyzer.Allergen
import com.nutriscan.app.localization.localizedAllergenLabel
import com.nutriscan.app.localization.localizedLanguageLabel
import com.nutriscan.app.localization.localizedThemeLabel
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
            title = { Text(stringResource(R.string.settings_clear_history_title)) },
            text = { Text(stringResource(R.string.settings_clear_history_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::clearHistory) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearHistoryDialog) {
                    Text(stringResource(R.string.cancel))
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
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle)
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
                            text = stringResource(R.string.settings_account_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_account_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted_text
                        )
                    }
                }
            }
        }

        item { SectionTitle(title = stringResource(R.string.settings_section_dietary)) }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.settings_halal_mode_title),
                        subtitle = stringResource(R.string.settings_halal_mode_body),
                        checked = uiState.halalModeEnabled,
                        onCheckedChange = onSetHalalMode
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.settings_healthy_mode_title),
                        subtitle = stringResource(R.string.settings_healthy_mode_body),
                        checked = uiState.healthyModeEnabled,
                        onCheckedChange = onSetHealthyMode
                    )
                    SettingsToggleRow(
                        icon = Icons.Default.Star,
                        title = stringResource(R.string.settings_vegan_mode_title),
                        subtitle = stringResource(R.string.settings_vegan_mode_body),
                        checked = uiState.veganModeEnabled,
                        onCheckedChange = onSetVeganMode
                    )
                }
            }
        }

        item { SectionTitle(title = stringResource(R.string.settings_section_allergens)) }

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
                                Text(localizedAllergenLabel(allergen))
                            }
                        )
                    }
                }
            }
        }

        item { SectionTitle(title = stringResource(R.string.settings_section_language)) }

        item {
            SettingsChoiceCard(
                icon = Icons.Default.Language,
                title = stringResource(R.string.settings_language_title),
                selectedValue = uiState.selectedLanguage,
                options = listOf(
                    "uz" to localizedLanguageLabel("uz"),
                    "ru" to localizedLanguageLabel("ru"),
                    "en" to localizedLanguageLabel("en")
                ),
                onSelected = onSetLanguage
            )
        }

        item { SectionTitle(title = stringResource(R.string.settings_section_appearance)) }

        item {
            SettingsChoiceCard(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_theme_title),
                selectedValue = uiState.selectedTheme,
                options = listOf(
                    "system" to localizedThemeLabel("system"),
                    "light" to localizedThemeLabel("light"),
                    "dark" to localizedThemeLabel("dark")
                ),
                onSelected = onSetTheme
            )
        }

        item { SectionTitle(title = stringResource(R.string.settings_section_faq)) }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FaqItem(
                        question = stringResource(R.string.settings_faq_scan_question),
                        answer = stringResource(R.string.settings_faq_scan_answer)
                    )
                    FaqItem(
                        question = stringResource(R.string.settings_faq_status_question),
                        answer = stringResource(R.string.settings_faq_status_answer)
                    )
                }
            }
        }

        item { SectionTitle(title = stringResource(R.string.settings_section_policy)) }

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
                        text = stringResource(R.string.settings_policy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted_text
                    )
                }
            }
        }

        item { SectionTitle(title = stringResource(R.string.settings_section_about)) }

        item {
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "NutriScan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.settings_about_version, appVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted_text
                    )
                    Text(
                        text = stringResource(R.string.settings_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted_text
                    )
                    Text(
                        text = stringResource(R.string.settings_about_history_count, uiState.historyCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = muted_text
                    )
                    Text(
                        text = stringResource(R.string.settings_clear_history_action),
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
