// app/src/main/java/com/example/novascope/viewmodel/NovascopeViewModel.kt
package com.example.novascope.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.novascope.ai.ArticleSummarizer
import com.example.novascope.ai.SummaryState
import com.example.novascope.data.RssService
import com.example.novascope.model.NewsItem
import com.example.novascope.ui.components.FeedCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class NovascopeViewModel(
    private val context: Context
) : ViewModel() {

    private val rssService = RssService()
    private val articleSummarizer = ArticleSummarizer(context)

    // UI state
    private val _uiState = MutableStateFlow(NovascopeUiState())
    val uiState: StateFlow<NovascopeUiState> = _uiState.asStateFlow()

    // Settings state
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var useDynamicColor by mutableStateOf(true)
        private set
    var enableAiSummary by mutableStateOf(true)
        private set
    var enableNotifications by mutableStateOf(true)
        private set

    // Feed sources
    private val feedSources = mutableListOf(
        FeedSource(
            id = UUID.randomUUID().toString(),
            name = "Tech News",
            url = "https://www.theverge.com/rss/index.xml",
            category = FeedCategory.Tech,
            isEnabled = true
        ),
        FeedSource(
            id = UUID.randomUUID().toString(),
            name = "World News",
            url = "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
            category = FeedCategory.News,
            isEnabled = true
        ),
        FeedSource(
            id = UUID.randomUUID().toString(),
            name = "Science Daily",
            url = "https://www.sciencedaily.com/rss/all.xml",
            category = FeedCategory.Science,
            isEnabled = true
        )
    )

    init {
        loadFeeds()
    }

    // Load feeds from all sources
    fun loadFeeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val enabledSources = feedSources.filter { it.isEnabled }
            val allItems = mutableListOf<NewsItem>()

            enabledSources.forEach { source ->
                try {
                    val items = rssService.fetchFeed(source.url)
                    allItems.addAll(items)
                } catch (e: Exception) {
                    // Handle error
                    _uiState.update {
                        it.copy(
                            error = "Error loading feed from ${source.name}: ${e.message}"
                        )
                    }
                }
            }

            // Sort by date (newest first) and mark first item as big article
            val sortedItems = allItems.sortedByDescending { it.publishTime }
                .mapIndexed { index, item ->
                    item.copy(isBigArticle = index == 0)
                }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    newsItems = sortedItems,
                    error = null
                )
            }
        }
    }

    // Add new feed source
    fun addFeedSource(url: String, category: FeedCategory, name: String? = null) {
        viewModelScope.launch {
            try {
                // First fetch to validate and get title if name is null
                val items = rssService.fetchFeed(url)
                val sourceName = name ?: items.firstOrNull()?.sourceName ?: "Unknown Feed"

                val newSource = FeedSource(
                    id = UUID.randomUUID().toString(),
                    name = sourceName,
                    url = url,
                    category = category,
                    isEnabled = true
                )

                feedSources.add(newSource)
                loadFeeds() // Reload all feeds

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Error adding feed: ${e.message}"
                    )
                }
            }
        }
    }

    // Toggle bookmark status for a news item
    fun toggleBookmark(newsItemId: String) {
        _uiState.update { state ->
            val updatedItems = state.newsItems.map { item ->
                if (item.id == newsItemId) {
                    item.copy(isBookmarked = !item.isBookmarked)
                } else {
                    item
                }
            }
            state.copy(newsItems = updatedItems)
        }
    }

    // Get bookmarked items
    fun getBookmarkedItems(): List<NewsItem> {
        return uiState.value.newsItems.filter { it.isBookmarked }
    }

    // Filter items by category
    fun filterByCategory(category: FeedCategory?): List<NewsItem> {
        if (category == null) {
            return uiState.value.newsItems
        }

        val categorySourceIds = feedSources
            .filter { it.category == category }
            .map { it.id }

        return uiState.value.newsItems.filter { item ->
            feedSources.any { source ->
                source.category == category && item.sourceName == source.name
            }
        }
    }

    // Get AI summary for an article
    fun generateSummary(newsItemId: String) {
        if (!enableAiSummary) return

        viewModelScope.launch {
            _uiState.update { state ->
                val currentSummaries = state.summaries.toMutableMap()
                currentSummaries[newsItemId] = SummaryState.Loading
                state.copy(summaries = currentSummaries)
            }

            val newsItem = uiState.value.newsItems.find { it.id == newsItemId }
            if (newsItem != null) {
                articleSummarizer.summarizeArticle(newsItem).collect { summaryState ->
                    _uiState.update { state ->
                        val currentSummaries = state.summaries.toMutableMap()
                        currentSummaries[newsItemId] = summaryState
                        state.copy(summaries = currentSummaries)
                    }
                }
            }
        }
    }

    // Update settings
    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
    }

    fun updateDynamicColor(enabled: Boolean) {
        useDynamicColor = enabled
    }

    fun updateAiSummary(enabled: Boolean) {
        enableAiSummary = enabled
    }

    fun updateNotifications(enabled: Boolean) {
        enableNotifications = enabled
    }

    // Clear error
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        articleSummarizer.close()
    }

    // Factory for creating ViewModel with context
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NovascopeViewModel::class.java)) {
                return NovascopeViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// UI State for Novascope
data class NovascopeUiState(
    val isLoading: Boolean = false,
    val newsItems: List<NewsItem> = emptyList(),
    val error: String? = null,
    val summaries: Map<String, SummaryState> = emptyMap()
)

// Feed source data class
data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val category: FeedCategory,
    val isEnabled: Boolean = true
)

// Theme mode enum
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}