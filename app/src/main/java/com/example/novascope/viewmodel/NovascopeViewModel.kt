// app/src/main/java/com/example/novascope/viewmodel/NovascopeViewModel.kt
package com.example.novascope.viewmodel

import android.content.Context
import android.util.Log  // Add this import
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
import com.example.novascope.model.FeedCategory  // Ensure correct import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// Simplified parts of NovascopeViewModel.kt
class NovascopeViewModel(private val context: Context) : ViewModel() {

    private val feedSources = mutableListOf(
        FeedSource(
            id = UUID.randomUUID().toString(),
            name = "Tech News",
            url = "https://www.theverge.com/rss/index.xml",
            category = FeedCategory.Tech,  // Make sure it's the correct enum
            isEnabled = true
        ),
        FeedSource(
            id = UUID.randomUUID().toString(),
            name = "World News",
            url = "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
            category = FeedCategory.News,  // Make sure it's the correct enum
            isEnabled = true
        )
    )


    private val rssService = RssService()

    // Simple UI state with only the necessary properties
    data class UiState(
        val isLoading: Boolean = false,
        val newsItems: List<NewsItem> = emptyList(),
        val errorMessage: String? = null,
        val bookmarkedItems: List<NewsItem> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Settings as separate StateFlow for better state management
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        loadFeeds()
    }

    // Simplified feed loading
// In NovascopeViewModel.kt - fix the loadFeeds method
    fun loadFeeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val allItems = mutableListOf<NewsItem>()

                feedSources.filter { it.isEnabled }.forEach { source ->
                    try {
                        val items = rssService.fetchFeed(source.url)
                        allItems.addAll(items)
                    } catch (e: Exception) {
                        // Log error but continue with other sources
                        Log.e("ViewModel", "Error loading feed: ${e.message}")
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        newsItems = allItems,
                        errorMessage = null  // Changed from error to errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error loading feeds: ${e.message}"  // Changed from error to errorMessage
                    )
                }
            }
        }
    }

    // Clear, focused functions for each action
    fun toggleBookmark(newsItemId: String) {
        _uiState.update { state ->
            val updatedItems = state.newsItems.map { item ->
                if (item.id == newsItemId) {
                    item.copy(isBookmarked = !item.isBookmarked)
                } else item
            }

            state.copy(
                newsItems = updatedItems,
                bookmarkedItems = updatedItems.filter { it.isBookmarked }
            )
        }
    }

    // Other simplified methods
}

data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val category: FeedCategory,
    val isEnabled: Boolean = true
)

// Settings as a data class for type safety
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