// app/src/main/java/com/example/novascope/ui/components/AiSummaryCard.kt
package com.example.novascope.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning // Keep this for the Error state
// Add an icon for the new state if needed, e.g.:
// import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.novascope.ai.SummaryState

@Composable
fun AiSummaryCard(
    summaryState: SummaryState,
    onRetry: () -> Unit,
    // Add any new callbacks if needed, e.g., onDownloadModel: () -> Unit
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "AI Summary",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Content based on state
            AnimatedContent(
                targetState = summaryState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "Summary State Animation"
            ) { state ->
                when (state) {
                    is SummaryState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Generating summary...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    is SummaryState.Success -> {
                        Text(
                            text = state.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(vertical = 8.dp)
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
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    // Add the new branch here:
                    is SummaryState.ModelNotDownloaded -> {
                        // Implement the UI for when the model is not downloaded.
                        // This is an example, adjust it to your needs.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // You might want a different icon, e.g., CloudDownload
                            Icon(
                                imageVector = Icons.Filled.Warning, // Placeholder, change if needed
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary, // Or another appropriate color
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AI model needs to be downloaded to generate summaries.", // Or state.message if ModelNotDownloaded has a message
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // Or another appropriate color
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // You might want a button to trigger the download
                            /*
                            Button(
                                onClick = { /* Call a function to start model download */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Download Model")
                            }
                            */
                        }
                    }
                    // If SummaryState is a sealed class and you've covered all subtypes,
                    // an 'else' branch is not strictly necessary. However, if it's an enum
                    // or you want to be safe for future additions, you can add an 'else'.
                    // else -> { /* Handle any other unexpected state, or leave empty if truly exhaustive */ }
                }
            }

            // Add a source attribution for the AI model
            AnimatedVisibility(
                visible = summaryState is SummaryState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Generated using SmolLM2-135M",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}