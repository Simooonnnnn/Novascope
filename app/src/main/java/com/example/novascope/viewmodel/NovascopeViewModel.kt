// app/src/main/java/com/example/novascope/viewmodel/NovascopeViewModel.kt
package com.example.novascope.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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

class NovascopeViewModel(private val context: Context) : ViewModel() {

    // Services
    private val rssService = RssService()
    private val feedRepository = FeedRepository(context)
    private val articleSummarizer = ArticleSummarizer(context)

    // Job management
    private var loadFeedsJob: Job? = null
    private var summaryJob: Job? = null
    private var importJob: Job? = null

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

    // Settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Bookmarked IDs for faster lookup
    private val bookmarkedIds = mutableSetOf<String>()

    // Activity for file picking
    private var activity: ComponentActivity? = null
    private var modelFilePickerLauncher: ActivityResultLauncher<String>? = null

    init {
        // Initialize feeds
        viewModelScope.launch {
            feedRepository.feeds.collect { latestFeeds ->
                _feeds.emit(latestFeeds)
                if (_uiState.value.newsItems.isEmpty()) {
                    loadFeeds()
                }
            }
        }

        // Initialize AI model in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(5000L) {
                    articleSummarizer.initializeModel()
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error initializing AI model: ${e.message}")
            }
        }
    }

    fun registerActivity(activity: ComponentActivity) {
        this.activity = activity
        modelFilePickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                viewModelScope.launch {
                    importModelFromUri(it)
                }
            }
        }
    }

    fun unregisterActivity() {
        activity = null
    }

    fun launchModelFilePicker() {
        modelFilePickerLauncher?.launch("*/*")
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
                if (enabledFeeds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            newsItems = emptyList()
                        )
                    }
                    return@launch
                }

                // Process feeds in parallel with structured concurrency
                val newsItems = coroutineScope {
                    enabledFeeds.map { feed ->
                        async(Dispatchers.IO) {
                            try {
                                rssService.fetchFeed(feed.url, forceRefresh)
                                    .map { it.copy(feedId = feed.id) }
                            } catch (e: Exception) {
                                Log.e("ViewModel", "Error loading feed ${feed.name}: ${e.message}")
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                }

                // Process and update UI in one pass
                val sortedItems = newsItems
                    .mapIndexed { index, item ->
                        item.copy(
                            isBookmarked = bookmarkedIds.contains(item.id),
                            isBigArticle = index == 0
                        )
                    }
                    .sortedByDescending { it.publishTimeMillis }

                val bookmarkedItems = sortedItems.filter { it.isBookmarked }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        newsItems = sortedItems,
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
            }
        }
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
            if (!articleSummarizer.isModelImported) {
                _uiState.update { it.copy(summaryState = SummaryState.ModelNotImported) }
                return
            }

            withTimeoutOrNull(10000L) {
                articleSummarizer.summarizeArticle(newsItem).collect { summaryState ->
                    _uiState.update { it.copy(summaryState = summaryState) }
                }
            } ?: run {
                val fallback = articleSummarizer.generateFallbackSummary(newsItem)
                _uiState.update {
                    it.copy(summaryState = SummaryState.Success(fallback))
                }
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "Error generating summary: ${e.message}")
            try {
                val fallback = articleSummarizer.generateFallbackSummary(newsItem)
                _uiState.update {
                    it.copy(summaryState = SummaryState.Success(fallback))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(summaryState = SummaryState.Error("Error: ${e.message}"))
                }
            }
        }
    }

    private suspend fun importModelFromUri(uri: Uri) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(modelImportState = ModelFileManager.ImportState.Importing(0)) }

                val progressJob = launch {
                    articleSummarizer.importState.collect { state ->
                        _uiState.update { it.copy(modelImportState = state) }
                    }
                }

                val importResult = articleSummarizer.importModel(uri)
                if (importResult && articleSummarizer.isModelImported) {
                    withTimeoutOrNull(15000L) {
                        articleSummarizer.initializeModel()
                    }

                    _uiState.value.selectedArticle?.let { article ->
                        generateSummary(article)
                    }
                }

                progressJob.cancel()
            } catch (e: Exception) {
                Log.e("ViewModel", "Error importing model: ${e.message}", e)
                _uiState.update {
                    it.copy(modelImportState = ModelFileManager.ImportState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    fun cancelModelImport() {
        importJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(modelImportState = ModelFileManager.ImportState.Idle) }
        }
    }

    fun isModelImported(): Boolean = articleSummarizer.isModelImported

    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
    }

    override fun onCleared() {
        super.onCleared()
        loadFeedsJob?.cancel()
        summaryJob?.cancel()
        importJob?.cancel()
        articleSummarizer.close()
    }
}

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