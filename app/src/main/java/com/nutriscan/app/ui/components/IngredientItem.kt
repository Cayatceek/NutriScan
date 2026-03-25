package com.nutriscan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nutriscan.app.R
import com.nutriscan.app.model.Ingredient
import com.nutriscan.app.model.IngredientType
import com.nutriscan.app.ui.theme.ingredient_bad
import com.nutriscan.app.ui.theme.ingredient_good
import com.nutriscan.app.ui.theme.ingredient_neutral

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(getIngredientColor(ingredient.type))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ingredient.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Type Label
            Text(
                text = getIngredientTypeLabel(ingredient.type),
                style = MaterialTheme.typography.labelSmall,
                color = getIngredientColor(ingredient.type)
            )
        }
    }
}

@Composable
private fun getIngredientTypeLabel(type: IngredientType): String {
    return when (type) {
        IngredientType.GOOD -> stringResource(R.string.ingredients_good)
        IngredientType.BAD -> stringResource(R.string.ingredients_bad)
        IngredientType.NEUTRAL -> stringResource(R.string.ingredients_neutral)
    }
}

private fun getIngredientColor(type: IngredientType): androidx.compose.ui.graphics.Color {
    return when (type) {
        IngredientType.GOOD -> ingredient_good
        IngredientType.BAD -> ingredient_bad
        IngredientType.NEUTRAL -> ingredient_neutral
    }
}
