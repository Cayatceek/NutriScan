package com.nutriscan.app.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscan.app.R
import com.nutriscan.app.localization.localizedAnalysisItem
import com.nutriscan.app.localization.localizedNutriScoreNote
import com.nutriscan.app.localization.localizedRecommendation
import com.nutriscan.app.localization.localizedStatusLabel
import com.nutriscan.app.localization.localizedStatusMessage
import com.nutriscan.app.model.Ingredient
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore
import com.nutriscan.app.model.Product
import com.nutriscan.app.model.ProductAnalysis
import com.nutriscan.app.model.Recommendation
import com.nutriscan.app.model.ScanResult
import com.nutriscan.app.model.SafetyStatus
import com.nutriscan.app.model.Warning
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.IngredientCard
import com.nutriscan.app.ui.components.LoadingState
import com.nutriscan.app.ui.components.RecommendationBlock
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.components.StatusBanner
import com.nutriscan.app.ui.components.WarningCard
import com.nutriscan.app.ui.components.WarningChip
import com.nutriscan.app.ui.components.warningIconForTitle
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.ui.theme.status_caution
import com.nutriscan.app.ui.theme.status_danger
import com.nutriscan.app.ui.theme.status_safe
import com.nutriscan.app.viewmodel.ResultViewModel
import com.nutriscan.app.viewmodel.ResultViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onScanAgain: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ResultViewModel = viewModel(
        factory = ResultViewModelFactory(context.applicationContext as android.app.Application)
    )
    val uiState by viewModel.uiState.collectAsState()
    var showAlternativesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadResult(productId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.shareResult()?.let { intent ->
                                startActivity(
                                    context,
                                    Intent.createChooser(intent, context.getString(R.string.result_share_chooser)),
                                    null
                                )
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Outlined.IosShare, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    title = stringResource(R.string.result_loading_title),
                    message = stringResource(R.string.result_loading_message),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    title = stringResource(R.string.result_error_title),
                    message = uiState.error ?: stringResource(R.string.result_error_default),
                    actionLabel = stringResource(R.string.retry),
                    onAction = { viewModel.loadResult(productId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.scanResult != null -> {
                ResultContent(
                    scanResult = uiState.scanResult!!,
                    productAnalysis = uiState.productAnalysis,
                    isCachedResult = uiState.isCachedResult,
                    isFavorite = uiState.isFavorite,
                    isSaved = uiState.isSaved,
                    onFavoriteClick = viewModel::toggleFavorite,
                    onSaveClick = viewModel::saveToHistory,
                    onAlternativesClick = { showAlternativesSheet = true },
                    onScanAgain = onScanAgain,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = paddingValues
                )
            }
        }
    }

    if (showAlternativesSheet && uiState.scanResult?.alternatives?.isNotEmpty() == true) {
        ModalBottomSheet(onDismissRequest = { showAlternativesSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiTokens.SpaceL),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                Text(
                    text = stringResource(R.string.result_alternatives_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                uiState.scanResult?.alternatives?.forEach { alternative ->
                    AlternativeCard(product = alternative)
                }
                Spacer(modifier = Modifier.height(UiTokens.SpaceS))
            }
        }
    }
}

@Composable
private fun ResultContent(
    scanResult: ScanResult,
    productAnalysis: ProductAnalysis?,
    isCachedResult: Boolean,
    isFavorite: Boolean,
    isSaved: Boolean,
    onFavoriteClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAlternativesClick: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val analysis = productAnalysis
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = UiTokens.SpaceM,
            top = UiTokens.SpaceM + contentPadding.calculateTopPadding(),
            end = UiTokens.SpaceM,
            bottom = UiTokens.SpaceL + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
    ) {
        item {
            ProductHeaderCard(
                scanResult = scanResult,
                isCachedResult = isCachedResult
            )
        }

        analysis?.let { safeAnalysis ->
            item {
                StatusBanner(
                    status = safeAnalysis.safetyStatus,
                    title = localizedStatusLabel(safeAnalysis.safetyStatus),
                    supportingText = localizedStatusMessage(safeAnalysis.safetyStatus)
                )
            }

            item {
                SafetyHighlightsCard(analysis = safeAnalysis)
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.result_body_title),
                    subtitle = stringResource(R.string.result_body_subtitle)
                )
            }

            item {
                BodyImpactCard(
                    bullets = buildImpactBullets(
                        context = context,
                        analysis = safeAnalysis,
                        nutriScore = scanResult.product.nutriscore
                    )
                )
            }

            item {
                val (recommendationTitle, recommendationMessage) = localizedRecommendation(safeAnalysis.safetyStatus)
                RecommendationBlock(
                    title = recommendationTitle,
                    message = recommendationMessage
                )
            }
        }

        item {
            ResultActionsCard(
                isFavorite = isFavorite,
                isSaved = isSaved,
                hasAlternatives = scanResult.alternatives.isNotEmpty(),
                onFavoriteClick = onFavoriteClick,
                onSaveClick = onSaveClick,
                onAlternativesClick = onAlternativesClick,
                onScanAgain = onScanAgain
            )
        }

        item {
            SectionTitle(
                title = stringResource(R.string.result_ingredient_explanation_title),
                subtitle = stringResource(R.string.result_ingredient_explanation_subtitle)
            )
        }

        items(scanResult.product.ingredients) { ingredient ->
            IngredientCard(ingredient = ingredient)
        }
    }
}

@Composable
private fun ProductHeaderCard(
    scanResult: ScanResult,
    isCachedResult: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
            ) {
                MetaPill(label = scanResult.product.brand)
                MetaPill(label = stringResource(R.string.result_meta_nutriscore, scanResult.product.nutriscore.grade))
                if (isCachedResult) {
                    MetaPill(label = stringResource(R.string.result_meta_cached_source))
                }
            }
            Text(
                text = scanResult.product.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.result_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
        }
    }
}

@Composable
private fun MetaPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SafetyHighlightsCard(
    analysis: ProductAnalysis
) {
    val context = LocalContext.current
    val allergenItems = analysis.detectedAllergens.map(context::localizedAnalysisItem)
    val harmfulItems = analysis.harmfulIngredients.map(context::localizedAnalysisItem)
    val dietaryWarnings = extractDietaryWarnings(analysis.warnings).map(context::localizedAnalysisItem)

    Card(
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Text(
                text = stringResource(R.string.result_warnings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            WarningCard(
                title = stringResource(R.string.result_warnings_allergens),
                items = allergenItems,
                icon = warningIconForTitle("allergens"),
                tint = status_danger
            )

            WarningCard(
                title = stringResource(R.string.result_warnings_additives),
                items = harmfulItems,
                icon = warningIconForTitle("additives"),
                tint = status_caution
            )

            WarningCard(
                title = stringResource(R.string.result_warnings_dietary),
                items = dietaryWarnings,
                icon = warningIconForTitle("dietary"),
                tint = status_danger
            )

            if (
                analysis.detectedAllergens.isEmpty() &&
                analysis.harmfulIngredients.isEmpty() &&
                dietaryWarnings.isEmpty()
            ) {
                WarningChip(
                    label = stringResource(R.string.result_warnings_none),
                    icon = Icons.Outlined.CheckCircle,
                    tint = status_safe
                )
            }
        }
    }
}

@Composable
private fun BodyImpactCard(
    bullets: List<String>
) {
    Card(
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            bullets.forEach { bullet ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultActionsCard(
    isFavorite: Boolean,
    isSaved: Boolean,
    hasAlternatives: Boolean,
    onFavoriteClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAlternativesClick: () -> Unit,
    onScanAgain: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Text(
                text = stringResource(R.string.result_actions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)) {
                OutlinedButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(UiTokens.Radius)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                    Text(
                        if (isFavorite) {
                            stringResource(R.string.result_action_favorited)
                        } else {
                            stringResource(R.string.result_action_favorite)
                        }
                    )
                }
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.ButtonHeight),
                    enabled = !isSaved,
                    shape = RoundedCornerShape(UiTokens.Radius)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                    Text(
                        if (isSaved) {
                            stringResource(R.string.result_action_saved)
                        } else {
                            stringResource(R.string.result_action_save)
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)) {
                OutlinedButton(
                    onClick = onAlternativesClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.ButtonHeight),
                    enabled = hasAlternatives,
                    shape = RoundedCornerShape(UiTokens.Radius)
                ) {
                    Text(stringResource(R.string.result_action_alternatives))
                }
                Button(
                    onClick = onScanAgain,
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(UiTokens.Radius)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                    Text(stringResource(R.string.result_action_scan_again))
                }
            }
        }
    }
}

@Composable
private fun AlternativeCard(
    product: Product
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiTokens.SpaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.nutriscore.grade,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildImpactBullets(
    context: android.content.Context,
    analysis: ProductAnalysis,
    nutriScore: NutriScore
): List<String> {
    val bullets = mutableListOf<String>()
    val allergenItems = analysis.detectedAllergens.map(context::localizedAnalysisItem).distinct()
    val harmfulItems = analysis.harmfulIngredients.map(context::localizedAnalysisItem).distinct()
    val dietaryItems = extractDietaryWarnings(analysis.warnings).map(context::localizedAnalysisItem).distinct()

    if (allergenItems.isNotEmpty()) {
        bullets += context.getString(R.string.result_body_allergens, allergenItems.joinToString(", "))
    }
    if (harmfulItems.isNotEmpty()) {
        bullets += context.getString(R.string.result_body_harmful, harmfulItems.joinToString(", "))
    }
    if (dietaryItems.isNotEmpty()) {
        bullets += context.getString(R.string.result_body_dietary, dietaryItems.joinToString(", "))
    }
    bullets += context.localizedNutriScoreNote(nutriScore)

    return bullets
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { listOf(context.getString(R.string.result_body_no_major)) }
        .take(4)
}

private fun extractDietaryWarnings(warnings: List<Warning>): List<String> {
    return warnings
        .flatMap { warning ->
            warning.message
                .substringAfter(':', warning.message)
                .split(",")
        }
        .map { it.trim().trimEnd('.') }
        .filter { it.isNotBlank() }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    NutriScanTheme {
        ResultContent(
            scanResult = ScanResult(
                product = Product(
                    id = "1",
                    name = "Chocolate Milk Bar",
                    brand = "Nutri Foods",
                    nutriscore = NutriScore.D,
                    ingredients = listOf(
                        Ingredient("Milk powder", IngredientType.BAD, "Contains dairy and can trigger milk sensitivity."),
                        Ingredient("Sugar", IngredientType.BAD, "High sugar load when eaten regularly."),
                        Ingredient("Cocoa", IngredientType.NEUTRAL, "Flavor ingredient.")
                    )
                ),
                explanation = "Poor nutritional quality. Limit consumption.",
                alternatives = listOf(
                    Product(
                        id = "2",
                        name = "Plain Chickpea Chips",
                        brand = "Simply Crunch",
                        nutriscore = NutriScore.B,
                        ingredients = emptyList()
                    )
                )
            ),
            productAnalysis = ProductAnalysis(
                safetyStatus = SafetyStatus.DANGER,
                detectedAllergens = listOf("Dairy"),
                harmfulIngredients = listOf("High sugar"),
                warnings = listOf(
                    Warning(
                        title = "Halal",
                        message = "Contains gelatin"
                    )
                ),
                explanation = "This product matches your selected allergens. Multiple risk signals are present.",
                recommendation = Recommendation(
                    title = "Avoid this product",
                    message = "Choose a lower sugar option without dairy ingredients."
                )
            ),
            isCachedResult = false,
            isFavorite = false,
            isSaved = false,
            onFavoriteClick = {},
            onSaveClick = {},
            onAlternativesClick = {},
            onScanAgain = {}
        )
    }
}
