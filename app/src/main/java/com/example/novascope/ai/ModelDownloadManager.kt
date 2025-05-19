// app/src/main/java/com/example/novascope/ai/ModelDownloadManager.kt
package com.example.novascope.ai

import android.content.Context
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class ModelDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloadManager"
        const val MODEL_FILE = "smollm2_article_summarizer.gguf"

        // Updated URLs with the new repository and specific q2_K model file
        // This is a quantized version that should be smaller and faster
        const val MODEL_URL = "https://huggingface.co/QuantFactory/SmolLM2-135M-GGUF/resolve/main/SmolLm2-135m-q2_K.gguf"
        const val VOCAB_FILE = "smollm2_vocab.txt"

        // Increase required space to 100MB to be safe
        private const val MIN_REQUIRED_SPACE = 100 * 1024 * 1024L
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Flag to track if a download is in progress
    private var isDownloading = false

    val isModelDownloaded: Boolean
        get() {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)
            return modelFile.exists() && vocabFile.exists() && modelFile.length() > 1000000 // At least 1MB
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
            _downloadState.value = DownloadState.Error("Not enough storage space. Free up at least 100MB and try again.")
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

            // Log attempt to download
            Log.d(TAG, "Attempting to download model from: $MODEL_URL")

            // First download model (larger file)
            downloadFileWithProgress(MODEL_URL, modelFile) { progress ->
                _downloadState.value = DownloadState.Downloading(progress / 2) // First half of progress
                Log.d(TAG, "Download progress: $progress%")
            }

            // Add a simple placeholder vocabulary file for now
            createPlaceholderVocabFile(vocabFile)
            _downloadState.value = DownloadState.Downloading(100) // Complete

            // Final check to ensure files were downloaded properly
            if (modelFile.exists() && modelFile.length() > 0 &&
                vocabFile.exists() && vocabFile.length() > 0) {
                _downloadState.value = DownloadState.Success
                Log.d(TAG, "Model download successful: ${modelFile.length()} bytes")
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

    // Create a simple vocabulary file as a fallback
    private fun createPlaceholderVocabFile(vocabFile: File) {
        try {
            vocabFile.createNewFile()
            vocabFile.writeText("""
                <unk>
                <s>
                </s>
                <pad>
                the
                a
                an
                is
                was
                to
                of
                in
                and
                for
                on
                at
                with
                by
                from
                about
                that
                it
                as
                this
                which
                but
                or
                not
                have
                has
                had
                are
                be
                been
                will
                would
                can
                could
                may
                might
                should
                must
                their
                there
                they
                them
                he
                him
                his
                she
                her
                hers
                you
                your
                yours
                we
                us
                our
                ours
                i
                me
                my
                mine
            """.trimIndent())
            Log.d(TAG, "Created placeholder vocabulary file")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating placeholder vocab file", e)
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
        if (isDownloading) {
            isDownloading = false
            _downloadState.value = DownloadState.Idle
            cleanupPartialDownloads()
        }
    }

    private suspend fun downloadFileWithProgress(
        url: String,
        destination: File,
        progressCallback: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 120000 // 120 seconds - increased timeout
        connection.readTimeout = 120000    // 120 seconds - increased timeout

        try {
            // Add User-Agent header to avoid being blocked by Hugging Face
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            connection.setRequestProperty("Accept", "*/*")

            Log.d(TAG, "Connection established, getting response code...")
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                Log.e(TAG, "Server returned HTTP ${responseCode}: ${connection.responseMessage}, Details: $errorMessage")
                throw IOException("Server returned HTTP ${responseCode}: ${connection.responseMessage}")
            }

            val contentLength = connection.contentLength
            Log.d(TAG, "Content length: $contentLength bytes")

            if (contentLength <= 0) {
                Log.w(TAG, "Content length not available, proceeding anyway")
            }

            // Create a temporary file first
            val tempFile = File(destination.absolutePath + ".tmp")
            tempFile.createNewFile()

            Log.d(TAG, "Starting to download file...")
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var totalBytesRead = 0L
                    var lastProgressUpdate = 0
                    var bytesRead: Int = -1

                    // Read from the input stream and write to the output file
                    while (isActive && isDownloading && input.read(buffer).also { bytesRead = it } != -1) {
                        // Check if download was canceled
                        if (!isDownloading) {
                            throw CancellationException("Download was cancelled")
                        }

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Calculate progress
                        val progress = if (contentLength > 0) {
                            (totalBytesRead * 100 / contentLength).toInt()
                        } else {
                            // If content length is unknown, use a dummy progress indicator
                            (totalBytesRead / 1024 / 10).toInt().coerceAtMost(99)
                        }

                        // Only update progress if it changed significantly (reduces UI updates)
                        if (progress - lastProgressUpdate >= 1 || progress == 100) {
                            progressCallback(progress)
                            lastProgressUpdate = progress
                            Log.d(TAG, "Download progress: $progress% ($totalBytesRead bytes)")
                        }
                    }
                }
            }

            // Move the temp file to the final destination
            if (destination.exists()) {
                destination.delete()
            }

            if (isDownloading && tempFile.renameTo(destination)) {
                Log.d(TAG, "Download completed successfully: ${destination.absolutePath} (${destination.length()} bytes)")
            } else {
                throw IOException("Failed to finalize downloaded file or download was cancelled")
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