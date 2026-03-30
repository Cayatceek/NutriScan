package com.nutriscan.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriscan.app.R
import com.nutriscan.app.ui.theme.NutriScanTheme
import com.nutriscan.app.ui.theme.UiTokens
import com.nutriscan.app.ui.theme.muted_text

private data class OnboardingSlideModel(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlideModel(
                icon = Icons.Outlined.Visibility,
                titleRes = R.string.onboarding_slide_1_title,
                bodyRes = R.string.onboarding_slide_1_body
            ),
            OnboardingSlideModel(
                icon = Icons.Outlined.QrCodeScanner,
                titleRes = R.string.onboarding_slide_2_title,
                bodyRes = R.string.onboarding_slide_2_body
            ),
            OnboardingSlideModel(
                icon = Icons.Outlined.Shield,
                titleRes = R.string.onboarding_slide_3_title,
                bodyRes = R.string.onboarding_slide_3_body
            )
        )
    }
    var currentSlide by remember { mutableIntStateOf(0) }
    val isLastSlide = currentSlide == slides.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onContinue) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
            ) {
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (slideInHorizontally { it / 5 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 5 } + fadeOut())
                    },
                    label = "onboarding-slide"
                ) { page ->
                    OnboardingSlide(slide = slides[page])
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    slides.forEachIndexed { index, _ ->
                        val selected = index == currentSlide
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(if (selected) 28.dp else 8.dp)
                                .height(8.dp)
                                .background(
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                    },
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceS)) {
                Button(
                    onClick = {
                        if (isLastSlide) onContinue() else currentSlide += 1
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiTokens.ButtonHeight),
                    shape = RoundedCornerShape(UiTokens.Radius)
                ) {
                    Text(
                        text = if (isLastSlide) {
                            stringResource(R.string.onboarding_button_continue)
                        } else {
                            stringResource(R.string.onboarding_button_next)
                        }
                    )
                }

                if (currentSlide > 0) {
                    TextButton(
                        onClick = { currentSlide -= 1 },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlide(slide: OnboardingSlideModel) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = UiTokens.CardElevationMedium)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiTokens.SpaceM)
                ) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f - (index * 0.1f))
                                .height(if (index == 0) 14.dp else 10.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + index * 0.05f),
                                    RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(slide.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(slide.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted_text,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    NutriScanTheme {
        OnboardingScreen(onContinue = {})
    }
}
