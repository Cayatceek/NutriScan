package com.nutriscan.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscan.app.model.Ingredient
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.NutriScore
import com.nutriscan.app.model.Product
import com.nutriscan.app.model.ProductAnalysis
import com.nutriscan.app.model.Recommendation
import com.nutriscan.app.model.ScanResult
import com.nutriscan.app.model.SafetyStatus
import com.nutriscan.app.ui.components.ErrorState
import com.nutriscan.app.ui.components.IngredientCard
import com.nutriscan.app.ui.components.LoadingState
import com.nutriscan.app.ui.components.RecommendationBlock
import com.nutriscan.app.ui.components.SectionTitle
import com.nutriscan.app.ui.components.StatusBanner
import com.nutriscan.app.ui.components.WarningCard
import com.nutriscan.app.ui.components.WarningChip
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.muted_text
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
                title = { Text("Product result") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.shareResult()?.let { intent ->
                                startActivity(context, Intent.createChooser(intent, "Share via"), null)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
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
                    title = "Preparing analysis",
                    message = "Checking safety signals and building the product summary.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            uiState.error != null -> {
                ErrorState(
                    title = "Result unavailable",
                    message = uiState.error ?: "We could not load this product right now.",
                    actionLabel = "Scan again",
                    onAction = onScanAgain,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            uiState.scanResult != null -> {
                ResultContent(
                    scanResult = uiState.scanResult!!,
                    productAnalysis = uiState.productAnalysis,
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
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(
                    text = "Alternatives",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = "Better options currently available in the local product catalog.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                uiState.scanResult?.alternatives?.forEach { alternative ->
                    AlternativeCard(
                        product = alternative,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ResultContent(
    scanResult: ScanResult,
    productAnalysis: ProductAnalysis?,
    isFavorite: Boolean,
    isSaved: Boolean,
    onFavoriteClick: () -> Unit,
    onSaveClick: () -> Unit,
    onAlternativesClick: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 20.dp + contentPadding.calculateTopPadding(),
            end = 16.dp,
            bottom = 28.dp + contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Nutri-Score ${scanResult.product.nutriscore.grade}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Text(
                    text = scanResult.product.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = scanResult.product.brand,
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted_text
                )
            }
        }

        productAnalysis?.let { analysis ->
            item {
                StatusBanner(
                    status = analysis.safetyStatus,
                    title = analysis.safetyStatus.name,
                    supportingText = bannerDescription(analysis)
                )
            }

            item {
                ResultSignalCard(analysis = analysis)
            }

            item {
                SectionTitle(
                    title = "How it affects your body",
                    subtitle = "Short, readable context for the current product."
                )
            }

            item {
                BodyImpactCard(
                    summary = analysis.explanation,
                    nutritionNote = scanResult.explanation
                )
            }

            item {
                SectionTitle(title = "Recommendation")
            }

            item {
                RecommendationBlock(
                    title = analysis.recommendation.title,
                    message = analysis.recommendation.message
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
                title = "Ingredient explanation",
                subtitle = "Each ingredient is labeled in plain language so you can scan the list quickly."
            )
        }

        items(scanResult.product.ingredients) { ingredient ->
            IngredientCard(ingredient = ingredient)
        }
    }
}

@Composable
private fun ResultSignalCard(
    analysis: ProductAnalysis
) {
    val dietaryWarnings = analysis.warnings.map { warning ->
        "${warning.title}: ${warning.message}"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Warnings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (analysis.detectedAllergens.isNotEmpty()) {
                WarningCard(title = "Allergens", items = analysis.detectedAllergens)
            }

            if (analysis.harmfulIngredients.isNotEmpty()) {
                WarningCard(title = "Harmful ingredients", items = analysis.harmfulIngredients)
            }

            if (dietaryWarnings.isNotEmpty()) {
                WarningCard(title = "Dietary conflicts", items = dietaryWarnings)
            }

            if (
                analysis.detectedAllergens.isEmpty() &&
                analysis.harmfulIngredients.isEmpty() &&
                dietaryWarnings.isEmpty()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WarningChip(label = "No active warnings")
                }
            }
        }
    }
}

@Composable
private fun BodyImpactCard(
    summary: String,
    nutritionNote: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = nutritionNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text,
                    modifier = Modifier.padding(14.dp)
                )
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Favorite")
                }
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaved,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaved) "Saved" else "Save")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAlternativesClick,
                    modifier = Modifier.weight(1f),
                    enabled = hasAlternatives,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Alternatives")
                }
                Button(
                    onClick = onScanAgain,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun AlternativeCard(
    product: Product,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            Text(
                text = "Nutri-Score ${product.nutriscore.grade}",
                style = MaterialTheme.typography.bodySmall,
                color = muted_text
            )
        }
    }
}

private fun bannerDescription(analysis: ProductAnalysis): String {
    return when (analysis.safetyStatus) {
        SafetyStatus.SAFE -> "No major safety conflicts were found for your current settings."
        SafetyStatus.CAUTION -> "Some ingredients need a closer look before regular consumption."
        SafetyStatus.DANGER -> "This product conflicts with active allergens or dietary safety rules."
    }
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
                    com.nutriscan.app.model.Warning(
                        title = "Halal",
                        message = "Check dairy processing source if halal mode matters for you."
                    )
                ),
                explanation = "This product matches your selected allergens and contains high sugar.",
                recommendation = Recommendation(
                    title = "Avoid this product",
                    message = "Choose a lower sugar option without dairy ingredients."
                )
            ),
            isFavorite = false,
            isSaved = false,
            onFavoriteClick = {},
            onSaveClick = {},
            onAlternativesClick = {},
            onScanAgain = {}
        )
    }
}
