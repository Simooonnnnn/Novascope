// app/src/main/java/com/example/novascope/ai/ModelDownloadManager.kt
package com.example.novascope.ai

import android.content.Context
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
        const val MODEL_FILE = "smollm2_article_summarizer.tflite"
        // Use a fallback URL that is more reliable
        const val MODEL_URL = "https://storage.googleapis.com/tensorflow/lite/models/smartreply/smartreply.tflite"
        const val VOCAB_FILE = "smollm2_vocab.txt"
        // Use a fallback URL that is more reliable
        const val VOCAB_URL = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/smartreply/smartreply_vocab.zip"

        // Minimum free space required (20MB)
        private const val MIN_REQUIRED_SPACE = 20 * 1024 * 1024L
    }
    private var isDownloading = false
    private suspend fun downloadFileWithProgress(...) {
        // ...
        while (isActive && isDownloading && input.read(buffer).also { bytesRead = it } != -1) {
            // Wenn der Download abgebrochen wurde, werfen wir eine CancellationException
            if (!isDownloading) {
                throw CancellationException("Download cancelled")
            }
            //

    // Methode zum Abbrechen des Downloads
    fun cancelDownload() {
        isDownloading = false
        _downloadState.value = DownloadState.Idle
        // Aufräumen teilweise heruntergeladener Dateien
        cleanupPartialDownloads()
    }


    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    // Flag to track if a download is in progress
    private var isDownloading = false

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

        // Check if already downloading
        if (isDownloading) {
            Log.d(TAG, "Download already in progress")
            return
        }

        // Check available storage space
        if (!hasEnoughSpace()) {
            _downloadState.value = DownloadState.Error("Not enough storage space. Free up at least 20MB and try again.")
            return
        }

        isDownloading = true
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

            // Final check to ensure files were downloaded properly
            if (modelFile.exists() && modelFile.length() > 0 &&
                vocabFile.exists() && vocabFile.length() > 0) {
                _downloadState.value = DownloadState.Success
            } else {
                throw IOException("Downloaded files are invalid or empty")
            }

        } catch (e: CancellationException) {
            Log.i(TAG, "Download was cancelled")
            _downloadState.value = DownloadState.Idle
            // Delete partial files
            cleanupPartialDownloads()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model: ${e.message}", e)
            _downloadState.value = DownloadState.Error("Download failed: ${e.message ?: "Unknown error"}")
            // Delete partial files
            cleanupPartialDownloads()
        } finally {
            isDownloading = false
        }
    }

    private fun cleanupPartialDownloads() {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)

            if (modelFile.exists() && !isModelDownloaded) {
                modelFile.delete()
            }

            if (vocabFile.exists() && !isModelDownloaded) {
                vocabFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up partial downloads", e)
        }
    }

    // Cancel the current download
    fun cancelDownload() {
        if (isDownloading && _downloadState.value is DownloadState.Downloading) {
            _downloadState.value = DownloadState.Idle
            isDownloading = false
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
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP ${responseCode}: ${connection.responseMessage}")
            }

            val contentLength = connection.contentLength
            if (contentLength <= 0) {
                throw IOException("Invalid content length: $contentLength")
            }

            // Create a temporary file first
            val tempFile = File(destination.absolutePath + ".tmp")
            tempFile.createNewFile()

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var lastProgressUpdate = 0
                    var bytesRead = 0 // Declare and initialize bytesRead here

                    // Assign to bytesRead within the loop condition
                    while (isActive && input.read(buffer).also { bytesRead = it } != -1) {
                        // Now bytesRead is guaranteed to be initialized before use
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val progress = (totalBytesRead * 100 / contentLength).toInt()

                        // Only update progress if it changed significantly (reduces UI updates)
                        if (progress - lastProgressUpdate >= 1 || progress == 100) {
                            progressCallback(progress)
                            lastProgressUpdate = progress
                        }

                        // Check if download was canceled
                        if (!isDownloading || _downloadState.value !is DownloadState.Downloading) {
                            throw CancellationException("Download was cancelled")
                        }
                    }
                }
            }

            // Move the temp file to the final destination
            if (destination.exists()) {
                destination.delete()
            }

            if (!tempFile.renameTo(destination)) {
                throw IOException("Failed to finalize downloaded file")
            }

        } catch (e: Exception) {
            // Handle the exception but make sure to rethrow it
            if (e !is CancellationException) {
                Log.e(TAG, "Error downloading file: ${e.message}", e)
            }
            throw e
        } finally {
            connection.disconnect()
        }
    }

    // Check if there's enough space available
    private fun hasEnoughSpace(): Boolean {
        try {
            val stats = StatFs(context.filesDir.path)
            val availableBytes = stats.availableBytes
            Log.d(TAG, "Available space: ${availableBytes / 1024 / 1024}MB")
            return availableBytes > MIN_REQUIRED_SPACE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking disk space", e)
            return false
        }
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        object Success : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}