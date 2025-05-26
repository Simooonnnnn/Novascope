// app/src/main/java/com/example/novascope/ai/ModelFileManager.kt
package com.example.novascope.ai

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ModelFileManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelFileManager"
        const val MODEL_FILE = "t5_small_summarizer.tflite"
        const val VOCAB_FILE = "t5_vocab.txt"

        // T5-small model optimized for mobile (quantized)
        private const val MODEL_URL = "https://tfhub.dev/tensorflow/lite-model/t5-small/1?lite-format=tflite"
        // Alternative: Use a direct URL to a pre-converted model
        // private const val MODEL_URL = "https://github.com/tensorflow/text/releases/download/v2.8.1/t5_small_summarizer.tflite"

        // Required space for T5-small model (~25MB)
        private const val MIN_REQUIRED_SPACE = 50 * 1024 * 1024L // 50MB
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private var isDownloading = false

    val isModelImported: Boolean
        get() {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)
            return modelFile.exists() && vocabFile.exists() && modelFile.length() > 1000000 // At least 1MB
        }

    /**
     * Automatically download T5 model if not present
     */
    suspend fun downloadModelIfNeeded(): Boolean {
        if (isModelImported) {
            _importState.value = ImportState.Success
            return true
        }

        if (isDownloading) {
            Log.d(TAG, "Download already in progress")
            return false
        }

        // Check available storage space
        if (!hasEnoughSpace()) {
            _importState.value = ImportState.Error("Not enough storage space. Free up at least 50MB and try again.")
            return false
        }

        isDownloading = true
        _importState.value = ImportState.Importing(0)

        try {
            Log.d(TAG, "Starting automatic T5 model download...")

            // Set up destination files
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)

            // Download the T5 model
            val success = downloadModelFromUrl(MODEL_URL, modelFile)
            if (!success) {
                throw IOException("Failed to download T5 model")
            }

            // Create T5 vocabulary file
            createT5VocabFile(vocabFile)
            _importState.value = ImportState.Importing(100)

            // Final verification
            if (modelFile.exists() && modelFile.length() > 0 &&
                vocabFile.exists() && vocabFile.length() > 0) {
                _importState.value = ImportState.Success
                Log.d(TAG, "T5 model download successful: ${modelFile.length()} bytes")
                return true
            } else {
                throw IOException("Downloaded files are invalid or empty")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading T5 model: ${e.message}", e)
            _importState.value = ImportState.Error("Download failed: ${e.message ?: "Unknown error"}")
            cleanupPartialDownloads()
            return false
        } finally {
            isDownloading = false
        }
    }

    /**
     * Download model from URL with progress tracking
     */
    private suspend fun downloadModelFromUrl(url: String, destination: File): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading from: $url")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error: ${connection.responseCode}")
                return@withContext false
            }

            val fileSize = connection.contentLength
            Log.d(TAG, "Model size: ${fileSize / 1024 / 1024}MB")

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastProgressUpdate = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Update progress
                        val progress = if (fileSize > 0) {
                            (totalBytesRead * 100 / fileSize).toInt()
                        } else {
                            (totalBytesRead / 1024 / 100).toInt().coerceAtMost(99)
                        }

                        if (progress - lastProgressUpdate >= 5) {
                            _importState.value = ImportState.Importing(progress)
                            lastProgressUpdate = progress
                            Log.d(TAG, "Download progress: $progress% (${totalBytesRead / 1024 / 1024}MB)")
                        }
                    }
                }
            }

            connection.disconnect()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model: ${e.message}", e)
            false
        }
    }

    /**
     * Create T5-specific vocabulary file
     */
    private fun createT5VocabFile(vocabFile: File) {
        try {
            vocabFile.createNewFile()
            // T5 uses SentencePiece tokenization, but for simplicity we'll create a basic vocab
            // In a production app, you'd want to include the actual T5 vocabulary
            vocabFile.writeText("""
                <pad>
                </s>
                <unk>
                <s>
                summarize:
                article
                news
                text
                the
                a
                an
                is
                was
                are
                were
                be
                been
                have
                has
                had
                do
                does
                did
                will
                would
                could
                should
                may
                might
                can
                must
                this
                that
                these
                those
                i
                you
                he
                she
                it
                we
                they
                me
                him
                her
                us
                them
                my
                your
                his
                her
                its
                our
                their
                mine
                yours
                hers
                ours
                theirs
                what
                which
                who
                whom
                whose
                where
                when
                why
                how
                and
                or
                but
                so
                if
                because
                although
                though
                while
                since
                after
                before
                during
                for
                in
                on
                at
                by
                with
                from
                to
                of
                about
                over
                under
                through
                between
                among
                against
                without
                within
                .
                ,
                !
                ?
                :
                ;
                "
                '
                (
                )
                [
                ]
                {
                }
            """.trimIndent())
            Log.d(TAG, "Created T5 vocabulary file")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating T5 vocab file", e)
        }
    }

    /**
     * Legacy method for manual import (now redirects to automatic download)
     */
    suspend fun importModel(uri: Uri): Boolean {
        return downloadModelIfNeeded()
    }

    private fun cleanupPartialDownloads() {
        try {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)

            if (modelFile.exists() && !isModelImported) {
                modelFile.delete()
            }

            if (vocabFile.exists() && !isModelImported) {
                vocabFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up partial downloads", e)
        }
    }

    // Cancel the current download
    fun cancelDownload() {
        Log.d(TAG, "Cancelling model download")
        isDownloading = false
        _importState.value = ImportState.Idle
        cleanupPartialDownloads()
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

    sealed class ImportState {
        object Idle : ImportState()
        data class Importing(val progress: Int) : ImportState()
        object Success : ImportState()
        data class Error(val message: String) : ImportState()
    }
}