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
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ArticleSummarizer(private val context: Context) {
    companion object {
        private const val TAG = "ArticleSummarizer"
        private const val MAX_INPUT_LENGTH = 512
        private const val MAX_OUTPUT_LENGTH = 128
        private const val SUMMARIZE_PREFIX = "summarize: "

        // T5 special tokens
        private const val PAD_TOKEN = 0
        private const val EOS_TOKEN = 1
        private const val UNK_TOKEN = 2
        private const val START_TOKEN = 3
    }

    private var modelInitialized = false
    private var interpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = mapOf()
    private var invVocabulary: Map<Int, String> = mapOf()

    // Use our model file manager with auto-download
    private val fileManager = ModelFileManager(context)

    val isModelImported: Boolean
        get() = fileManager.isModelImported

    val importState = fileManager.importState

    /**
     * Initialize T5 model with automatic download
     */
    suspend fun initializeModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (modelInitialized && interpreter != null) {
                Log.d(TAG, "T5 model already initialized")
                return@withContext true
            }

            Log.d(TAG, "Starting T5 model initialization...")

            // Auto-download model if needed (this will update the import state)
            val downloadSuccess = fileManager.downloadModelIfNeeded()
            if (!downloadSuccess) {
                Log.e(TAG, "Failed to download T5 model")
                return@withContext false
            }

            val modelFile = File(context.filesDir, ModelFileManager.MODEL_FILE)
            if (!modelFile.exists()) {
                Log.e(TAG, "T5 model file not found after download")
                return@withContext false
            }

            Log.d(TAG, "Loading T5 model: ${modelFile.absolutePath} (${modelFile.length()} bytes)")

            // Load the TFLite model
            val modelBuffer = loadModelFile(modelFile)
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Use 2 threads for better performance
                setUseXNNPACK(true) // Enable XNNPACK acceleration if available
            }

            interpreter = Interpreter(modelBuffer, options)

            // Load vocabulary
            loadT5Vocabulary()

            modelInitialized = true
            Log.d(TAG, "T5 model initialized successfully")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing T5 model", e)
            modelInitialized = false
            interpreter?.close()
            interpreter = null
            return@withContext false
        }
    }

    /**
     * Load model file into ByteBuffer
     */
    private fun loadModelFile(modelFile: File): MappedByteBuffer {
        val fileInputStream = FileInputStream(modelFile)
        val fileChannel = fileInputStream.channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Load T5 vocabulary from file
     */
    private suspend fun loadT5Vocabulary() = withContext(Dispatchers.IO) {
        try {
            val vocabFile = File(context.filesDir, ModelFileManager.VOCAB_FILE)
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
                Log.d(TAG, "T5 vocabulary loaded: ${vocabulary.size} tokens")
            } else {
                Log.e(TAG, "T5 vocabulary file not found")
                createFallbackVocabulary()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading T5 vocabulary", e)
            createFallbackVocabulary()
        }
    }

    /**
     * Create fallback vocabulary if main vocab file is unavailable
     */
    private fun createFallbackVocabulary() {
        Log.d(TAG, "Creating fallback T5 vocabulary")
        val basicTokens = listOf(
            "<pad>", "</s>", "<unk>", "<s>", "summarize:", "article", "news", "text",
            "the", "a", "an", "is", "was", "are", "were", "be", "been", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may", "might", "can", "must",
            "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they",
            "me", "him", "her", "us", "them", "my", "your", "his", "her", "its", "our", "their",
            "what", "which", "who", "whom", "where", "when", "why", "how",
            "and", "or", "but", "so", "if", "because", "although", "while", "since",
            "for", "in", "on", "at", "by", "with", "from", "to", "of", "about",
            ".", ",", "!", "?", ":", ";", "\"", "'", "(", ")", "[", "]"
        )

        val vocabMap = mutableMapOf<String, Int>()
        val invVocabMap = mutableMapOf<Int, String>()

        basicTokens.forEachIndexed { index, token ->
            vocabMap[token] = index
            invVocabMap[index] = token
        }

        vocabulary = vocabMap
        invVocabulary = invVocabMap
        Log.d(TAG, "Created fallback T5 vocabulary with ${vocabulary.size} tokens")
    }

    /**
     * Generate summary using T5 model
     */
    suspend fun summarizeArticle(newsItem: NewsItem): Flow<SummaryState> = flow {
        emit(SummaryState.Loading)

        try {
            // Check if model is imported
            if (!fileManager.isModelImported) {
                Log.d(TAG, "T5 model not imported, attempting auto-download")
                emit(SummaryState.Loading)

                val downloaded = fileManager.downloadModelIfNeeded()
                if (!downloaded) {
                    emit(SummaryState.ModelNotImported)
                    return@flow
                }
            }

            // Ensure model is initialized
            if (!modelInitialized) {
                Log.d(TAG, "Initializing T5 model")
                val initialized = initializeModel()
                if (!initialized) {
                    Log.e(TAG, "Failed to initialize T5 model")
                    emit(SummaryState.Error("Failed to initialize T5 model"))
                    return@flow
                }
            }

            val content = newsItem.content ?: newsItem.title
            if (content.isNullOrBlank()) {
                emit(SummaryState.Error("No content available to summarize"))
                return@flow
            }

            Log.d(TAG, "Generating T5 summary for: ${newsItem.title}")
            val summary = generateT5Summary(content)
            emit(SummaryState.Success(summary))

        } catch (e: Exception) {
            Log.e(TAG, "Error generating T5 summary", e)
            // Fallback to extractive summary
            val fallbackSummary = generateFallbackSummary(newsItem)
            emit(SummaryState.Success(fallbackSummary))
        }
    }

    /**
     * Generate summary using T5 TensorFlow Lite model
     */
    private suspend fun generateT5Summary(content: String): String = withContext(Dispatchers.Default) {
        try {
            val interpreter = this@ArticleSummarizer.interpreter ?: throw IllegalStateException("T5 model not initialized")

            // Preprocess text for T5
            val preprocessedText = preprocessTextForT5(content)
            Log.d(TAG, "Preprocessed text: ${preprocessedText.take(100)}...")

            // Tokenize input
            val inputTokens = tokenizeForT5(preprocessedText)
            Log.d(TAG, "Input tokens: ${inputTokens.size}")

            // Prepare input tensor
            val inputBuffer = ByteBuffer.allocateDirect(4 * MAX_INPUT_LENGTH).apply {
                order(ByteOrder.nativeOrder())
                rewind()

                inputTokens.forEachIndexed { index, token ->
                    if (index < MAX_INPUT_LENGTH) {
                        putInt(token)
                    }
                }

                // Pad with PAD_TOKEN
                repeat(MAX_INPUT_LENGTH - inputTokens.size.coerceAtMost(MAX_INPUT_LENGTH)) {
                    putInt(PAD_TOKEN)
                }
                rewind()
            }

            // Prepare output tensor
            val outputBuffer = ByteBuffer.allocateDirect(4 * MAX_OUTPUT_LENGTH).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run inference
            interpreter.run(inputBuffer, outputBuffer)

            // Extract output tokens
            outputBuffer.rewind()
            val outputTokens = mutableListOf<Int>()
            repeat(MAX_OUTPUT_LENGTH) {
                val token = outputBuffer.int
                if (token == EOS_TOKEN) {

                }
                if (token != PAD_TOKEN) {
                    outputTokens.add(token)
                }
            }

            // Detokenize output
            val summary = detokenizeFromT5(outputTokens)
            Log.d(TAG, "Generated T5 summary: $summary")

            return@withContext if (summary.isNotBlank()) {
                cleanupSummary(summary)
            } else {
                "Unable to generate summary using T5 model."
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in T5 inference", e)
            throw e
        }
    }

    /**
     * Preprocess text for T5 input
     */
    private fun preprocessTextForT5(text: String): String {
        val cleanText = text
            .replace(Regex("<[^>]*>"), "") // Remove HTML tags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Add T5 task prefix and limit length
        val prefixedText = "$SUMMARIZE_PREFIX$cleanText"
        return if (prefixedText.length > 2000) {
            prefixedText.take(2000)
        } else {
            prefixedText
        }
    }

    /**
     * Simple tokenization for T5 (word-based with fallback)
     */
    private fun tokenizeForT5(text: String): List<Int> {
        val tokens = mutableListOf<Int>()

        // Add start token
        tokens.add(START_TOKEN)

        // Simple word-based tokenization
        val words = text.lowercase()
            .split(Regex("[\\s\\p{Punct}]+"))
            .filter { it.isNotBlank() }

        for (word in words) {
            val tokenId = vocabulary[word] ?: vocabulary[word.take(10)] ?: UNK_TOKEN
            tokens.add(tokenId)

            if (tokens.size >= MAX_INPUT_LENGTH - 1) break
        }

        return tokens
    }

    /**
     * Detokenize T5 output tokens back to text
     */
    private fun detokenizeFromT5(tokens: List<Int>): String {
        return tokens
            .mapNotNull { tokenId -> invVocabulary[tokenId] }
            .filter { it != "<pad>" && it != "<s>" && it != "</s>" }
            .joinToString(" ")
            .replace(" .", ".")
            .replace(" ,", ",")
            .replace(" !", "!")
            .replace(" ?", "?")
            .replace(" :", ":")
            .replace(" ;", ";")
            .trim()
    }

    /**
     * Clean up and post-process the generated summary
     */
    private fun cleanupSummary(summary: String): String {
        return summary
            .replace(SUMMARIZE_PREFIX, "")
            .split(". ")
            .joinToString(". ") { sentence ->
                sentence.trim().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
            .let { if (it.endsWith(".")) it else "$it." }
            .take(300) // Limit summary length
    }

    /**
     * Fallback extractive summarization method
     */
    suspend fun generateFallbackSummary(newsItem: NewsItem): String {
        return withContext(Dispatchers.Default) {
            try {
                val title = newsItem.title.trim()
                val content = newsItem.content ?: ""

                if (content.isBlank()) {
                    return@withContext if (title.length > 100) {
                        title.take(97) + "..."
                    } else {
                        title
                    }
                }

                val preprocessedContent = preprocessTextForT5(content.replace(SUMMARIZE_PREFIX, ""))

                if (preprocessedContent.length < 150) {
                    return@withContext if (preprocessedContent != title) {
                        "$title - $preprocessedContent"
                    } else {
                        title
                    }
                }

                // Extract key sentences
                val sentences = preprocessedContent
                    .split(Regex("[.!?]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.length > 15 && it.length < 200 }

                if (sentences.isEmpty()) {
                    return@withContext title
                }

                // Score and select best sentences
                val scoredSentences = scoreSentences(sentences, title)
                val selectedSentences = selectBestSentences(scoredSentences, 200)

                val summary = selectedSentences.joinToString(". ") + "."
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

    private fun scoreSentences(sentences: List<String>, title: String): List<Pair<String, Double>> {
        val titleWords = title.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()

        return sentences.map { sentence ->
            var score = 0.0
            val sentenceWords = sentence.lowercase().split(Regex("\\W+")).filter { it.length > 2 }

            // Title word overlap score
            val titleOverlap = sentenceWords.count { it in titleWords }.toDouble() / titleWords.size.coerceAtLeast(1)
            score += titleOverlap * 3.0

            // Position score (earlier sentences preferred)
            val position = sentences.indexOf(sentence)
            val positionScore = (sentences.size - position).toDouble() / sentences.size
            score += positionScore * 1.0

            // Length score
            val lengthScore = when {
                sentence.length < 50 -> 0.5
                sentence.length > 200 -> 0.7
                else -> 1.0
            }
            score += lengthScore

            sentence to score
        }
    }

    private fun selectBestSentences(scoredSentences: List<Pair<String, Double>>, targetLength: Int): List<String> {
        val sortedSentences = scoredSentences.sortedByDescending { it.second }
        val selectedSentences = mutableListOf<String>()
        var currentLength = 0

        for ((sentence, _) in sortedSentences) {
            if (currentLength + sentence.length <= targetLength || selectedSentences.isEmpty()) {
                selectedSentences.add(sentence)
                currentLength += sentence.length
                if (selectedSentences.size >= 3) break
            }
        }

        return selectedSentences.sortedBy { sentence ->
            scoredSentences.indexOfFirst { it.first == sentence }
        }
    }

    /**
     * Import model (now redirects to auto-download)
     */
    suspend fun importModel(uri: Uri): Boolean {
        return fileManager.downloadModelIfNeeded()
    }

    /**
     * Clean up resources
     */
    fun close() {
        if (modelInitialized) {
            try {
                interpreter?.close()
                modelInitialized = false
                Log.d(TAG, "T5 model resources cleaned up")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing T5 model", e)
            }
        }
    }
}

sealed class SummaryState {
    object Loading : SummaryState()
    object ModelNotImported : SummaryState()
    data class Success(val summary: String) : SummaryState()
    data class Error(val message: String) : SummaryState()
}