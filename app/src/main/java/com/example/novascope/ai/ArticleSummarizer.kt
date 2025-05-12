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
import org.tensorflow.lite.support.label.Category // Import Category class
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Implementation of the SmolLM integration for article summarization.
 * This uses a small language model that can run locally on the device.
 */
class ArticleSummarizer(private val context: Context) {
    companion object {
        private const val TAG = "ArticleSummarizer"
        private const val MODEL_FILE = "smollm_article_summarizer.tflite"
        private const val MAX_TOKENS = 512
        private const val SUMMARY_LENGTH = 150
    }

    private var modelInitialized = false
    private lateinit var classifier: BertNLClassifier

    // Initialize the model
    suspend fun initializeModel() = withContext(Dispatchers.IO) {
        try {
            if (!modelInitialized) {
                // Copy model from assets to internal storage if needed
                val modelFile = File(context.filesDir, MODEL_FILE)
                if (!modelFile.exists()) {
                    context.assets.open(MODEL_FILE).use { inputStream ->
                        FileOutputStream(modelFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                // Create the classifier options
                val options = BertNLClassifier.BertNLClassifierOptions.builder()
                    .setMaxSeqLen(MAX_TOKENS)
                    .build()

                // Load the model
                classifier = BertNLClassifier.createFromFileAndOptions(context, modelFile.absolutePath, options)
                modelInitialized = true
                Log.d(TAG, "Model initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            throw e
        }
    }

    // Generate summary from article content
    suspend fun summarizeArticle(newsItem: NewsItem): Flow<SummaryState> = flow {
        emit(SummaryState.Loading)

        try {
            // Ensure model is initialized
            if (!modelInitialized) {
                initializeModel()
            }

            val content = newsItem.content ?: newsItem.title
            if (content.isNullOrBlank()) {
                emit(SummaryState.Error("No content available to summarize"))
                return@flow
            }

            // Process and tokenize the text
            val processedText = preprocessText(content)

            withContext(Dispatchers.Default) {
                // Generate summary
                val result: List<Category> = classifier.classify(processedText) // Explicitly declare type

                // Post-process the summary
                // FIX: Access the label property of the first Category object in the result list
                val summaryText = postprocessSummary(result[0].label)

                emit(SummaryState.Success(summaryText))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error summarizing article", e)
            emit(SummaryState.Error("Error generating summary: ${e.message}"))
        }
    }

    // Preprocess text for the model
    private fun preprocessText(text: String): String {
        // Trim to max token length to avoid OOM errors
        return text.take(MAX_TOKENS * 4) // Approximate character limit
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    // Post-process model output
    private fun postprocessSummary(rawSummary: String): String {
        // Clean up the raw model output
        val trimmedSummary = rawSummary.trim()
        val takenSummary: String = trimmedSummary.take(SUMMARY_LENGTH)
        return takenSummary.plus("...")
    }

    // Fallback method when model can't be loaded
    suspend fun generateFallbackSummary(newsItem: NewsItem): String {
        return withContext(Dispatchers.Default) {
            val title = newsItem.title
            val content = newsItem.content ?: ""

            // Simple extractive summarization as fallback
            val sentences = content.split(". ")
            val topSentences = sentences.take(2).joinToString(". ")

            "Summary: $title. $topSentences."
        }
    }

    // Clean up resources
    fun close() {
        if (modelInitialized) {
            try {
                classifier.close()
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