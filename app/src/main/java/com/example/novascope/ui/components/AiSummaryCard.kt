// app/src/main/java/com/example/novascope/ui/components/AiSummaryCard.kt
package com.example.novascope.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.novascope.ai.SummaryState
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.theme.NovascopeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummaryCard(
    summaryState: SummaryState,
    onRetry: () -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Scale animation for card appearance
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card scale"
    )

    // Alpha animation for smooth fade
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = MaterialMotion.DURATION_MEDIUM,
            easing = MaterialMotion.EmphasizedDecelerateEasing
        ),
        label = "card alpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = MaterialMotion.DURATION_MEDIUM,
                easing = MaterialMotion.EmphasizedDecelerateEasing
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = MaterialMotion.DURATION_MEDIUM,
                easing = MaterialMotion.EmphasizedDecelerateEasing
            ),
            initialOffsetY = { -it / 2 }
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = MaterialMotion.DURATION_SHORT,
                easing = MaterialMotion.EmphasizedAccelerateEasing
            )
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = MaterialMotion.DURATION_SHORT,
                easing = MaterialMotion.EmphasizedAccelerateEasing
            ),
            targetOffsetY = { -it / 2 }
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(cardScale)
                .alpha(cardAlpha),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "AI Summary",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Content based on state
                AnimatedContent(
                    targetState = summaryState,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = MaterialMotion.DURATION_MEDIUM
                            )
                        ) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis = MaterialMotion.DURATION_SHORT
                            )
                        )
                    },
                    label = "summary content"
                ) { state ->
                    when (state) {
                        is SummaryState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 2.dp,
                                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f),
                                        strokeCap = StrokeCap.Round
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Generating summary...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        is SummaryState.Success -> {
                            Text(
                                text = state.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        is SummaryState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedButton(
                                    onClick = onRetry,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiSummaryLoadingPreview() {
    NovascopeTheme {
        AiSummaryCard(
            summaryState = SummaryState.Loading,
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiSummarySuccessPreview() {
    NovascopeTheme {
        AiSummaryCard(
            summaryState = SummaryState.Success(
                "This article discusses the latest advancements in AI technology, focusing on small language models that can run efficiently on mobile devices. These models offer privacy benefits by processing data locally without sending it to remote servers."
            ),
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiSummaryErrorPreview() {
    NovascopeTheme {
        AiSummaryCard(
            summaryState = SummaryState.Error("Could not generate summary. Please try again."),
            onRetry = {}
        )
    }
}