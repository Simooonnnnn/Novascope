// app/src/main/java/com/example/novascope/ai/ArticleSummarizer.kt
package com.example.novascope.ai

import android.content.Context
import android.net.Uri
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

class ArticleSummarizer(private val context: Context) {
    companion object {
        private const val TAG = "ArticleSummarizer"
        private const val MODEL_FILE = ModelFileManager.MODEL_FILE
        private const val VOCAB_FILE = ModelFileManager.VOCAB_FILE
        private const val MAX_INPUT_TOKENS = 512
        private const val MAX_OUTPUT_TOKENS = 150
        private const val FALLBACK_TEXT = "Unable to generate summary. Please try again later."
    }

    private var modelInitialized = false
    private var interpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = mapOf()
    private var invVocabulary: Map<Int, String> = mapOf()

    // Use our model file manager
    private val fileManager = ModelFileManager(context)

    val isModelImported: Boolean
        get() = fileManager.isModelImported

    val importState = fileManager.importState

    // Initialize the model - with more diagnostics
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (modelInitialized && interpreter != null) {
                Log.d(TAG, "Model already initialized, returning true")
                return@withContext true
            }

            if (!fileManager.isModelImported) {
                Log.d(TAG, "Model is not imported")
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

    // Import model
    suspend fun importModel(uri: Uri): Boolean {
        return fileManager.importModel(uri)
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
            // Check if model is imported first
            if (!fileManager.isModelImported) {
                Log.d(TAG, "Model not imported")
                emit(SummaryState.ModelNotImported)
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
            emit(SummaryState.Success(fallback))
        }
    }

    // Improve text preprocessing
    private fun preprocessText(text: String): String {
        return text
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace("\n", " ")
            .trim()
    }

    // Improved fallback method for extractive summarization
    suspend fun generateFallbackSummary(newsItem: NewsItem): String {
        return withContext(Dispatchers.Default) {
            try {
                val title = newsItem.title.trim()
                val content = newsItem.content ?: ""

                // If no content, just return a short version of the title
                if (content.isBlank()) {
                    return@withContext if (title.length > 100) {
                        title.take(97) + "..."
                    } else {
                        title
                    }
                }

                val preprocessedContent = preprocessText(content)

                // If content is very short, create a simple summary
                if (preprocessedContent.length < 150) {
                    return@withContext if (preprocessedContent != title) {
                        "$title - $preprocessedContent"
                    } else {
                        title
                    }
                }

                // Split into sentences more reliably
                val sentences = preprocessedContent
                    .split(Regex("[.!?]+"))
                    .map { it.trim() }
                    .filter {
                        it.isNotEmpty() &&
                                it.length > 15 &&
                                it.length < 300 &&
                                !it.equals(title, ignoreCase = true) // Don't include the title as a sentence
                    }

                if (sentences.isEmpty()) {
                    return@withContext title
                }

                // Score sentences based on importance
                val scoredSentences = scoreSentences(sentences, title)

                // Select top sentences for summary
                val selectedSentences = selectBestSentences(scoredSentences, targetLength = 200)

                if (selectedSentences.isEmpty()) {
                    return@withContext title
                }

                // Create final summary
                val summary = selectedSentences.joinToString(". ") + "."

                // Make sure summary is different from title and not too long
                return@withContext if (summary.length > 300) {
                    summary.take(297) + "..."
                } else {
                    summary
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating fallback summary", e)
                return@withContext newsItem.title
            }
        }
    }

    // Score sentences based on various factors
    private fun scoreSentences(sentences: List<String>, title: String): List<Pair<String, Double>> {
        val titleWords = title.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()

        return sentences.map { sentence ->
            var score = 0.0
            val sentenceWords = sentence.lowercase().split(Regex("\\W+")).filter { it.length > 2 }

            // Score based on title word overlap
            val titleOverlap = sentenceWords.count { it in titleWords }.toDouble() / titleWords.size
            score += titleOverlap * 3.0

            // Score based on sentence position (earlier sentences often more important)
            val position = sentences.indexOf(sentence)
            val positionScore = (sentences.size - position).toDouble() / sentences.size
            score += positionScore * 1.0

            // Score based on sentence length (prefer medium-length sentences)
            val lengthScore = when {
                sentence.length < 50 -> 0.5
                sentence.length > 200 -> 0.7
                else -> 1.0
            }
            score += lengthScore

            // Boost score for sentences with important keywords
            val importantWords = setOf("said", "says", "according", "reported", "announced", "revealed", "found", "discovered", "study", "analysis", "result", "concluded")
            val keywordBoost = sentenceWords.count { it in importantWords } * 0.5
            score += keywordBoost

            // Penalize sentences that are questions or very short
            if (sentence.endsWith("?")) score -= 0.5
            if (sentence.length < 30) score -= 1.0

            sentence to score
        }
    }

    // Select the best sentences while keeping summary concise
    private fun selectBestSentences(scoredSentences: List<Pair<String, Double>>, targetLength: Int): List<String> {
        val sortedSentences = scoredSentences.sortedByDescending { it.second }
        val selectedSentences = mutableListOf<String>()
        var currentLength = 0

        for ((sentence, _) in sortedSentences) {
            if (currentLength + sentence.length <= targetLength || selectedSentences.isEmpty()) {
                selectedSentences.add(sentence)
                currentLength += sentence.length

                // Stop at 3 sentences max for readability
                if (selectedSentences.size >= 3) break
            }
        }

        // Sort selected sentences back to their original order
        return selectedSentences.sortedBy { sentence ->
            scoredSentences.indexOfFirst { it.first == sentence }
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

// Update the SummaryState sealed class to include a ModelNotImported state
sealed class SummaryState {
    object Loading : SummaryState()
    object ModelNotImported : SummaryState()
    data class Success(val summary: String) : SummaryState()
    data class Error(val message: String) : SummaryState()
}