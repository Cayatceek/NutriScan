package com.nutriscan.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nutriscan.app.R
import com.nutriscan.app.localization.localizedIngredientTone
import com.nutriscan.app.model.Ingredient
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.model.SafetyStatus
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.card_stroke
import com.nutriscan.app.ui.theme.ingredient_bad
import com.nutriscan.app.ui.theme.ingredient_good
import com.nutriscan.app.ui.theme.ingredient_neutral
import com.nutriscan.app.ui.theme.muted_text
import com.nutriscan.app.ui.theme.status_caution
import com.nutriscan.app.ui.theme.status_caution_container
import com.nutriscan.app.ui.theme.status_danger
import com.nutriscan.app.ui.theme.status_danger_container
import com.nutriscan.app.ui.theme.status_safe
import com.nutriscan.app.ui.theme.status_safe_container

@Composable
fun StatusBanner(
    status: SafetyStatus,
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = statusColors(status)
    val icon = when (status) {
        SafetyStatus.SAFE -> Icons.Outlined.CheckCircle
        SafetyStatus.CAUTION -> Icons.Outlined.ReportProblem
        SafetyStatus.DANGER -> Icons.Outlined.ErrorOutline
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(contentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.92f)
                    )
                }
            }
        }
    }
}

@Composable
fun WarningChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WarningCard(
    title: String,
    items: List<String>,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
}

@Composable
fun IngredientCard(
    ingredient: Ingredient,
    modifier: Modifier = Modifier
) {
    val tone = when (ingredient.type) {
        IngredientType.GOOD -> ingredient_good
        IngredientType.BAD -> ingredient_bad
        IngredientType.NEUTRAL -> ingredient_neutral
    }
    val toneLabel = when (ingredient.type) {
        IngredientType.GOOD -> localizedIngredientTone(IngredientType.GOOD)
        IngredientType.BAD -> localizedIngredientTone(IngredientType.BAD)
        IngredientType.NEUTRAL -> localizedIngredientTone(IngredientType.NEUTRAL)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(UiTokens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(tone, CircleShape)
                )
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = toneLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = tone,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                text = ingredient.description ?: stringResource(R.string.ingredient_description_fallback),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecommendationBlock(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UiTokens.LargeRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceXS)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecommendationCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    RecommendationBlock(
        title = title,
        message = message,
        modifier = modifier
    )
}

@Composable
fun ModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = card_stroke,
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun SectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text
            )
        }
    }
}

@Composable
fun LoadingState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "loader")
    val pulse = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, card_stroke),
            elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(14.dp)
                        .alpha(pulse.value)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .alpha(pulse.value)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(12.dp)
                        .alpha(pulse.value)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text
                )
            }
        }
    }
}

@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, card_stroke),
            elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReportProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted_text,
                    textAlign = TextAlign.Center
                )
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.LocalOffer,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, card_stroke),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationLow)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = muted_text,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

fun statusColors(status: SafetyStatus): Pair<Color, Color> {
    return when (status) {
        SafetyStatus.SAFE -> status_safe_container to status_safe
        SafetyStatus.CAUTION -> status_caution_container to status_caution
        SafetyStatus.DANGER -> status_danger_container to status_danger
    }
}

fun warningIconForTitle(title: String): ImageVector {
    return when {
        title.contains("allergen", ignoreCase = true) -> Icons.Outlined.ReportProblem
        title.contains("diet", ignoreCase = true) || title.contains("halal", ignoreCase = true) -> Icons.Outlined.Shield
        else -> Icons.Outlined.LocalOffer
    }
}
