package com.nutriscan.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscan.app.R
import com.nutriscan.app.localization.asStringOrNull
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
import com.nutriscan.app.ui.components.BrandedTransitionScreen
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.IngredientCard
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.components.WarningChip
import com.nutriscan.app.ui.components.statusColors
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.viewmodel.ResultViewModel
import com.nutriscan.app.viewmodel.ResultViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    productId: String,
    onNavigateBack: () -> Unit,
    onScanAgain: () -> Unit,
    onOpenAlternative: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ResultViewModel = viewModel(
        factory = ResultViewModelFactory(context.applicationContext as android.app.Application)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAlternativesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadResultIfNeeded(productId)
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
                            buildShareIntent(
                                context = context,
                                scanResult = uiState.scanResult,
                                analysis = uiState.productAnalysis
                            )?.let { intent ->
                                context.startActivity(
                                    Intent.createChooser(
                                        intent,
                                        context.getString(R.string.result_share_chooser)
                                    )
                                )
                            }
                        },
                        enabled = uiState.scanResult != null
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
                BrandedTransitionScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    title = stringResource(R.string.result_error_title),
                    message = uiState.error.asStringOrNull()
                        ?: stringResource(R.string.result_error_default),
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
                    onFavoriteClick = viewModel::toggleFavorite,
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
                    AlternativeCard(
                        product = alternative,
                        onClick = {
                            showAlternativesSheet = false
                            onOpenAlternative(alternative.id)
                        }
                    )
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
    onFavoriteClick: () -> Unit,
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
            ResultHeroCard(
                scanResult = scanResult,
                analysis = analysis,
                isCachedResult = isCachedResult
            )
        }

        analysis?.let { safeAnalysis ->
            item {
                ResultSignalsCard(analysis = safeAnalysis)
            }

            item {
                DecisionSummaryCard(
                    recommendation = localizedRecommendation(safeAnalysis.safetyStatus),
                    bullets = buildImpactBullets(
                        context = context,
                        analysis = safeAnalysis,
                        nutriScore = scanResult.product.nutriscore
                    )
                )
            }
        }

        item {
            ResultActionCluster(
                isFavorite = isFavorite,
                hasAlternatives = scanResult.alternatives.isNotEmpty(),
                onFavoriteClick = onFavoriteClick,
                onAlternativesClick = onAlternativesClick,
                onScanAgain = onScanAgain
            )
        }

        if (scanResult.product.ingredients.isNotEmpty()) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultHeroCard(
    scanResult: ScanResult,
    analysis: ProductAnalysis?,
    isCachedResult: Boolean
) {
    val status = analysis?.safetyStatus ?: SafetyStatus.CAUTION
    val statusLabel = localizedStatusLabel(status)
    val statusMessage = localizedStatusMessage(status)
    val (containerColor, contentColor) = statusColors(status)
    val statusIcon = when (status) {
        SafetyStatus.SAFE -> Icons.Outlined.CheckCircle
        SafetyStatus.CAUTION -> Icons.Outlined.ReportProblem
        SafetyStatus.DANGER -> Icons.Outlined.ErrorOutline
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
            ) {
                if (scanResult.product.brand.isNotBlank()) {
                    HeroMetaPill(label = scanResult.product.brand)
                }
                HeroMetaPill(label = stringResource(R.string.result_meta_nutriscore, scanResult.product.nutriscore.grade))
                if (isCachedResult) {
                    HeroMetaPill(label = stringResource(R.string.result_meta_cached_source))
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = containerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = scanResult.product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetaPill(
    label: String
) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultSignalsCard(
    analysis: ProductAnalysis
) {
    val context = LocalContext.current
    val allergenItems = analysis.detectedAllergens.map(context::localizedAnalysisItem).distinct()
    val harmfulItems = analysis.harmfulIngredients.map(context::localizedAnalysisItem).distinct()
    val dietaryWarnings = extractDietaryWarnings(analysis.warnings)
        .map(context::localizedAnalysisItem)
        .distinct()

    Card(
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
        ) {
            Text(
                text = stringResource(R.string.result_warnings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (allergenItems.isEmpty() && harmfulItems.isEmpty() && dietaryWarnings.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(UiTokens.Radius),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = statusColors(SafetyStatus.SAFE).second,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.result_warnings_none),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (allergenItems.isNotEmpty()) {
                    SignalGroup(
                        title = stringResource(R.string.result_warnings_allergens),
                        items = allergenItems,
                        icon = Icons.Outlined.ReportProblem,
                        tint = statusColors(SafetyStatus.DANGER).second
                    )
                }
                if (harmfulItems.isNotEmpty()) {
                    SignalGroup(
                        title = stringResource(R.string.result_warnings_additives),
                        items = harmfulItems,
                        icon = Icons.Outlined.LocalOffer,
                        tint = statusColors(SafetyStatus.CAUTION).second
                    )
                }
                if (dietaryWarnings.isNotEmpty()) {
                    SignalGroup(
                        title = stringResource(R.string.result_warnings_dietary),
                        items = dietaryWarnings,
                        icon = Icons.Outlined.Shield,
                        tint = statusColors(SafetyStatus.DANGER).second
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignalGroup(
    title: String,
    items: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
        ) {
            items.forEach { item ->
                WarningChip(
                    label = item,
                    icon = icon,
                    tint = tint
                )
            }
        }
    }
}

@Composable
private fun DecisionSummaryCard(
    recommendation: Pair<String, String>,
    bullets: List<String>
) {
    Card(
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Text(
                text = recommendation.first,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = recommendation.second,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            bullets.forEach { bullet ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.42f)
                    ) {}
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultActionCluster(
    isFavorite: Boolean,
    hasAlternatives: Boolean,
    onFavoriteClick: () -> Unit,
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
            Button(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiTokens.ButtonHeight),
                shape = RoundedCornerShape(UiTokens.Radius)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                Text(text = stringResource(R.string.result_action_scan_again))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                OutlinedButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(UiTokens.Radius),
                    border = BorderStroke(
                        1.dp,
                        if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f) else card_stroke
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isFavorite) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                    Text(
                        text = if (isFavorite) {
                            stringResource(R.string.result_action_favorited)
                        } else {
                            stringResource(R.string.result_action_favorite)
                        }
                    )
                }

                if (hasAlternatives) {
                    OutlinedButton(
                        onClick = onAlternativesClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(UiTokens.ButtonHeight),
                        shape = RoundedCornerShape(UiTokens.Radius)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalOffer,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(UiTokens.SpaceXS))
                        Text(text = stringResource(R.string.result_action_alternatives))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlternativeCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = product.nutriscore.grade,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildShareIntent(
    context: android.content.Context,
    scanResult: ScanResult?,
    analysis: ProductAnalysis?
): Intent? {
    if (scanResult == null) return null

    val lines = buildList {
        add(scanResult.product.name)
        analysis?.let { safeAnalysis ->
            add(
                context.getString(
                    R.string.share_safety_status,
                    context.localizedStatusLabel(safeAnalysis.safetyStatus)
                )
            )
        }

        val allergens = analysis?.detectedAllergens
            ?.map(context::localizedAnalysisItem)
            ?.distinct()
            .orEmpty()
        if (allergens.isNotEmpty()) {
            add(context.getString(R.string.share_detected_allergens, allergens.joinToString(", ")))
        }

        val harmful = analysis?.harmfulIngredients
            ?.map(context::localizedAnalysisItem)
            ?.distinct()
            .orEmpty()
        if (harmful.isNotEmpty()) {
            add(context.getString(R.string.share_harmful_ingredients, harmful.joinToString(", ")))
        }

        analysis?.let { safeAnalysis ->
            val (_, recommendationMessage) = context.localizedRecommendation(safeAnalysis.safetyStatus)
            add(context.getString(R.string.share_recommendation, recommendationMessage))
        } ?: add(context.localizedNutriScoreNote(scanResult.product.nutriscore))

        if (scanResult.product.ingredients.isNotEmpty()) {
            add("")
            add(context.getString(R.string.share_ingredients_heading))
            scanResult.product.ingredients.take(6).forEach { ingredient ->
                add("• ${ingredient.name}")
            }
        }

        add("")
        add(context.getString(R.string.share_footer))
    }.joinToString(separator = "\n")

    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, scanResult.product.name))
        putExtra(Intent.EXTRA_TEXT, lines)
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
                        Ingredient(
                            "Milk powder",
                            IngredientType.BAD,
                            "Contains dairy and can trigger milk sensitivity."
                        ),
                        Ingredient(
                            "Sugar",
                            IngredientType.BAD,
                            "High sugar load when eaten regularly."
                        ),
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
            onFavoriteClick = {},
            onAlternativesClick = {},
            onScanAgain = {}
        )
    }
}
