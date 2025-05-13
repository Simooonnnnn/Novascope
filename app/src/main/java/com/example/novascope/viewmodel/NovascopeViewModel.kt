// app/src/main/java/com/example/novascope/viewmodel/NovascopeViewModel.kt
package com.example.novascope.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novascope.ai.ArticleSummarizer
import com.example.novascope.ai.SummaryState
import com.example.novascope.data.FeedRepository
import com.example.novascope.data.RssService
import com.example.novascope.model.Feed
import com.example.novascope.model.FeedCategory
import com.example.novascope.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class NovascopeViewModel(private val context: Context) : ViewModel() {

    // Services
    private val rssService = RssService()
    private val feedRepository = FeedRepository(context)
    private val articleSummarizer = ArticleSummarizer(context)

    // Concurrent tracking to prevent duplicate work
    private val feedsBeingFetched = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val isLoadingFeeds = AtomicBoolean(false)
    private var currentLoadJob: Job? = null

    // UI state with optimized updates
    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val newsItems: List<NewsItem> = emptyList(),
        val bookmarkedItems: List<NewsItem> = emptyList(),
        val errorMessage: String? = null,
        val selectedArticle: NewsItem? = null,
        val summaryState: SummaryState = SummaryState.Loading
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Feeds from repository
    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds: StateFlow<List<Feed>> = _feeds.asStateFlow()

    // Settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Articles cache to avoid recreating objects
    private val articlesCache = ConcurrentHashMap<String, NewsItem>()

    init {
        // Observe feeds from repository
        viewModelScope.launch {
            feedRepository.feeds.collect { latestFeeds ->
                _feeds.value = latestFeeds
                // Only reload feeds when necessary
                if (uiState.value.newsItems.isEmpty() && !isLoadingFeeds.get()) {
                    loadFeeds()
                }
            }
        }

        // Initialize the AI model in a non-blocking way
        viewModelScope.launch(Dispatchers.IO) {
            try {
                articleSummarizer.initializeModel()
            } catch (e: Exception) {
                Log.e("ViewModel", "Error initializing AI model: ${e.message}")
            }
        }
    }

    // Load all feeds with optimized parallel loading
    fun loadFeeds(forceRefresh: Boolean = false) {
        // Cancel any existing load job
        currentLoadJob?.cancel()

        // Set loading state atomically
        if (!isLoadingFeeds.compareAndSet(false, true)) {
            if (!forceRefresh) return  // Don't reload if already loading
        }

        currentLoadJob = viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                // Clear cache when forcing refresh
                rssService.clearCache()
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            try {
                val allItems = mutableListOf<NewsItem>()
                val enabledFeeds = feedRepository.getEnabledFeeds()

                // Process feeds in parallel
                val deferredItems = enabledFeeds.map { feed ->
                    async(Dispatchers.IO) {
                        try {
                            if (!feedsBeingFetched.add(feed.id)) {
                                return@async emptyList<NewsItem>() // Skip if already fetching
                            }

                            val items = rssService.fetchFeed(feed.url, forceRefresh)

                            // Add the feed ID to each item for tracking
                            items.map { it.copy(feedId = feed.id) }
                        } catch (e: Exception) {
                            Log.e("ViewModel", "Error loading feed ${feed.name}: ${e.message}")
                            emptyList<NewsItem>()
                        } finally {
                            feedsBeingFetched.remove(feed.id)
                        }
                    }
                }

                // Await all results
                val results = deferredItems.awaitAll()

                // Combine and dedup results
                val itemsMap = ConcurrentHashMap<String, NewsItem>()
                results.forEach { items ->
                    items.forEach { item ->
                        // Preserve bookmark status from cache
                        val existingItem = articlesCache[item.id]
                        if (existingItem != null && existingItem.isBookmarked) {
                            itemsMap[item.id] = item.copy(isBookmarked = true)
                        } else {
                            itemsMap[item.id] = item
                        }
                    }
                }

                // Update cache
                articlesCache.putAll(itemsMap)

                // Convert to list and sort
                allItems.addAll(itemsMap.values)
                val sortedItems = allItems.sortedByDescending { it.publishTimeMillis }

                // Mark first item as big article
                val displayItems = if (sortedItems.isNotEmpty()) {
                    listOf(sortedItems.first().copy(isBigArticle = true)) +
                            sortedItems.drop(1).map { it.copy(isBigArticle = false) }
                } else {
                    sortedItems
                }

                // Update bookmarked items list
                val bookmarkedItems = displayItems.filter { it.isBookmarked }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        newsItems = displayItems,
                        bookmarkedItems = bookmarkedItems,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading feeds: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Error loading feeds: ${e.message}"
                    )
                }
            } finally {
                isLoadingFeeds.set(false)
            }
        }
    }

    // Toggle bookmark with optimized state updates
    fun toggleBookmark(newsItemId: String) {
        val cachedItem = articlesCache[newsItemId] ?: return

        // Update cache directly for immediate response
        val updatedItem = cachedItem.copy(isBookmarked = !cachedItem.isBookmarked)
        articlesCache[newsItemId] = updatedItem

        _uiState.update { state ->
            // Update main items list efficiently
            val updatedItems = state.newsItems.map {
                if (it.id == newsItemId) updatedItem else it
            }

            // Update bookmarked items list
            val updatedBookmarks = if (updatedItem.isBookmarked) {
                state.bookmarkedItems + updatedItem
            } else {
                state.bookmarkedItems.filter { it.id != newsItemId }
            }

            state.copy(
                newsItems = updatedItems,
                bookmarkedItems = updatedBookmarks
            )
        }
    }

    // Get feeds by category
    fun getFeedsByCategory(category: FeedCategory): List<Feed> {
        return feedRepository.getFeedsByCategory(category)
    }

    // Add a new feed
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

    // Delete a feed
    fun deleteFeed(feedId: String) {
        viewModelScope.launch {
            feedRepository.deleteFeed(feedId)

            // Remove associated articles from UI to prevent showing deleted feed's content
            _uiState.update { state ->
                val updatedItems = state.newsItems.filter { it.feedId != feedId }
                val updatedBookmarks = state.bookmarkedItems.filter { it.feedId != feedId }

                state.copy(
                    newsItems = updatedItems,
                    bookmarkedItems = updatedBookmarks
                )
            }
        }
    }

    // Toggle feed enabled state
    fun toggleFeedEnabled(feedId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            feedRepository.toggleFeedEnabled(feedId, isEnabled)

            // If disabling a feed, remove its items from UI
            if (!isEnabled) {
                _uiState.update { state ->
                    val updatedItems = state.newsItems.filter { it.feedId != feedId }
                    val updatedBookmarks = state.bookmarkedItems.filter { it.feedId != feedId }

                    state.copy(
                        newsItems = updatedItems,
                        bookmarkedItems = updatedBookmarks
                    )
                }
            }
        }
    }

    // Get a feed by its URL with timeout
    suspend fun getFeedInfoFromUrl(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val items = rssService.fetchFeed(url)
                if (items.isNotEmpty()) {
                    items.first().sourceName
                } else {
                    url
                }
            } catch (e: Exception) {
                url
            }
        }
    }

    // Select article for detail view with optimized summary loading
    fun selectArticle(articleId: String) {
        val article = articlesCache[articleId] ?:
        _uiState.value.newsItems.find { it.id == articleId }

        if (article != null) {
            _uiState.update { it.copy(selectedArticle = article) }

            // Generate AI summary if feature is enabled
            if (_settings.value.enableAiSummary) {
                generateSummary(article)
            }
        }
    }

    // Generate summary with local AI and caching
    private fun generateSummary(newsItem: NewsItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(summaryState = SummaryState.Loading) }

            try {
                articleSummarizer.summarizeArticle(newsItem).collect { summaryState ->
                    _uiState.update { it.copy(summaryState = summaryState) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        summaryState = SummaryState.Error("Error generating summary: ${e.message}")
                    )
                }

                // Try fallback summary
                try {
                    val fallbackSummary = articleSummarizer.generateFallbackSummary(newsItem)
                    _uiState.update {
                        it.copy(
                            summaryState = SummaryState.Success(fallbackSummary)
                        )
                    }
                } catch (e: Exception) {
                    // Keep the error state
                }
            }
        }
    }

    // Update app settings
    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
    }

    // Clean up resources
    override fun onCleared() {
        super.onCleared()
        currentLoadJob?.cancel()
        articleSummarizer.close()
        articlesCache.clear()
    }
}

// Settings data classes
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val enableAiSummary: Boolean = true,
    val enableNotifications: Boolean = true,
    val textSize: TextSize = TextSize.MEDIUM
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class TextSize(val scaleFactor: Float) {
    SMALL(0.8f), MEDIUM(1f), LARGE(1.2f)
}