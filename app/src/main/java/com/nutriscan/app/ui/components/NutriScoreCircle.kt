package com.nutriscan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nutriscan.app.model.NutriScore
import com.nutriscan.app.ui.theme.nutriscore_a
import com.nutriscan.app.ui.theme.nutriscore_b
import com.nutriscan.app.ui.theme.nutriscore_c
import com.nutriscan.app.ui.theme.nutriscore_d
import com.nutriscan.app.ui.theme.nutriscore_e

@Composable
fun NutriScoreCircle(
    score: NutriScore,
    size: NutriScoreSize = NutriScoreSize.LARGE,
    modifier: Modifier = Modifier
) {
    val dimensions = when (size) {
        NutriScoreSize.SMALL -> 48.dp to MaterialTheme.typography.titleMedium
        NutriScoreSize.MEDIUM -> 80.dp to MaterialTheme.typography.headlineMedium
        NutriScoreSize.LARGE -> 120.dp to MaterialTheme.typography.displaySmall
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .size(dimensions.first)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(getNutriScoreColor(score)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.grade,
                style = dimensions.second.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        if (size == NutriScoreSize.LARGE) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nutri-Score",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

enum class NutriScoreSize {
    SMALL,
    MEDIUM,
    LARGE
}

private fun getNutriScoreColor(score: NutriScore): Color {
    return when (score) {
        NutriScore.A -> nutriscore_a
        NutriScore.B -> nutriscore_b
        NutriScore.C -> nutriscore_c
        NutriScore.D -> nutriscore_d
        NutriScore.E -> nutriscore_e
    }
}
