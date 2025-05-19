// app/src/main/java/com/example/novascope/ui/components/ModelDownloadDialog.kt
package com.example.novascope.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.novascope.ai.ModelDownloadManager

@Composable
fun ModelDownloadDialog(
    downloadState: ModelDownloadManager.DownloadState,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {
        // If we're in the middle of downloading, cancel the download
        if (downloadState is ModelDownloadManager.DownloadState.Downloading) {
            onCancelDownload()
        }
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "AI Summarization Model",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To use AI summaries, you need to download the SmolLM2 language model (approx. 50MB).",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (downloadState) {
                    is ModelDownloadManager.DownloadState.Idle -> {
                        Button(
                            onClick = onDownloadClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Model")
                        }
                    }

                    is ModelDownloadManager.DownloadState.Downloading -> {
                        Text(
                            text = "Downloading: ${downloadState.progress}%",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onCancelDownload,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel Download")
                        }
                    }

                    is ModelDownloadManager.DownloadState.Success -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Download complete!",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }

                    is ModelDownloadManager.DownloadState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDownloadClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Download")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text("Close")
                        }
                    }
                }

                if (downloadState !is ModelDownloadManager.DownloadState.Success &&
                    downloadState !is ModelDownloadManager.DownloadState.Downloading) {
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}