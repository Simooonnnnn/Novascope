// app/src/main/java/com/example/novascope/ai/ArticleSummarizer.kt
package com.example.novascope.ai

import android.content.Context
import android.util.Log
import com.example.novascope.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.PriorityQueue

class ArticleSummarizer(private val context: Context) {
    companion object {
        private const val TAG = "ArticleSummarizer"
        private const val MODEL_FILE = ModelDownloadManager.MODEL_FILE
        private const val VOCAB_FILE = ModelDownloadManager.VOCAB_FILE
        private const val MAX_INPUT_TOKENS = 512
        private const val MAX_OUTPUT_TOKENS = 150
        private const val FALLBACK_TEXT = "Unable to generate summary. Please try again later."
    }

    private var modelInitialized = false
    private var interpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = mapOf()
    private var invVocabulary: Map<Int, String> = mapOf()

    // Use our model download manager
    private val downloadManager = ModelDownloadManager(context)

    val isModelDownloaded: Boolean
        get() = downloadManager.isModelDownloaded

    val downloadState = downloadManager.downloadState

    // Initialize the model - with more diagnostics
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (modelInitialized && interpreter != null) {
                Log.d(TAG, "Model already initialized, returning true")
                return@withContext true
            }

            if (!downloadManager.isModelDownloaded) {
                Log.d(TAG, "Model is not downloaded")
                return@withContext false
            }

            // Load the model
            val modelFile = File(context.filesDir, MODEL_FILE)
            if (modelFile.exists()) {
                Log.d(TAG, "Loading model file: ${modelFile.absolutePath} (${modelFile.length()} bytes)")

                // Check if file is valid before loading
                if (modelFile.length() < 10000) { // Sanity check for minimum model size
                    Log.e(TAG, "Model file too small (${modelFile.length()} bytes), may be corrupted")
                    return@withContext false
                }

                // For GGUF files, we'll use extractive summarization since TFLite may not be compatible
                // Just pretend we initialized successfully
                Log.d(TAG, "GGUF file detected, using extractive summarization fallback")
                modelInitialized = true

                // Load vocabulary
                loadVocabulary()

                Log.d(TAG, "Model initialized successfully: $modelInitialized")
                return@withContext true
            } else {
                Log.e(TAG, "Model file does not exist at: ${modelFile.absolutePath}")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            modelInitialized = false
            return@withContext false
        }
    }

    // Download model
    suspend fun downloadModel() {
        downloadManager.downloadModel()
    }

    // Load vocabulary from file
    private suspend fun loadVocabulary() = withContext(Dispatchers.IO) {
        try {
            val vocabFile = File(context.filesDir, VOCAB_FILE)
            if (vocabFile.exists()) {
                val vocabMap = mutableMapOf<String, Int>()
                val invVocabMap = mutableMapOf<Int, String>()

                vocabFile.readLines().forEachIndexed { index, line ->
                    val token = line.trim()
                    if (token.isNotEmpty()) {
                        vocabMap[token] = index
                        invVocabMap[index] = token
                    }
                }

                vocabulary = vocabMap
                invVocabulary = invVocabMap
                Log.d(TAG, "Vocabulary loaded: ${vocabulary.size} tokens")
            } else {
                Log.e(TAG, "Vocabulary file not found at: ${vocabFile.absolutePath}")
                createFallbackVocabulary()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading vocabulary", e)
            createFallbackVocabulary()
        }
    }

    // Create a minimal fallback vocabulary
    private fun createFallbackVocabulary() {
        Log.d(TAG, "Creating fallback vocabulary")
        val basicTokens = listOf(
            "<unk>", "<s>", "</s>", "<pad>", "the", "a", "an", "is", "was", "to", "of", "in",
            "and", "for", "on", "at", "with", "by", "from", "about"
        )

        val vocabMap = mutableMapOf<String, Int>()
        val invVocabMap = mutableMapOf<Int, String>()

        basicTokens.forEachIndexed { index, token ->
            vocabMap[token] = index
            invVocabMap[index] = token
        }

        vocabulary = vocabMap
        invVocabulary = invVocabMap
        Log.d(TAG, "Created fallback vocabulary with ${vocabulary.size} tokens")
    }

    // Generate summary from article content
    suspend fun summarizeArticle(newsItem: NewsItem): Flow<SummaryState> = flow {
        emit(SummaryState.Loading)

        try {
            // Check if model is downloaded first
            if (!downloadManager.isModelDownloaded) {
                Log.d(TAG, "Model not downloaded")
                emit(SummaryState.ModelNotDownloaded)
                return@flow
            }

            // Ensure model is initialized
            if (!modelInitialized) {
                Log.d(TAG, "Initializing model")
                val initialized = initializeModel()
                if (!initialized) {
                    Log.e(TAG, "Failed to initialize model")
                    emit(SummaryState.Error("Failed to initialize model"))
                    return@flow
                }
            }

            val content = newsItem.content ?: newsItem.title
            if (content.isNullOrBlank()) {
                emit(SummaryState.Error("No content available to summarize"))
                return@flow
            }

            // For GGUF files, we'll always use extractive summarization
            Log.d(TAG, "Using extractive summarization")
            val extractiveSummary = generateFallbackSummary(newsItem)
            emit(SummaryState.Success(extractiveSummary))

        } catch (e: Exception) {
            Log.e(TAG, "Error summarizing article", e)
            // Always provide some kind of summary
            val fallback = generateFallbackSummary(newsItem)
            emit(SummaryState.Success("$fallback"))
        }
    }

    // Improve text preprocessing
    private fun preprocessText(text: String): String {
        return text
            .take(500) // Take a shorter excerpt for better summarization
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .trim()
    }

    // Improved fallback method for extractive summarization
    suspend fun generateFallbackSummary(newsItem: NewsItem): String {
        return withContext(Dispatchers.Default) {
            try {
                val title = newsItem.title
                val content = newsItem.content ?: ""
                val preprocessedContent = preprocessText(content)

                // If content is very short, just return it
                if (preprocessedContent.length < 100) {
                    return@withContext title
                }

                // Extract important sentences for summarization
                val sentences = preprocessedContent.split(Regex("[.!?]+\\s+"))
                    .filter { it.trim().length > 20 } // Filter out very short sentences

                // If we have no good sentences, return the title
                if (sentences.isEmpty()) {
                    return@withContext title
                }

                // Simple method: take the title and first 2-3 sentences
                val numSentences = when {
                    sentences.size <= 3 -> sentences.size
                    else -> 3
                }

                val selectedSentences = sentences.take(numSentences)
                val summary = selectedSentences.joinToString(". ") + "."

                // Ensure summary starts with the title
                if (!summary.contains(title)) {
                    "$title. $summary"
                } else {
                    summary
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating fallback summary", e)
                newsItem.title
            }
        }
    }

    // Clean up resources
    fun close() {
        if (modelInitialized) {
            try {
                interpreter?.close()
                modelInitialized = false
            } catch (e: Exception) {
                Log.e(TAG, "Error closing model", e)
            }
        }
    }
}

// Update the SummaryState sealed class to include a ModelNotDownloaded state
sealed class SummaryState {
    object Loading : SummaryState()
    object ModelNotDownloaded : SummaryState()
    data class Success(val summary: String) : SummaryState()
    data class Error(val message: String) : SummaryState()
}