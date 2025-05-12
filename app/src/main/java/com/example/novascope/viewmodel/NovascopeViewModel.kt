// app/src/main/java/com/example/novascope/viewmodel/NovascopeViewModel.kt
package com.example.novascope.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.novascope.ai.ArticleSummarizer
import com.example.novascope.ai.SummaryState
import com.example.novascope.data.FeedRepository
import com.example.novascope.data.RssService
import com.example.novascope.model.Feed
import com.example.novascope.model.FeedCategory
import com.example.novascope.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class NovascopeViewModel(private val context: Context) : ViewModel() {

    // Services
    private val rssService = RssService()
    private val feedRepository = FeedRepository(context)
    private val articleSummarizer = ArticleSummarizer(context)

    // UI state
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

    init {
        // Observe feeds from repository
        viewModelScope.launch {
            feedRepository.feeds.collect { latestFeeds ->
                _feeds.value = latestFeeds
                // Reload news when feeds change
                loadFeeds()
            }
        }

        // Initialize the AI model
        viewModelScope.launch {
            try {
                articleSummarizer.initializeModel()
            } catch (e: Exception) {
                Log.e("ViewModel", "Error initializing AI model: ${e.message}")
            }
        }
    }

    // Load all feeds
    fun loadFeeds(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val allItems = mutableListOf<NewsItem>()
                val enabledFeeds = feedRepository.getEnabledFeeds()

                for (feed in enabledFeeds) {
                    try {
                        val items = rssService.fetchFeed(feed.url)
                        // Add the feed ID to each item for tracking
                        val itemsWithFeedId = items.map { it.copy(feedId = feed.id) }
                        allItems.addAll(itemsWithFeedId)
                    } catch (e: Exception) {
                        Log.e("ViewModel", "Error loading feed ${feed.name}: ${e.message}")
                    }
                }

                // Sort by publish date, newest first
                val sortedItems = allItems.sortedByDescending { it.publishTimeMillis }

                // Mark first item as big article
                val displayItems = if (sortedItems.isNotEmpty()) {
                    listOf(sortedItems.first().copy(isBigArticle = true)) +
                            sortedItems.drop(1).map { it.copy(isBigArticle = false) }
                } else {
                    sortedItems
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        newsItems = displayItems,
                        errorMessage = null,
                        // Preserve bookmarked state when refreshing
                        bookmarkedItems = updateBookmarks(displayItems, it.bookmarkedItems)
                    )
                }
            } catch (e: Exception) {
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

    // Preserve bookmarks when refreshing
    private fun updateBookmarks(newItems: List<NewsItem>, oldBookmarked: List<NewsItem>): List<NewsItem> {
        val bookmarkedIds = oldBookmarked.map { it.id }.toSet()
        return newItems.filter { it.isBookmarked || bookmarkedIds.contains(it.id) }
    }

    // Toggle bookmark for an article
    fun toggleBookmark(newsItemId: String) {
        _uiState.update { state ->
            val updatedItems = state.newsItems.map { item ->
                if (item.id == newsItemId) {
                    item.copy(isBookmarked = !item.isBookmarked)
                } else item
            }

            // Update bookmarked items list
            val updatedBookmarks = if (updatedItems.find { it.id == newsItemId }?.isBookmarked == true) {
                state.bookmarkedItems + updatedItems.find { it.id == newsItemId }!!
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
        }
    }

    // Toggle feed enabled state
    fun toggleFeedEnabled(feedId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            feedRepository.toggleFeedEnabled(feedId, isEnabled)
        }
    }

    // Get a feed by its URL
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

    // Select article for detail view
    fun selectArticle(articleId: String) {
        val article = _uiState.value.newsItems.find { it.id == articleId }
        _uiState.update { it.copy(selectedArticle = article) }

        // Generate AI summary if feature is enabled
        article?.let {
            if (_settings.value.enableAiSummary) {
                generateSummary(it)
            }
        }
    }

    // Generate summary with local AI
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
        articleSummarizer.close()
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