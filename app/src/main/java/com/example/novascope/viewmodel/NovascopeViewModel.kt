// app/src/main/java/com/example/novascope/viewmodel/NovascopeViewModel.kt
package com.example.novascope.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.novascope.ai.ArticleSummarizer
import com.example.novascope.ai.ModelDownloadManager
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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class NovascopeViewModel(private val context: Context) : ViewModel() {

    // Services
    private val rssService = RssService()
    private val feedRepository = FeedRepository(context)
    private val articleSummarizer = ArticleSummarizer(context)

    // Tracking state
    private val isLoadingFeeds = AtomicBoolean(false)
    private var currentLoadJob: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var currentSummaryJob: Job? = null
    private var currentDownloadJob: Job? = null // <--- Add this line


    // UI state with optimized updates
    data class UiState(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val newsItems: List<NewsItem> = emptyList(),
        val bookmarkedItems: List<NewsItem> = emptyList(),
        val errorMessage: String? = null,
        val selectedArticle: NewsItem? = null,
        val summaryState: SummaryState = SummaryState.Loading,
        val modelDownloadState: ModelDownloadManager.DownloadState = ModelDownloadManager.DownloadState.Idle
    )

// Update the downloadModel function in NovascopeViewModel.kt

    fun downloadModel() {
        // Cancel any existing download job
        currentDownloadJob?.cancel()

        currentDownloadJob = viewModelScope.launch {
            try {
                // Update UI state
                _uiState.update { it.copy(modelDownloadState = ModelDownloadManager.DownloadState.Downloading(0)) }

                // Collect download progress updates
                val progressJob = viewModelScope.launch {
                    articleSummarizer.downloadState.collect { state ->
                        _uiState.update { it.copy(modelDownloadState = state) }
                    }
                }

                // Start the download
                articleSummarizer.downloadModel()

                // Once download completes successfully, initialize the model
                if (articleSummarizer.isModelDownloaded) {
                    // Try to initialize with longer timeout
                    withTimeoutOrNull(30000L) {
                        articleSummarizer.initializeModel()
                    } ?: Log.w("ViewModel", "Model initialization timed out")

                    // Try to generate summary again if there's a selected article
                    val selectedArticle = _uiState.value.selectedArticle
                    if (selectedArticle != null) {
                        generateSummary(selectedArticle)
                    }
                }

                // Make sure to cancel the progress collector when done
                progressJob.cancel()

            } catch (e: Exception) {
                Log.e("ViewModel", "Error downloading model: ${e.message}")
                _uiState.update {
                    it.copy(modelDownloadState = ModelDownloadManager.DownloadState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    fun cancelModelDownload() {
        currentDownloadJob?.cancel()

        viewModelScope.launch {
            // Access the ArticleSummarizer to cancel the download
            articleSummarizer.downloadState.value.let {
                if (it is ModelDownloadManager.DownloadState.Downloading) {
                    // This will call the ModelDownloadManager's cancelDownload method
                    val downloadManager = ModelDownloadManager(context)
                    downloadManager.cancelDownload()

                    // Update UI state
                    _uiState.update { state ->
                        state.copy(modelDownloadState = ModelDownloadManager.DownloadState.Idle)
                    }
                }
            }
        }
    }

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
                // Add timeout to prevent blocking if model initialization takes too long
                withTimeoutOrNull(10000L) {
                    articleSummarizer.initializeModel()
                } ?: Log.w("ViewModel", "Model initialization timed out")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error initializing AI model: ${e.message}")
                // Don't crash, continue without the model
            }
        }
    }

    // Add lifecycle observer management
    fun attachLifecycleObserver(observer: LifecycleEventObserver) {
        lifecycleObserver = observer
    }

    fun detachLifecycleObserver(observer: LifecycleEventObserver) {
        if (lifecycleObserver === observer) {
            lifecycleObserver = null
        }
    }

    // Load all feeds with optimized parallel loading
// Partial optimization for loadFeeds method in NovascopeViewModel.kt
    fun loadFeeds(forceRefresh: Boolean = false) {
        // Cancel any existing load job
        currentLoadJob?.cancel()

        // Set loading state atomically
        if (!isLoadingFeeds.compareAndSet(false, true)) {
            if (!forceRefresh) return  // Don't reload if already loading
        }

        currentLoadJob = viewModelScope.launch {
            try {
                // Update loading state only - separate update to reduce composition passes
                _uiState.update {
                    it.copy(
                        isRefreshing = forceRefresh,
                        isLoading = !forceRefresh
                    )
                }

                if (forceRefresh) {
                    // Clear cache when forcing refresh
                    rssService.clearCache()
                }

                val enabledFeeds = feedRepository.getEnabledFeeds()

                // Don't proceed if no feeds are enabled
                if (enabledFeeds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            newsItems = emptyList(),
                            bookmarkedItems = emptyList(),
                            errorMessage = null
                        )
                    }
                    return@launch
                }

                // Process feeds in parallel with improved error handling
                val results = withContext(Dispatchers.IO) {
                    enabledFeeds.map { feed ->
                        async {
                            try {
                                val items = rssService.fetchFeed(feed.url, forceRefresh)
                                // Add the feed ID to each item for tracking
                                items.map { it.copy(feedId = feed.id) }
                            } catch (e: Exception) {
                                Log.e("ViewModel", "Error loading feed ${feed.name}: ${e.message}")
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                }

                // Process results efficiently with a single state update
                processNewsItems(results)

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

    private fun processNewsItems(newItems: List<NewsItem>) {
        // Perform all processing in one pass for better efficiency
        val processedItems = ArrayList<NewsItem>(newItems.size)
        val bookmarkedItems = ArrayList<NewsItem>()

        newItems.forEachIndexed { index, item ->
            val existingItem = articlesCache[item.id]
            val isBookmarked = existingItem?.isBookmarked == true

            // Create updated item with bookmarked status and big article flag
            val updatedItem = item.copy(
                isBookmarked = isBookmarked,
                isBigArticle = index == 0 && processedItems.isEmpty()
            )

            // Update cache
            articlesCache[item.id] = updatedItem

            // Add to processed items
            processedItems.add(updatedItem)

            // Add to bookmarked items if bookmarked
            if (isBookmarked) {
                bookmarkedItems.add(updatedItem)
            }
        }

        // Sort once by publish time for better performance
        val sortedItems = processedItems.sortedByDescending { it.publishTimeMillis }

        // Update state in a single pass
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                newsItems = sortedItems,
                bookmarkedItems = bookmarkedItems,
                errorMessage = null
            )
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

            // If this is the selected article, update that too
            val updatedSelectedArticle = if (state.selectedArticle?.id == newsItemId) {
                updatedItem
            } else {
                state.selectedArticle
            }

            state.copy(
                newsItems = updatedItems,
                bookmarkedItems = updatedBookmarks,
                selectedArticle = updatedSelectedArticle
            )
        }
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

            // Remove associated articles from UI
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
                // Add timeout to prevent blocking
                val items = withTimeoutOrNull(5000L) {
                    rssService.fetchFeed(url)
                } ?: return@withContext url

                items.firstOrNull()?.sourceName ?: url
            } catch (e: Exception) {
                url
            }
        }
    }

    // Select article for detail view with optimized summary loading
    fun selectArticle(articleId: String) {
        // Cancel any existing summary generation job
        currentSummaryJob?.cancel()

        // Try to find the article in multiple places
        val article = articlesCache[articleId]
            ?: _uiState.value.newsItems.find { it.id == articleId }
            ?: _uiState.value.bookmarkedItems.find { it.id == articleId }

        if (article != null) {
            // Always update the selected article in the UI state
            _uiState.update { it.copy(selectedArticle = article) }

            // Log the found article for debugging
            Log.d("NovascopeViewModel", "Article found: ${article.title}")
            Log.d("NovascopeViewModel", "Article content length: ${article.content?.length ?: 0}")

            // Only generate the summary if the feature is enabled
            if (_settings.value.enableAiSummary) {
                generateSummary(article)
            }
        } else {
            // Better error handling when article not found
            Log.e("NovascopeViewModel", "Article not found: $articleId")

            // Set error state
            _uiState.update {
                it.copy(
                    summaryState = SummaryState.Error("Article not found"),
                    selectedArticle = null
                )
            }
        }
    }

    // Generate summary with local AI and caching
    private fun generateSummary(newsItem: NewsItem) {
        // Set loading state immediately
        _uiState.update { it.copy(summaryState = SummaryState.Loading) }

        currentSummaryJob = viewModelScope.launch {
            try {
                // Use a timeout to prevent hanging
                withTimeoutOrNull(15000L) {
                    articleSummarizer.summarizeArticle(newsItem).collect { summaryState ->
                        _uiState.update { it.copy(summaryState = summaryState) }
                    }
                } ?: run {
                    // Timeout occurred, use fallback
                    val fallbackSummary = articleSummarizer.generateFallbackSummary(newsItem)
                    _uiState.update {
                        it.copy(summaryState = SummaryState.Success("$fallbackSummary (timeout occurred)"))
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error generating summary: ${e.message}")

                // Try fallback summary on error
                try {
                    val fallbackSummary = articleSummarizer.generateFallbackSummary(newsItem)
                    _uiState.update {
                        it.copy(summaryState = SummaryState.Success(fallbackSummary))
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(summaryState = SummaryState.Error("Error: ${e.message}"))
                    }
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
        currentSummaryJob?.cancel()
        articleSummarizer.close()
        articlesCache.clear()
        lifecycleObserver = null
    }
}

// Settings data class
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