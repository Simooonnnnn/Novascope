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

class ModelFileManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelFileManager"
        const val MODEL_FILE = "smollm2_article_summarizer.gguf"
        const val VOCAB_FILE = "smollm2_vocab.txt"

        // Required space for a typical GGUF file
        private const val MIN_REQUIRED_SPACE = 100 * 1024 * 1024L // 100MB
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // Flag to track if an import is in progress
    private var isImporting = false

    val isModelImported: Boolean
        get() {
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)
            return modelFile.exists() && vocabFile.exists() && modelFile.length() > 1000000 // At least 1MB
        }

    /**
     * Import a model file from a URI
     */
    suspend fun importModel(uri: Uri): Boolean {
        if (isModelImported) {
            _importState.value = ImportState.Success
            return true
        }

        // Check if already importing
        if (isImporting) {
            Log.d(TAG, "Import already in progress")
            return false
        }

        // Check available storage space
        if (!hasEnoughSpace()) {
            _importState.value = ImportState.Error("Not enough storage space. Free up at least 100MB and try again.")
            return false
        }

        isImporting = true
        _importState.value = ImportState.Importing(0)

        try {
            // Create directory if needed
            val assetsDir = File(context.filesDir, "assets")
            if (!assetsDir.exists()) {
                assetsDir.mkdirs()
            }

            // Set up destination files
            val modelFile = File(context.filesDir, MODEL_FILE)
            val vocabFile = File(context.filesDir, VOCAB_FILE)

            // Log attempt to import
            Log.d(TAG, "Attempting to import model from URI: $uri")

            // Copy the file from URI to internal storage
            val success = copyFileFromUri(uri, modelFile)
            if (!success) {
                throw IOException("Failed to copy file from URI")
            }

            // Add a simple placeholder vocabulary file
            createPlaceholderVocabFile(vocabFile)
            _importState.value = ImportState.Importing(100) // Complete

            // Final check to ensure files were imported properly
            if (modelFile.exists() && modelFile.length() > 0 &&
                vocabFile.exists() && vocabFile.length() > 0) {
                _importState.value = ImportState.Success
                Log.d(TAG, "Model import successful: ${modelFile.length()} bytes")
                return true
            } else {
                throw IOException("Imported files are invalid or empty")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error importing model: ${e.message}", e)
            _importState.value = ImportState.Error("Import failed: ${e.message ?: "Unknown error"}")
            // Delete partial files
            cleanupPartialImports()
            return false
        } finally {
            isImporting = false
        }
    }

    /**
     * Copy file from URI to destination
     */
    private suspend fun copyFileFromUri(uri: Uri, destination: File): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destination).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastProgressUpdate = 0

                    // Get input stream size if possible
                    val fileSize = try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val sizeIndex = cursor.getColumnIndex("_size")
                                if (sizeIndex != -1) cursor.getLong(sizeIndex) else -1L
                            } else -1L
                        } ?: -1L
                    } catch (e: Exception) {
                        -1L
                    }

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Calculate progress
                        val progress = if (fileSize > 0) {
                            (totalBytesRead * 100 / fileSize).toInt()
                        } else {
                            // If file size is unknown, use a dummy progress indicator
                            (totalBytesRead / 1024 / 10).toInt().coerceAtMost(99)
                        }

                        // Only update progress if it changed significantly (reduces UI updates)
                        if (progress - lastProgressUpdate >= 1 || progress == 100) {
                            _importState.value = ImportState.Importing(progress)
                            lastProgressUpdate = progress
                            Log.d(TAG, "Import progress: $progress% ($totalBytesRead bytes)")
                        }
                    }
                }
            } ?: throw IOException("Could not open input stream for URI")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file: ${e.message}", e)
            false
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

    private fun cleanupPartialImports() {
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
            Log.e(TAG, "Error cleaning up partial imports", e)
        }
    }

    // Cancel the current import
    fun cancelImport() {
        Log.d(TAG, "Cancelling model import")
        isImporting = false
        _importState.value = ImportState.Idle
        cleanupPartialImports()
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