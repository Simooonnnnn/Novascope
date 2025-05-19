// app/src/main/java/com/example/novascope/ui/components/SummaryDialog.kt
package com.example.novascope.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.novascope.ai.SummaryState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryDialog(
    summaryState: SummaryState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedIcon(
                            when (summaryState) {
                                is SummaryState.Loading -> Icons.Default.Psychology
                                is SummaryState.Success -> Icons.Default.Check
                                is SummaryState.Error -> Icons.Default.Warning
                            },
                            tint = when (summaryState) {
                                is SummaryState.Loading -> MaterialTheme.colorScheme.primary
                                is SummaryState.Success -> MaterialTheme.colorScheme.primary
                                is SummaryState.Error -> MaterialTheme.colorScheme.error
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "AI Summary",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Content based on state
                AnimatedContent(
                    targetState = summaryState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) +
                                expandVertically(animationSpec = tween(300, easing = EaseOutCubic)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "Summary Animation"
                ) { state ->
                    when (state) {
                        is SummaryState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Animated circular loading
                                    val transition = rememberInfiniteTransition(label = "Loading Animation")
                                    val angle by transition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "Rotation Animation"
                                    )

                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 4.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        "Analyzing article...",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        is SummaryState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = state.summary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Divider()

                                Text(
                                    text = "Generated using SmolLM2-135M",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }

                        is SummaryState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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

@Composable
private fun AnimatedIcon(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "Icon Animation")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale Animation"
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(32.dp)
            .then(
                if (icon == Icons.Default.Psychology) {
                    Modifier.scale(scale)
                } else {
                    Modifier
                }
            )
    )
}