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

        // Set to false to use real T5 model
        private const val USE_SIMULATED_AI = false
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

            // Auto-download model if needed
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

            if (USE_SIMULATED_AI) {
                // Fallback simulation
                Log.d(TAG, "Using simulated T5 model for demonstration")
                loadT5Vocabulary()
                modelInitialized = true
                return@withContext true
            } else {
                // Real TensorFlow Lite model loading
                val modelBuffer = loadModelFile(modelFile)
                val options = Interpreter.Options().apply {
                    setNumThreads(2)
                    setUseXNNPACK(true)
                    setUseNNAPI(false) // Disable NNAPI for better compatibility
                }

                interpreter = Interpreter(modelBuffer, options)
                loadT5Vocabulary()
                modelInitialized = true
                Log.d(TAG, "T5 model initialized successfully")
                return@withContext true
            }

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
     * Load comprehensive T5 vocabulary
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
                createComprehensiveT5Vocabulary()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading T5 vocabulary", e)
            createComprehensiveT5Vocabulary()
        }
    }

    /**
     * Create a more comprehensive T5 vocabulary for better tokenization
     */
    private fun createComprehensiveT5Vocabulary() {
        Log.d(TAG, "Creating comprehensive T5 vocabulary")

        val t5Tokens = mutableListOf<String>().apply {
            // Special tokens
            addAll(listOf("<pad>", "</s>", "<unk>", "<s>"))

            // Task prefix
            add("summarize:")

            // Common content words
            addAll(listOf(
                "article", "news", "text", "content", "story", "report", "information",
                "main", "key", "important", "according", "said", "says", "announced",
                "reported", "study", "research", "found", "shows", "indicates"
            ))

            // Function words
            addAll(listOf(
                "the", "a", "an", "and", "or", "but", "so", "if", "when", "where",
                "what", "who", "how", "why", "which", "that", "this", "these", "those"
            ))

            // Verbs
            addAll(listOf(
                "is", "was", "are", "were", "be", "been", "being", "have", "has", "had",
                "having", "do", "does", "did", "done", "doing", "will", "would", "could",
                "should", "may", "might", "can", "must", "shall"
            ))

            // Pronouns
            addAll(listOf(
                "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us",
                "them", "my", "your", "his", "her", "its", "our", "their"
            ))

            // Prepositions
            addAll(listOf(
                "for", "in", "on", "at", "by", "with", "from", "to", "of", "about",
                "over", "under", "through", "between", "among", "against", "without",
                "within", "during", "before", "after", "since", "until", "while"
            ))

            // Conjunctions and discourse markers
            addAll(listOf(
                "because", "although", "though", "unless", "whether", "as", "like",
                "than", "however", "therefore", "moreover", "furthermore", "meanwhile",
                "consequently", "nonetheless", "nevertheless"
            ))

            // Quantifiers and determiners
            addAll(listOf(
                "more", "most", "less", "least", "very", "quite", "rather", "really",
                "actually", "indeed", "certainly", "probably", "perhaps", "maybe",
                "definitely", "all", "some", "any", "many", "much", "few", "little",
                "several", "various", "different", "same", "other", "another", "each", "every"
            ))

            // Numbers
            addAll((0..100).map { it.toString() })
            addAll(listOf("hundred", "thousand", "million", "billion"))

            // Time expressions
            addAll(listOf(
                "year", "month", "week", "day", "hour", "minute", "second", "today",
                "yesterday", "tomorrow", "morning", "afternoon", "evening", "night"
            ))

            // Common nouns in news
            addAll(listOf(
                "government", "company", "business", "market", "economy", "financial",
                "political", "social", "economic", "technology", "science", "university",
                "school", "education", "health", "medical", "hospital", "doctor",
                "patient", "treatment", "disease", "country", "state", "city", "town",
                "people", "person", "man", "woman", "child", "family", "group", "team"
            ))

            // Punctuation
            addAll(listOf(".", ",", "!", "?", ":", ";", "\"", "'", "(", ")", "[", "]",
                "{", "}", "-", "—", "–", "/", "\\", "&", "%", "$", "#", "@",
                "*", "+", "=", "<", ">", "|", "~", "`", "^"))
        }

        val vocabMap = mutableMapOf<String, Int>()
        val invVocabMap = mutableMapOf<Int, String>()

        t5Tokens.forEachIndexed { index, token ->
            vocabMap[token] = index
            invVocabMap[index] = token
        }

        vocabulary = vocabMap
        invVocabulary = invVocabMap
        Log.d(TAG, "Created comprehensive T5 vocabulary with ${vocabulary.size} tokens")
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

            val summary = if (USE_SIMULATED_AI || interpreter == null) {
                generateFallbackSummary(newsItem)
            } else {
                try {
                    generateRealT5Summary(content)
                } catch (e: Exception) {
                    Log.e(TAG, "T5 inference failed, falling back to extractive: ${e.message}")
                    generateFallbackSummary(newsItem)
                }
            }

            emit(SummaryState.Success(summary))

        } catch (e: Exception) {
            Log.e(TAG, "Error generating T5 summary", e)
            // Fallback to extractive summary
            val fallbackSummary = generateFallbackSummary(newsItem)
            emit(SummaryState.Success(fallbackSummary))
        }
    }

    /**
     * Generate summary using actual T5 TensorFlow Lite model
     */
    private suspend fun generateRealT5Summary(content: String): String = withContext(Dispatchers.Default) {
        try {
            val interpreter = this@ArticleSummarizer.interpreter
                ?: throw IllegalStateException("T5 model not initialized")

            Log.d(TAG, "Starting real T5 inference")

            // Preprocess text for T5
            val preprocessedText = preprocessTextForT5(content)
            Log.d(TAG, "Preprocessed text length: ${preprocessedText.length}")

            // Tokenize input using our vocabulary
            val inputTokens = tokenizeForT5(preprocessedText)
            Log.d(TAG, "Input tokens: ${inputTokens.size}")

            // Prepare input tensor (encoder input)
            val inputBuffer = ByteBuffer.allocateDirect(4 * MAX_INPUT_LENGTH).apply {
                order(ByteOrder.nativeOrder())
                rewind()

                inputTokens.take(MAX_INPUT_LENGTH).forEach { token ->
                    putInt(token)
                }

                // Pad with PAD_TOKEN
                repeat(MAX_INPUT_LENGTH - inputTokens.size.coerceAtMost(MAX_INPUT_LENGTH)) {
                    putInt(PAD_TOKEN)
                }
                rewind()
            }

            // Prepare output tensor (decoder output)
            val outputBuffer = ByteBuffer.allocateDirect(4 * MAX_OUTPUT_LENGTH).apply {
                order(ByteOrder.nativeOrder())
            }

            // Run T5 inference
            Log.d(TAG, "Running T5 inference...")
            val startTime = System.currentTimeMillis()

            interpreter.run(inputBuffer, outputBuffer)

            val inferenceTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "T5 inference completed in ${inferenceTime}ms")

            // Extract output tokens
            outputBuffer.rewind()
            val outputTokens = mutableListOf<Int>()
            var tokenIndex = 0
            while (tokenIndex < MAX_OUTPUT_LENGTH) {
                val token = outputBuffer.int
                if (token == EOS_TOKEN) break
                if (token != PAD_TOKEN && token != START_TOKEN) {
                    outputTokens.add(token)
                }
                tokenIndex++
            }

            Log.d(TAG, "Output tokens: ${outputTokens.size}")

            // Detokenize output
            val summary = detokenizeFromT5(outputTokens)
            Log.d(TAG, "Generated T5 summary: $summary")

            return@withContext if (summary.isNotBlank() && summary.length > 10) {
                cleanupSummary(summary)
            } else {
                Log.w(TAG, "T5 generated empty or very short summary, using fallback")
                "T5 model generated a summary, but it was too short to be useful."
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in T5 inference", e)
            throw e
        }
    }

    /**
     * Improved preprocessing for T5
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
        return if (prefixedText.length > 1500) {
            // Take first 1500 chars to stay within token limits
            prefixedText.take(1500)
        } else {
            prefixedText
        }
    }

    /**
     * Improved tokenization for T5 using subword approach
     */
    private fun tokenizeForT5(text: String): List<Int> {
        val tokens = mutableListOf<Int>()

        // Add start token for decoder
        tokens.add(START_TOKEN)

        // Split into words and punctuation
        val words = text.lowercase()
            .replace(Regex("([.!?,:;\"'()\\[\\]{}])"), " $1 ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        for (word in words) {
            val tokenId = vocabulary[word] ?: run {
                // Try to handle unknown words by breaking them down
                val subTokens = handleUnknownWord(word)
                subTokens.forEach { subToken ->
                    tokens.add(vocabulary[subToken] ?: UNK_TOKEN)
                }
                //continue
            }
            //tokens.add(tokenId)

            if (tokens.size >= MAX_INPUT_LENGTH - 1) break
        }

        return tokens
    }

    /**
     * Handle unknown words by breaking them into subwords
     */
    private fun handleUnknownWord(word: String): List<String> {
        // Try common prefixes and suffixes
        val prefixes = listOf("un", "re", "pre", "dis", "over", "under", "out")
        val suffixes = listOf("ing", "ed", "er", "est", "ly", "tion", "ness", "ment")

        for (prefix in prefixes) {
            if (word.startsWith(prefix) && word.length > prefix.length + 2) {
                val remaining = word.substring(prefix.length)
                if (vocabulary.containsKey(remaining)) {
                    return listOf(prefix, remaining)
                }
            }
        }

        for (suffix in suffixes) {
            if (word.endsWith(suffix) && word.length > suffix.length + 2) {
                val base = word.substring(0, word.length - suffix.length)
                if (vocabulary.containsKey(base)) {
                    return listOf(base, suffix)
                }
            }
        }

        // If no subword found, return the original word
        return listOf(word)
    }

    /**
     * Detokenize T5 output tokens back to text
     */
    private fun detokenizeFromT5(tokens: List<Int>): String {
        val words = tokens
            .mapNotNull { tokenId ->
                invVocabulary[tokenId]?.takeIf {
                    it !in listOf("<pad>", "<s>", "</s>", "<unk>")
                }
            }

        if (words.isEmpty()) return ""

        return words.joinToString(" ")
            .replace(" .", ".")
            .replace(" ,", ",")
            .replace(" !", "!")
            .replace(" ?", "?")
            .replace(" :", ":")
            .replace(" ;", ";")
            .replace(" '", "'")
            .replace("( ", "(")
            .replace(" )", ")")
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
     * Enhanced fallback summarization
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

                // Extract key sentences using improved algorithm
                val sentences = preprocessedContent
                    .split(Regex("[.!?]+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.length > 15 && it.length < 200 }

                if (sentences.isEmpty()) {
                    return@withContext title
                }

                // Score sentences based on multiple factors
                val scoredSentences = scoreSentencesAdvanced(sentences, title)
                val selectedSentences = selectBestSentences(scoredSentences, 250)

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

    private fun scoreSentencesAdvanced(sentences: List<String>, title: String): List<Pair<String, Double>> {
        val titleWords = title.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val allWords = sentences.flatMap { it.lowercase().split(Regex("\\W+")) }
        val wordFreq = allWords.groupingBy { it }.eachCount()

        return sentences.map { sentence ->
            var score = 0.0
            val sentenceWords = sentence.lowercase().split(Regex("\\W+")).filter { it.length > 2 }

            // Title word overlap score (increased weight)
            val titleOverlap = sentenceWords.count { it in titleWords }.toDouble() / titleWords.size.coerceAtLeast(1)
            score += titleOverlap * 4.0

            // Position score (earlier sentences preferred)
            val position = sentences.indexOf(sentence)
            val positionScore = (sentences.size - position).toDouble() / sentences.size
            score += positionScore * 1.5

            // Word frequency score (avoid very common words)
            val avgWordFreq = sentenceWords.map { wordFreq[it] ?: 1 }.average()
            val freqScore = 1.0 / (1.0 + avgWordFreq / 10.0)
            score += freqScore * 0.5

            // Length score (prefer medium-length sentences)
            val lengthScore = when {
                sentence.length < 50 -> 0.3
                sentence.length in 50..150 -> 1.2
                sentence.length in 150..250 -> 1.0
                else -> 0.6
            }
            score += lengthScore

            // Content quality indicators
            val qualityIndicators = listOf(
                "according to", "reported", "announced", "study shows", "research indicates",
                "experts say", "officials said", "data shows", "analysis reveals"
            )
            if (qualityIndicators.any { sentence.lowercase().contains(it) }) {
                score += 2.0
            }

            // Numbers and statistics
            if (sentence.matches(Regex(".*\\d+.*"))) {
                score += 1.0
            }

            sentence to score
        }
    }

    private fun selectBestSentences(scoredSentences: List<Pair<String, Double>>, targetLength: Int): List<String> {
        val sortedSentences = scoredSentences.sortedByDescending { it.second }
        val selectedSentences = mutableListOf<String>()
        var currentLength = 0

        for ((sentence, score) in sortedSentences) {
            if (currentLength + sentence.length <= targetLength || selectedSentences.isEmpty()) {
                selectedSentences.add(sentence)
                currentLength += sentence.length
                if (selectedSentences.size >= 3) break
            }
        }

        // Return sentences in original order
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