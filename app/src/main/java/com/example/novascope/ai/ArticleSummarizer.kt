// Updates to app/src/main/java/com/example/novascope/ai/ArticleSummarizer.kt
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

    // Initialize the model
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (modelInitialized) return@withContext true

            if (!downloadManager.isModelDownloaded) {
                return@withContext false
            }

            // Load the model
            val modelFile = File(context.filesDir, MODEL_FILE)
            if (modelFile.exists()) {
                val options = Interpreter.Options()
                interpreter = Interpreter(modelFile, options)

                // Load vocabulary
                loadVocabulary()

                modelInitialized = true
                Log.d(TAG, "Model initialized successfully")
                return@withContext true
            } else {
                Log.e(TAG, "Model file does not exist!")
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
                    vocabMap[token] = index
                    invVocabMap[index] = token
                }

                vocabulary = vocabMap
                invVocabulary = invVocabMap
                Log.d(TAG, "Vocabulary loaded: ${vocabulary.size} tokens")
            } else {
                Log.e(TAG, "Vocabulary file not found!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading vocabulary", e)
            // Create a minimal fallback vocabulary
            vocabulary = mapOf()
            invVocabulary = mapOf()
        }
    }

    // Generate summary from article content
    suspend fun summarizeArticle(newsItem: NewsItem): Flow<SummaryState> = flow {
        emit(SummaryState.Loading)

        try {
            // Check if model is downloaded first
            if (!downloadManager.isModelDownloaded) {
                emit(SummaryState.ModelNotDownloaded)
                return@flow
            }

            // Ensure model is initialized
            if (!modelInitialized) {
                val initialized = initializeModel()
                if (!initialized) {
                    emit(SummaryState.Error("Failed to initialize model"))
                    return@flow
                }
            }

            val content = newsItem.content ?: newsItem.title
            if (content.isNullOrBlank()) {
                emit(SummaryState.Error("No content available to summarize"))
                return@flow
            }

            // For now, use a fallback approach if the model isn't properly initialized
            if (!modelInitialized || interpreter == null) {
                val fallbackSummary = generateFallbackSummary(newsItem)
                emit(SummaryState.Success(fallbackSummary))
                return@flow
            }

            // Process the text and generate summary
            val summary = withContext(Dispatchers.Default) {
                try {
                    val processedText = preprocessText(content)
                    generateSummaryWithModel(processedText)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating summary", e)
                    // Use fallback if model inference fails
                    generateFallbackSummary(newsItem)
                }
            }

            emit(SummaryState.Success(summary))

        } catch (e: Exception) {
            Log.e(TAG, "Error summarizing article", e)
            // Always provide some kind of summary
            val fallback = generateFallbackSummary(newsItem)
            emit(SummaryState.Success("$fallback (Error: ${e.message})"))
        }
    }

    // Rest of the methods remain the same...
    // (keeping the existing methods for generateSummaryWithModel, tokenize, preprocessText, etc.)

    // Generate summary using the TF Lite model
    private fun generateSummaryWithModel(text: String): String {
        try {
            if (interpreter == null) return FALLBACK_TEXT

            Log.d(TAG, "Starting model inference with SmolLM2")

            // Basic preprocessing - simplified for example
            // In a real implementation, you'd need proper BPE tokenization
            val cleanedText = preprocessText(text)
            Log.d(TAG, "Preprocessed text length: ${cleanedText.length}")

            // Simple word-level tokenization
            val tokens = tokenize(cleanedText)
            Log.d(TAG, "Tokenized to ${tokens.size} tokens")

            // Map tokens to vocabulary IDs
            val inputIds = tokens.map {
                vocabulary[it.lowercase()] ?: vocabulary["<unk>"] ?: 0
            }.take(MAX_INPUT_TOKENS).toIntArray()

            Log.d(TAG, "Input IDs: ${inputIds.take(10)}... (truncated)")

            // Create input tensor
            val inputBuffer = ByteBuffer.allocateDirect(4 * inputIds.size).order(ByteOrder.nativeOrder())
            for (id in inputIds) {
                inputBuffer.putInt(id)
            }
            inputBuffer.rewind()

            // Create output buffer for generated tokens
            val outputBuffer = ByteBuffer.allocateDirect(4 * MAX_OUTPUT_TOKENS).order(ByteOrder.nativeOrder())

            // Setup input/output
            val inputShape = intArrayOf(1, inputIds.size)
            val outputShape = intArrayOf(1, MAX_OUTPUT_TOKENS)

            // Run the model
            try {
                // Create input/output objects that match TF Lite requirements
                val inputs = arrayOf<Any>(inputBuffer)
                val outputs = mutableMapOf<Int, Any>()
                val inputsIndexes = intArrayOf(0)
                val outputsIndexes = intArrayOf(0)

                Log.d(TAG, "Running model inference...")
                interpreter?.runForMultipleInputsOutputs(inputs, outputs)
                Log.d(TAG, "Model inference completed")

                // Process output - decode the generated token IDs
                outputBuffer.rewind()
                val outputIds = ArrayList<Int>(MAX_OUTPUT_TOKENS)
                for (i in 0 until MAX_OUTPUT_TOKENS) {
                    val id = outputBuffer.getInt()
                    outputIds.add(id)
                    // Break on EOS token (usually token ID 1 or 2 in most models)
                    if (id == 1 || id == 2) break
                }

                // Convert ids back to tokens and join
                val outputTokens = outputIds.mapNotNull {
                    invVocabulary[it]?.replace("##", "")
                }

                val summary = outputTokens.joinToString(" ")
                    .replace(" ##", "")  // Fix wordpiece tokens
                    .replace(" .", ".")   // Fix spacing around punctuation
                    .replace(" ,", ",")
                    .replace(" !", "!")
                    .replace(" ?", "?")
                    .replace("  ", " ")   // Remove double spaces

                Log.d(TAG, "Generated summary: $summary")
                return summary
            } catch (e: Exception) {
                Log.e(TAG, "Error during model inference", e)
                return FALLBACK_TEXT
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in model inference", e)
            return FALLBACK_TEXT
        }
    }

    // Improve tokenization for better model performance
    private fun tokenize(text: String): List<String> {
        // A more robust approach would use a proper tokenizer like WordPiece or SentencePiece
        // This is a simplified version for example purposes
        return text.split(Regex("\\s+|(?=[.,!?])|(?<=[.,!?])"))
            .filter { it.isNotBlank() }
    }

    // Improve text preprocessing
    private fun preprocessText(text: String): String {
        return text
            .take(MAX_INPUT_TOKENS * 8) // Approximate character limit
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .trim()
    }

    // Fallback method for when the model can't be loaded
    suspend fun generateFallbackSummary(newsItem: NewsItem): String {
        return withContext(Dispatchers.Default) {
            val title = newsItem.title
            val content = newsItem.content ?: ""

            // Simple extractive summarization as fallback
            // Extract first sentence from title and first two sentences from content
            val titleSentence = title.split(". ").firstOrNull() ?: ""

            val contentSentences = content.split(". ")
                .filter { it.isNotBlank() }
                .take(3)
                .joinToString(". ")

            if (contentSentences.isNotBlank()) {
                "$titleSentence. $contentSentences."
            } else {
                titleSentence
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