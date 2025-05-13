// app/src/main/java/com/example/novascope/ai/ArticleSummarizer.kt
package com.example.novascope.ai

import android.content.Context
import android.util.Log
import com.example.novascope.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.label.Category
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.util.PriorityQueue
import java.util.zip.ZipInputStream

/**
 * Implementation of SmolLM2-135M integration for article summarization.
 * This uses a small language model that can run locally on the device.
 */
class ArticleSummarizer(private val context: Context) {
    companion object {
        private const val TAG = "ArticleSummarizer"
        private const val MODEL_FILE = "smollm2_article_summarizer.tflite"
        private const val MODEL_URL = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M/resolve/main/model_optimized.tflite"
        private const val VOCAB_FILE = "smollm2_vocab.txt"
        private const val VOCAB_URL = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M/resolve/main/tokenizer.txt"
        private const val MAX_INPUT_TOKENS = 512
        private const val MAX_OUTPUT_TOKENS = 150
        private const val FALLBACK_TEXT = "Unable to generate summary. Please try again later."
    }

    private var modelInitialized = false
    private var interpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = mapOf()
    private var invVocabulary: Map<Int, String> = mapOf()

    // Initialize the model
    suspend fun initializeModel() = withContext(Dispatchers.IO) {
        try {
            if (!modelInitialized) {
                // Handle model downloading and initialization
                prepareModelFiles()

                // Load the model
                val modelFile = File(context.filesDir, MODEL_FILE)
                if (modelFile.exists()) {
                    val options = Interpreter.Options()
                    interpreter = Interpreter(modelFile, options)
                    modelInitialized = true
                    Log.d(TAG, "Model initialized successfully")
                } else {
                    Log.e(TAG, "Model file does not exist!")
                    throw Exception("Model file not found")
                }

                // Load vocabulary
                loadVocabulary()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            // Don't throw - let the app continue with fallback summarization
            modelInitialized = false
        }
    }

    // Download and prepare model files
    private suspend fun prepareModelFiles() = withContext(Dispatchers.IO) {
        try {
            // Download model if needed
            val modelFile = File(context.filesDir, MODEL_FILE)
            if (!modelFile.exists()) {
                try {
                    URL(MODEL_URL).openStream().use { inputStream ->
                        FileOutputStream(modelFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d(TAG, "Model downloaded successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download model: ${e.message}")
                    // Continue and see if we can use a fallback or bundled model
                }
            }

            // Download vocabulary if needed
            val vocabFile = File(context.filesDir, VOCAB_FILE)
            if (!vocabFile.exists()) {
                try {
                    URL(VOCAB_URL).openStream().use { inputStream ->
                        FileOutputStream(vocabFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d(TAG, "Vocabulary downloaded successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download vocabulary: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing model files", e)
        }
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
            // Ensure model is initialized
            if (!modelInitialized) {
                try {
                    initializeModel()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize model", e)
                    emit(SummaryState.Error("Failed to initialize model: ${e.message}"))
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

    // Generate summary using the TF Lite model
    private fun generateSummaryWithModel(text: String): String {
        // This is a simplified implementation - in a real app, you'd need proper tokenization and processing
        try {
            if (interpreter == null) return FALLBACK_TEXT

            // Tokenize input text
            val tokens = tokenize(text)
            val inputIds = tokens.map { vocabulary[it] ?: 0 }.take(MAX_INPUT_TOKENS).toIntArray()

            // Prepare input and output tensors
            val inputBuffer = ByteBuffer.allocateDirect(4 * inputIds.size)
            for (id in inputIds) {
                inputBuffer.putInt(id)
            }
            inputBuffer.rewind()

            // Create output buffer for the generated tokens
            val outputBuffer = ByteBuffer.allocateDirect(4 * MAX_OUTPUT_TOKENS)

            // Run inference
            val inputs = arrayOf(inputBuffer)
            val outputs = mapOf(0 to outputBuffer)

            interpreter?.runForMultipleInputsOutputs(inputs, outputs)

            // Process output
            outputBuffer.rewind()
            val outputIds = ArrayList<Int>(MAX_OUTPUT_TOKENS)
            for (i in 0 until MAX_OUTPUT_TOKENS) {
                val id = outputBuffer.getInt()
                outputIds.add(id)
                // Break on EOS token if present
                if (id == 1) break
            }

            // Convert ids back to tokens and join
            val outputTokens = outputIds.mapNotNull { invVocabulary[it] }
            return outputTokens.joinToString(" ")
        } catch (e: Exception) {
            Log.e(TAG, "Error in model inference", e)
            return FALLBACK_TEXT
        }
    }

    // Basic tokenization for text
    private fun tokenize(text: String): List<String> {
        // Simple whitespace tokenization - in a real app, you'd use a proper BPE tokenizer
        return text.split(Regex("\\s+"))
    }

    // Preprocess text for the model
    private fun preprocessText(text: String): String {
        // Clean and prepare text for the model
        return text.take(MAX_INPUT_TOKENS * 4) // Approximate character limit
            .replace("\\s+".toRegex(), " ")
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

// State class for summary generation
sealed class SummaryState {
    object Loading : SummaryState()
    data class Success(val summary: String) : SummaryState()
    data class Error(val message: String) : SummaryState()
}