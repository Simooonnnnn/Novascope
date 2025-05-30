// app/src/main/java/com/example/novascope/ui/components/ModelImportDialog.kt
package com.example.novascope.ui.components

import android.util.Log
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
import com.example.novascope.ai.ModelFileManager

@Composable
fun ModelImportDialog(
    importState: ModelFileManager.ImportState,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    // Log the current state for debugging
    LaunchedEffect(importState) {
        Log.d("ModelImportDialog", "Dialog state changed to: $importState")
    }

    Dialog(onDismissRequest = {
        Log.d("ModelImportDialog", "Dialog dismissed")
        // If we're in the middle of downloading, cancel the download
        if (importState is ModelFileManager.ImportState.Importing) {
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
                    text = "AI Summarization Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Download the T5 neural network model for advanced AI summarization.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (importState) {
                    is ModelFileManager.ImportState.Idle -> {
                        IdleContent(onDownloadClick = onDownloadClick)
                    }

                    is ModelFileManager.ImportState.Importing -> {
                        ImportingContent(
                            progress = importState.progress,
                            onCancelDownload = onCancelDownload
                        )
                    }

                    is ModelFileManager.ImportState.Success -> {
                        SuccessContent(onDismiss = onDismiss)
                    }

                    is ModelFileManager.ImportState.Error -> {
                        ErrorContent(
                            message = importState.message,
                            onDownloadClick = onDownloadClick,
                            onDismiss = onDismiss
                        )
                    }
                }

                // Show "Maybe Later" button only when not downloading or successful
                if (importState !is ModelFileManager.ImportState.Success &&
                    importState !is ModelFileManager.ImportState.Importing) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            Log.d("ModelImportDialog", "Maybe Later clicked")
                            onDismiss()
                        }
                    ) {
                        Text("Maybe Later")
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onDownloadClick: () -> Unit) {
    Button(
        onClick = {
            Log.d("ModelImportDialog", "Download AI Model button clicked in dialog")
            onDownloadClick()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Download AI Model")
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About the AI Model:",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = "• T5 (Text-to-Text Transfer Transformer)\n• Optimized for mobile devices\n• ~25MB download size\n• Runs entirely on your device",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy-focused: All processing happens locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ImportingContent(
    progress: Int,
    onCancelDownload: () -> Unit
) {
    Text(
        text = "Downloading AI Model",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "${progress}% complete",
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(16.dp))

    LinearProgressIndicator(
        progress = { progress / 100f },
        modifier = Modifier.fillMaxWidth(),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (progress < 50) "Downloading neural network model..." else "Setting up AI summarization...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = {
            Log.d("ModelImportDialog", "Cancel download clicked")
            onCancelDownload()
        },
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

@Composable
private fun SuccessContent(onDismiss: () -> Unit) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(64.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "AI Model Ready!",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "T5 neural network model has been successfully downloaded and is ready to generate intelligent summaries.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            Log.d("ModelImportDialog", "Start Using AI Summaries clicked")
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Using AI Summaries")
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Error,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(48.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Download Failed",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            Log.d("ModelImportDialog", "Try Again clicked")
            onDownloadClick()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Try Again")
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = {
            Log.d("ModelImportDialog", "Close error dialog clicked")
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Close")
    }
}