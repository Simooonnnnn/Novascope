// app/src/main/java/com/example/novascope/data/FeedRepository.kt
package com.example.novascope.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.novascope.model.Feed
import com.example.novascope.model.FeedCategory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Repository for managing RSS feeds with optimized storage and caching
 */
class FeedRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Optimized Gson instance - reused across the class
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    // Mutex for thread-safe operations
    private val mutex = Mutex()

    // Flag to track initialization state
    private val initialized = AtomicBoolean(false)

    // StateFlow to hold the current feeds
    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds: StateFlow<List<Feed>> = _feeds.asStateFlow()

    // In-memory cache for category-specific feeds
    private val feedsByCategory = mutableMapOf<FeedCategory, List<Feed>>()

    init {
        // Load feeds from storage
        loadFeeds()
    }

    // Load feeds from SharedPreferences with error handling
    private fun loadFeeds() {
        if (initialized.get()) return

        val feedsJson = prefs.getString(KEY_FEEDS, null)
        val feedsList = if (feedsJson != null) {
            try {
                val type = object : TypeToken<List<Feed>>() {}.type
                gson.fromJson<List<Feed>>(feedsJson, type)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing feeds JSON: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        _feeds.value = feedsList

        // Update category cache
        updateCategoryCache(feedsList)

        // If no feeds yet, add default feeds
        if (feedsList.isEmpty()) {
            addDefaultFeeds()
        }

        initialized.set(true)
    }

    // Save feeds to SharedPreferences with optimization
    private suspend fun saveFeeds(feeds: List<Feed>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val feedsJson = gson.toJson(feeds)
            prefs.edit().putString(KEY_FEEDS, feedsJson).apply()
            _feeds.value = feeds

            // Update category cache
            updateCategoryCache(feeds)
        }
    }

    // Update category cache for faster lookups
    private fun updateCategoryCache(feeds: List<Feed>) {
        for (category in FeedCategory.values()) {
            feedsByCategory[category] = feeds.filter { it.category == category }
        }
    }

    // Get feeds by category using the cache
    fun getFeedsByCategory(category: FeedCategory): List<Feed> {
        return feedsByCategory[category] ?: _feeds.value.filter { it.category == category }.also {
            feedsByCategory[category] = it
        }
    }

    // Get enabled feeds
    fun getEnabledFeeds(): List<Feed> {
        return _feeds.value.filter { it.isEnabled }
    }

    // Add a new feed
    suspend fun addFeed(feed: Feed) = withContext(Dispatchers.IO) {
        val currentFeeds = _feeds.value.toMutableList()
        currentFeeds.add(feed)
        saveFeeds(currentFeeds)
    }

    // Update an existing feed
    suspend fun updateFeed(feed: Feed) = withContext(Dispatchers.IO) {
        val currentFeeds = _feeds.value.toMutableList()
        val index = currentFeeds.indexOfFirst { it.id == feed.id }
        if (index != -1) {
            currentFeeds[index] = feed
            saveFeeds(currentFeeds)
        }
    }

    // Delete a feed
    suspend fun deleteFeed(feedId: String) = withContext(Dispatchers.IO) {
        val currentFeeds = _feeds.value.toMutableList()
        currentFeeds.removeIf { it.id == feedId }
        saveFeeds(currentFeeds)
    }

    // Toggle feed enabled state
    suspend fun toggleFeedEnabled(feedId: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val currentFeeds = _feeds.value.toMutableList()
        val index = currentFeeds.indexOfFirst { it.id == feedId }
        if (index != -1) {
            val feed = currentFeeds[index]
            currentFeeds[index] = feed.copy(isEnabled = isEnabled)
            saveFeeds(currentFeeds)
        }
    }

    // Add default feeds with Swiss and tech focus
    private fun addDefaultFeeds() {
        val defaultFeeds = listOf(
            // Swiss News
            Feed(
                name = "SRF News",
                url = "https://www.srf.ch/bnf/rss/19920122",
                category = FeedCategory.News,
                iconUrl = "https://www.srf.ch/shared/ressources/icons/touch-icons/apple-touch-icon-152x152.png",
                isDefault = true
            ),
            Feed(
                name = "The Guardian",
                url = "https://www.theguardian.com/world/rss",
                category = FeedCategory.News,
                iconUrl = "https://assets.guim.co.uk/images/guardian-logo-rss.c45beb1bafa34b347ac333af2e6fe23f.png",
                isDefault = true
            ),


            // Technology
            Feed(
                name = "Digitec",
                url = "https://static.digitecgalaxus.ch/feeds/rss/digitec_CH_en.xml",
                category = FeedCategory.Tech,
                iconUrl = "https://static.digitecgalaxus.ch/web/assets/images/apple-touch-icon-152x152.png",
                isDefault = true
            ),
            Feed(
                name = "The Verge",
                url = "https://www.theverge.com/rss/index.xml",
                category = FeedCategory.Tech,
                iconUrl = "https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395359/favicon-16x16.0.png",
                isDefault = true
            ),
            Feed(
                name = "UX Collective",
                url = "https://uxdesign.cc/feed/",
                category = FeedCategory.Tech,
                iconUrl = "https://cdn-images-1.medium.com/fit/c/36/36/1*mDhF9X4VO0rCrJvWFatyxg.png",
                isDefault = true
            ),


            // Science
            Feed(
                name = "NASA News",
                url = "https://www.nasa.gov/feed/",
                category = FeedCategory.Science,
                iconUrl = "https://www.nasa.gov/wp-content/themes/nasa/assets/images/favicon-192.png",
                isDefault = true
            ),

            // Finance
            Feed(
                name = "Bloomberg Markets",
                url = "https://feeds.bloomberg.com/markets/news.rss",
                category = FeedCategory.Finance,
                iconUrl = "https://assets.bwbx.io/s3/javelin/public/javelin/images/bloomberg-logo-new-2x-default-_e9e82cea1f.png",
                isDefault = true
            )
        )

        // Launch in IO context to avoid blocking the main thread
        _feeds.value = defaultFeeds

        // Update category cache
        updateCategoryCache(defaultFeeds)

        // Save in background
        val feedsJson = gson.toJson(defaultFeeds)
        prefs.edit().putString(KEY_FEEDS, feedsJson).apply()
    }

    companion object {
        private const val TAG = "FeedRepository"
        private const val PREFS_NAME = "novascope_feeds"
        private const val KEY_FEEDS = "feeds"
    }
}