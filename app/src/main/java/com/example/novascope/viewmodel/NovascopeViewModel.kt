// app/src/main/java/com/example/novascope/viewmodel/NovascopeViewModel.kt
package com.example.novascope.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novascope.ai.ArticleSummarizer
import com.example.novascope.ai.ModelFileManager
import com.example.novascope.ai.SummaryState
import com.example.novascope.data.FeedRepository
import com.example.novascope.data.RssService
import com.example.novascope.model.Feed
import com.example.novascope.model.FeedCategory
import com.example.novascope.model.NewsItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.random.Random
import com.example.novascope.ai.T5DebugHelper

class NovascopeViewModel(private val context: Context) : ViewModel() {

    // Services
    private val rssService = RssService()
    private val feedRepository = FeedRepository(context)
    private val articleSummarizer = ArticleSummarizer(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("novascope_settings", Context.MODE_PRIVATE)

    // Job management
    private var loadFeedsJob: Job? = null
    private var summaryJob: Job? = null
    private var modelInitJob: Job? = null

    // UI state
    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val newsItems: List<NewsItem> = emptyList(),
        val bookmarkedItems: List<NewsItem> = emptyList(),
        val errorMessage: String? = null,
        val selectedArticle: NewsItem? = null,
        val summaryState: SummaryState = SummaryState.Loading,
        val modelImportState: ModelFileManager.ImportState = ModelFileManager.ImportState.Idle
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Feeds with SharedFlow for better performance
    private val _feeds = MutableSharedFlow<List<Feed>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val feeds: SharedFlow<List<Feed>> = _feeds.asSharedFlow()

    // Settings with persistence
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Bookmarked IDs for faster lookup
    private val bookmarkedIds = mutableSetOf<String>()

    // Activity reference (no longer needed for file picker but kept for compatibility)
    private var activity: ComponentActivity? = null

    init {
        // Debug T5 model setup
        Log.d("ViewModel", "ViewModel initialization started")

        // Run T5 debug check
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val debugInfo = T5DebugHelper.debugT5Model(context)
                Log.d("ViewModel", "T5 Debug Info:\n$debugInfo")

                val testInfo = T5DebugHelper.testT5Initialization(context)
                Log.d("ViewModel", "T5 Test Info:\n$testInfo")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error in T5 debug check: ${e.message}", e)
            }
        }

        // Initialize feeds
        viewModelScope.launch {
            feedRepository.feeds.collect { latestFeeds ->
                _feeds.emit(latestFeeds)
                if (_uiState.value.newsItems.isEmpty()) {
                    loadFeeds()
                }
            }
        }

        // Initialize AI model in background with automatic download
        // Delay this slightly to let the app settle first
        viewModelScope.launch {
            delay(3000) // Wait 3 seconds before starting model initialization
            initializeAiModelInBackground()
        }

        // Monitor model import state
        viewModelScope.launch {
            articleSummarizer.importState.collect { state ->
                Log.d("ViewModel", "Model import state changed: $state")
                _uiState.update { it.copy(modelImportState = state) }
            }
        }

        // Watch settings changes and save them
        viewModelScope.launch {
            settings.collect { newSettings ->
                saveSettings(newSettings)
            }
        }
    }

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings(): AppSettings {
        return AppSettings(
            themeMode = ThemeMode.values()[prefs.getInt("theme_mode", ThemeMode.SYSTEM.ordinal)],
            useDynamicColor = prefs.getBoolean("dynamic_color", true),
            enableAiSummary = prefs.getBoolean("ai_summary", true),
            enableNotifications = prefs.getBoolean("notifications", true),
            enableWebScraping = prefs.getBoolean("web_scraping", true), // Default enabled
            textSize = TextSize.values()[prefs.getInt("text_size", TextSize.MEDIUM.ordinal)]
        )
    }

    /**
     * Save settings to SharedPreferences
     */
    private fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putInt("theme_mode", settings.themeMode.ordinal)
            putBoolean("dynamic_color", settings.useDynamicColor)
            putBoolean("ai_summary", settings.enableAiSummary)
            putBoolean("notifications", settings.enableNotifications)
            putBoolean("web_scraping", settings.enableWebScraping)
            putInt("text_size", settings.textSize.ordinal)
            apply()
        }
    }

    /**
     * Update a specific setting
     */
    fun updateSetting(update: (AppSettings) -> AppSettings) {
        _settings.update(update)
    }

    /**
     * Initialize AI model in background with automatic download
     */
    private fun initializeAiModelInBackground() {
        modelInitJob?.cancel()
        modelInitJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ViewModel", "Starting AI model initialization")

                // Give the app some time to settle before initializing the model
                delay(2000)

                // Attempt to initialize the model (will auto-download if needed)
                val success = withTimeoutOrNull(60000L) { // Increased to 60 second timeout
                    articleSummarizer.initializeModel()
                } ?: false

                if (success) {
                    Log.d("ViewModel", "AI model initialization completed successfully")

                    // Update UI state to indicate model is ready
                    _uiState.update {
                        it.copy(modelImportState = ModelFileManager.ImportState.Success)
                    }
                } else {
                    Log.w("ViewModel", "AI model initialization failed or timed out")

                    // Update UI state to show model not imported
                    _uiState.update {
                        it.copy(modelImportState = ModelFileManager.ImportState.Error("Model initialization failed"))
                    }
                }

            } catch (e: Exception) {
                Log.e("ViewModel", "Error initializing AI model: ${e.message}", e)

                // Update UI state to show error
                _uiState.update {
                    it.copy(modelImportState = ModelFileManager.ImportState.Error("Initialization error: ${e.message}"))
                }
            }
        }
    }

    fun registerActivity(activity: ComponentActivity) {
        this.activity = activity
        // No longer need file picker launcher for automatic T5 download
    }

    fun unregisterActivity() {
        activity = null
    }

    /**
     * Trigger T5 model download
     */
    fun downloadAiModel() {
        modelInitJob?.cancel()
        modelInitJob = viewModelScope.launch {
            try {
                Log.d("ViewModel", "Starting T5 model download")

                // Update UI state immediately to show download starting
                _uiState.update { it.copy(modelImportState = ModelFileManager.ImportState.Importing(0)) }

                // Start monitoring the import state from the summarizer
                val progressJob = launch {
                    articleSummarizer.importState.collect { state ->
                        Log.d("ViewModel", "Import state update: $state")
                        _uiState.update { it.copy(modelImportState = state) }
                    }
                }

                val success = withTimeoutOrNull(120000L) { // 2 minutes timeout for download
                    articleSummarizer.initializeModel()
                } ?: false

                if (success) {
                    Log.d("ViewModel", "T5 model download and initialization successful")

                    // If there's a selected article, generate summary
                    _uiState.value.selectedArticle?.let { article ->
                        delay(500) // Small delay to ensure model is ready
                        generateSummary(article)
                    }
                } else {
                    Log.e("ViewModel", "T5 model download failed or timed out")
                    _uiState.update {
                        it.copy(modelImportState = ModelFileManager.ImportState.Error("Download failed or timed out"))
                    }
                }

                progressJob.cancel()

            } catch (e: Exception) {
                Log.e("ViewModel", "Error downloading T5 model: ${e.message}", e)
                _uiState.update {
                    it.copy(modelImportState = ModelFileManager.ImportState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    /**
     * Cancel model download
     */
    fun cancelModelDownload() {
        modelInitJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(modelImportState = ModelFileManager.ImportState.Idle) }
        }
    }

    /**
     * Legacy method for compatibility - now triggers automatic download
     */
    fun launchModelFilePicker() {
        downloadAiModel()
    }

    fun loadFeeds(forceRefresh: Boolean = false) {
        loadFeedsJob?.cancel()
        loadFeedsJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isRefreshing = forceRefresh,
                        isLoading = !forceRefresh && it.newsItems.isEmpty(),
                        errorMessage = null
                    )
                }

                if (forceRefresh) {
                    rssService.clearCache()
                }

                val enabledFeeds = feedRepository.getEnabledFeeds()
                Log.d("ViewModel", "Loading ${enabledFeeds.size} enabled feeds")

                if (enabledFeeds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            newsItems = emptyList(),
                            errorMessage = "No RSS feeds configured. Go to Explore to add some feeds."
                        )
                    }
                    return@launch
                }

                // Pass web scraping setting to RSS service
                val webScrapingEnabled = _settings.value.enableWebScraping
                Log.d("ViewModel", "Web scraping enabled: $webScrapingEnabled")

                // Process feeds in parallel with better error handling
                val feedResults = coroutineScope {
                    enabledFeeds.map { feed ->
                        async(Dispatchers.IO) {
                            try {
                                Log.d("ViewModel", "Fetching feed: ${feed.name} (${feed.url})")
                                val items = if (webScrapingEnabled) {
                                    rssService.fetchFeedWithScraping(feed.url, forceRefresh)
                                } else {
                                    rssService.fetchFeed(feed.url, forceRefresh)
                                }
                                Log.d("ViewModel", "Feed ${feed.name} returned ${items.size} items")

                                FeedResult.Success(
                                    feed = feed,
                                    items = items.map { it.copy(feedId = feed.id) }
                                )
                            } catch (e: Exception) {
                                Log.e("ViewModel", "Error loading feed ${feed.name}: ${e.message}", e)
                                FeedResult.Error(feed = feed, error = e.message ?: "Unknown error")
                            }
                        }
                    }.awaitAll()
                }

                // Separate successful results from errors
                val successfulResults = feedResults.filterIsInstance<FeedResult.Success>()
                val errorResults = feedResults.filterIsInstance<FeedResult.Error>()

                // Collect all news items from successful feeds
                val allNewsItems = successfulResults.flatMap { it.items }
                Log.d("ViewModel", "Total items collected: ${allNewsItems.size}")

                // Process and update UI with improved sorting
                val sortedItems = if (allNewsItems.isNotEmpty()) {
                    smartSortNewsItems(allNewsItems)
                        .mapIndexed { index, item ->
                            item.copy(
                                isBookmarked = bookmarkedIds.contains(item.id),
                                isBigArticle = index == 0
                            )
                        }
                } else {
                    emptyList()
                }

                val bookmarkedItems = sortedItems.filter { it.isBookmarked }

                // Prepare error message if some feeds failed
                val errorMessage = if (errorResults.isNotEmpty()) {
                    val failedFeeds = errorResults.map { it.feed.name }
                    "Some feeds failed to load: ${failedFeeds.joinToString(", ")}"
                } else null

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        newsItems = sortedItems,
                        bookmarkedItems = bookmarkedItems,
                        errorMessage = errorMessage
                    )
                }

                Log.d("ViewModel", "Feed loading completed. ${sortedItems.size} items loaded.")

            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading feeds: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Error loading feeds: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Improved news sorting that balances recency with diversity
     */
    private fun smartSortNewsItems(items: List<NewsItem>): List<NewsItem> {
        if (items.isEmpty()) return items

        val currentTime = System.currentTimeMillis()
        val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds

        // Group items by feed to ensure diversity
        val itemsByFeed = items.groupBy { it.feedId }
        val sortedItems = mutableListOf<NewsItem>()

        // First, add the most recent item from each feed to ensure diversity
        itemsByFeed.values.forEach { feedItems ->
            val mostRecent = feedItems.maxByOrNull { it.publishTimeMillis }
            mostRecent?.let { sortedItems.add(it) }
        }

        // Then add remaining items with weighted randomization
        val remainingItems = items.filter { item ->
            !sortedItems.any { it.id == item.id }
        }.map { item ->
            // Calculate age-based weight (newer articles get higher weight)
            val age = currentTime - item.publishTimeMillis
            val normalizedAge = (age.toDouble() / maxAge).coerceIn(0.0, 1.0)

            // Weight formula: newer articles get higher weight + random factor
            val isVeryRecent = age < (6 * 60 * 60 * 1000L) // 6 hours
            val recencyBoost = if (isVeryRecent) 0.3 else 0.0

            val weight = (1.0 - normalizedAge * 0.7) + recencyBoost + (Random.nextDouble() * 0.4)

            item to weight
        }.sortedByDescending { it.second }
            .map { it.first }

        // Combine the lists: recent items from each feed first, then weighted remaining items
        sortedItems.addAll(remainingItems)

        return sortedItems.distinctBy { it.id } // Remove any duplicates
    }

    // Helper class to handle feed loading results
    private sealed class FeedResult {
        data class Success(val feed: Feed, val items: List<NewsItem>) : FeedResult()
        data class Error(val feed: Feed, val error: String) : FeedResult()
    }

    /**
     * Refresh feeds (called by pull-to-refresh)
     */
    fun refreshFeeds() {
        loadFeeds(forceRefresh = true)
    }

    fun toggleBookmark(newsItemId: String) {
        if (bookmarkedIds.contains(newsItemId)) {
            bookmarkedIds.remove(newsItemId)
        } else {
            bookmarkedIds.add(newsItemId)
        }

        _uiState.update { state ->
            val updatedItems = state.newsItems.map {
                if (it.id == newsItemId) it.copy(isBookmarked = !it.isBookmarked) else it
            }
            val updatedBookmarks = updatedItems.filter { it.isBookmarked }
            val updatedSelected = state.selectedArticle?.let {
                if (it.id == newsItemId) it.copy(isBookmarked = bookmarkedIds.contains(newsItemId)) else it
            }

            state.copy(
                newsItems = updatedItems,
                bookmarkedItems = updatedBookmarks,
                selectedArticle = updatedSelected
            )
        }
    }

    fun addFeed(name: String, url: String, category: FeedCategory) {
        viewModelScope.launch {
            val newFeed = Feed(
                id = UUID.randomUUID().toString(),
                name = name,
                url = url,
                category = category,
                isEnabled = true
            )
            feedRepository.addFeed(newFeed)
        }
    }

    fun deleteFeed(feedId: String) {
        viewModelScope.launch {
            feedRepository.deleteFeed(feedId)
            _uiState.update { state ->
                state.copy(
                    newsItems = state.newsItems.filter { it.feedId != feedId },
                    bookmarkedItems = state.bookmarkedItems.filter { it.feedId != feedId }
                )
            }
        }
    }

    fun toggleFeedEnabled(feedId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            feedRepository.toggleFeedEnabled(feedId, isEnabled)
            if (!isEnabled) {
                _uiState.update { state ->
                    state.copy(
                        newsItems = state.newsItems.filter { it.feedId != feedId },
                        bookmarkedItems = state.bookmarkedItems.filter { it.feedId != feedId }
                    )
                }
            }
        }
    }

    suspend fun getFeedInfoFromUrl(url: String): String {
        return try {
            withTimeoutOrNull(3000L) {
                rssService.fetchFeed(url).firstOrNull()?.sourceName
            } ?: url
        } catch (e: Exception) {
            url
        }
    }

    fun selectArticle(articleId: String) {
        summaryJob?.cancel()

        val article = _uiState.value.newsItems.find { it.id == articleId }
            ?: _uiState.value.bookmarkedItems.find { it.id == articleId }

        if (article != null) {
            _uiState.update { it.copy(selectedArticle = article) }

            if (_settings.value.enableAiSummary) {
                summaryJob = viewModelScope.launch {
                    generateSummary(article)
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    summaryState = SummaryState.Error("Article not found"),
                    selectedArticle = null
                )
            }
        }
    }

    private suspend fun generateSummary(newsItem: NewsItem) {
        _uiState.update { it.copy(summaryState = SummaryState.Loading) }

        try {
            Log.d("ViewModel", "Starting summary generation for: ${newsItem.title}")

            if (!articleSummarizer.isModelImported) {
                Log.d("ViewModel", "T5 model not available, showing ModelNotImported state")
                _uiState.update { it.copy(summaryState = SummaryState.ModelNotImported) }
                return
            }

            withTimeoutOrNull(30000L) { // 30 second timeout for summary generation
                articleSummarizer.summarizeArticle(newsItem).collect { summaryState ->
                    Log.d("ViewModel", "Summary state: $summaryState")
                    _uiState.update { it.copy(summaryState = summaryState) }
                }
            } ?: run {
                Log.w("ViewModel", "Summary generation timed out")
                _uiState.update {
                    it.copy(summaryState = SummaryState.Error("Summary generation timed out"))
                }
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "Error generating summary: ${e.message}", e)
            _uiState.update {
                it.copy(summaryState = SummaryState.Error("Error: ${e.message}"))
            }
        }
    }

    /**
     * Legacy method - now triggers automatic download
     */
    private suspend fun importModelFromUri(uri: Uri) {
        downloadAiModel()
    }

    fun cancelModelImport() {
        cancelModelDownload()
    }

    fun isModelImported(): Boolean {
        return try {
            articleSummarizer.isModelImported
        } catch (e: Exception) {
            Log.e("ViewModel", "Error checking model import status: ${e.message}")
            false
        }
    }

    /**
     * Clear all caches
     */
    fun clearAllCaches() {
        viewModelScope.launch {
            rssService.clearCache()
            Log.d("ViewModel", "All caches cleared")
        }
    }

    /**
     * Toggle web scraping setting
     */
    fun toggleWebScraping(enabled: Boolean) {
        updateSetting { it.copy(enableWebScraping = enabled) }
        // Optionally refresh feeds to apply the new setting immediately
        if (_uiState.value.newsItems.isNotEmpty()) {
            refreshFeeds()
        }
    }

    /**
     * Toggle AI summary setting
     */
    fun toggleAiSummary(enabled: Boolean) {
        updateSetting { it.copy(enableAiSummary = enabled) }
    }

    /**
     * Toggle notifications setting
     */
    fun toggleNotifications(enabled: Boolean) {
        updateSetting { it.copy(enableNotifications = enabled) }
    }

    /**
     * Toggle dynamic color setting
     */
    fun toggleDynamicColor(enabled: Boolean) {
        updateSetting { it.copy(useDynamicColor = enabled) }
    }

    /**
     * Update theme mode
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        updateSetting { it.copy(themeMode = themeMode) }
    }

    /**
     * Update text size
     */
    fun updateTextSize(textSize: TextSize) {
        updateSetting { it.copy(textSize = textSize) }
    }

    /**
     * Get current web scraping status
     */
    fun isWebScrapingEnabled(): Boolean {
        return _settings.value.enableWebScraping
    }

    /**
     * Get current AI summary status
     */
    fun isAiSummaryEnabled(): Boolean {
        return _settings.value.enableAiSummary
    }

    /**
     * Perform a manual refresh of all feeds
     */
    fun performManualRefresh() {
        viewModelScope.launch {
            try {
                rssService.clearCache()
                loadFeeds(forceRefresh = true)
                Log.d("ViewModel", "Manual refresh completed")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error during manual refresh: ${e.message}", e)
                _uiState.update {
                    it.copy(errorMessage = "Refresh failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Clear specific feed cache
     */
    fun clearFeedCache(feedUrl: String) {
        viewModelScope.launch {
            rssService.clearCache(feedUrl)
            Log.d("ViewModel", "Cache cleared for feed: $feedUrl")
        }
    }

    /**
     * Get feed statistics
     */
    fun getFeedStatistics(): Map<String, Any> {
        val currentState = _uiState.value
        return mapOf(
            "totalArticles" to currentState.newsItems.size,
            "bookmarkedArticles" to currentState.bookmarkedItems.size,
            "totalFeeds" to (_feeds.replayCache.firstOrNull()?.size ?: 0),
            "enabledFeeds" to feedRepository.getEnabledFeeds().size,
            "webScrapingEnabled" to _settings.value.enableWebScraping,
            "aiSummaryEnabled" to _settings.value.enableAiSummary
        )
    }

    /**
     * Debug method to get current state information
     */
    fun getDebugInfo(): String {
        val currentState = _uiState.value
        val currentSettings = _settings.value

        return buildString {
            appendLine("=== NovascopeViewModel Debug Info ===")
            appendLine("Loading: ${currentState.isLoading}")
            appendLine("Refreshing: ${currentState.isRefreshing}")
            appendLine("News Items: ${currentState.newsItems.size}")
            appendLine("Bookmarked Items: ${currentState.bookmarkedItems.size}")
            appendLine("Error Message: ${currentState.errorMessage}")
            appendLine("Selected Article: ${currentState.selectedArticle?.title}")
            appendLine("Summary State: ${currentState.summaryState}")
            appendLine("Model Import State: ${currentState.modelImportState}")
            appendLine("")
            appendLine("=== Settings ===")
            appendLine("Theme Mode: ${currentSettings.themeMode}")
            appendLine("Dynamic Color: ${currentSettings.useDynamicColor}")
            appendLine("AI Summary: ${currentSettings.enableAiSummary}")
            appendLine("Notifications: ${currentSettings.enableNotifications}")
            appendLine("Web Scraping: ${currentSettings.enableWebScraping}")
            appendLine("Text Size: ${currentSettings.textSize}")
            appendLine("")
            appendLine("=== Jobs Status ===")
            appendLine("Load Feeds Job Active: ${loadFeedsJob?.isActive}")
            appendLine("Summary Job Active: ${summaryJob?.isActive}")
            appendLine("Model Init Job Active: ${modelInitJob?.isActive}")
            appendLine("=== End Debug Info ===")
        }
    }

    /**
     * Clean up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        loadFeedsJob?.cancel()
        summaryJob?.cancel()
        modelInitJob?.cancel()
        articleSummarizer.close()
        Log.d("ViewModel", "ViewModel resources cleaned up")
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val enableAiSummary: Boolean = true,
    val enableNotifications: Boolean = true,
    val enableWebScraping: Boolean = true, // New setting
    val textSize: TextSize = TextSize.MEDIUM
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class TextSize(val scaleFactor: Float) {
    SMALL(0.8f), MEDIUM(1f), LARGE(1.2f)
}