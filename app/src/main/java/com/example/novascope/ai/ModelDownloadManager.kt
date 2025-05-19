// app/src/main/java/com/example/novascope/ai/ModelDownloadManager.kt
package com.example.novascope.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
        const val MODEL_FILE = "smollm2_article_summarizer.tflite"
        const val MODEL_URL = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M/resolve/main/model_optimized.tflite"
        const val VOCAB_FILE = "smollm2_vocab.txt"
        const val VOCAB_URL = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M/resolve/main/tokenizer.txt"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    val isModelDownloaded: Boolean
        get() {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)
            return modelFile.exists() && vocabFile.exists()
        }

    suspend fun downloadModel() {
        if (isModelDownloaded) {
            _downloadState.value = DownloadState.Success
            return
        }

        _downloadState.value = DownloadState.Downloading(0)

        try {
            // Create directory if needed
            val assetsDir = File(context.filesDir, "assets")
            if (!assetsDir.exists()) {
                assetsDir.mkdirs()
            }

            // Download model
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)

            // First download model (larger file)
            downloadFileWithProgress(MODEL_URL, modelFile) { progress ->
                _downloadState.value = DownloadState.Downloading(progress / 2) // First half of progress
            }

            // Then download vocabulary
            downloadFileWithProgress(VOCAB_URL, vocabFile) { progress ->
                _downloadState.value = DownloadState.Downloading(50 + progress / 2) // Second half of progress
            }

            _downloadState.value = DownloadState.Success

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model: ${e.message}", e)
            _downloadState.value = DownloadState.Error("Download failed: ${e.message}")
        }
    }

    private suspend fun downloadFileWithProgress(
        url: String,
        destination: File,
        progressCallback: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000 // 30 seconds
        connection.readTimeout = 30000

        try {
            val contentLength = connection.contentLength

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            progressCallback(progress)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        object Success : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}