package com.example.novascope.data

import android.content.Context
import android.content.SharedPreferences
import com.example.novascope.model.Feed
import com.example.novascope.model.FeedCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository for managing RSS feeds
 * Uses SharedPreferences for storage as a simple solution
 */
class FeedRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // StateFlow to hold the current feeds
    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds: StateFlow<List<Feed>> = _feeds.asStateFlow()

    init {
        // Load feeds from storage
        loadFeeds()
    }

    // Load feeds from SharedPreferences
    private fun loadFeeds() {
        val feedsJson = prefs.getString(KEY_FEEDS, null)
        val feedsList = if (feedsJson != null) {
            try {
                val type = object : TypeToken<List<Feed>>() {}.type
                gson.fromJson<List<Feed>>(feedsJson, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        _feeds.value = feedsList

        // If no feeds yet, add default feeds
        if (feedsList.isEmpty()) {
            addDefaultFeeds()
        }
    }

    // Save feeds to SharedPreferences
    private fun saveFeeds(feeds: List<Feed>) {
        val feedsJson = gson.toJson(feeds)
        prefs.edit().putString(KEY_FEEDS, feedsJson).apply()
        _feeds.value = feeds
    }

    // Get feeds by category
    fun getFeedsByCategory(category: FeedCategory): List<Feed> {
        return _feeds.value.filter { it.category == category }
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

    // Add default feeds
    private fun addDefaultFeeds() {
        val defaultFeeds = listOf(
            // News
            Feed(
                name = "CNN Top Stories",
                url = "http://rss.cnn.com/rss/cnn_topstories.rss",
                category = FeedCategory.News,
                iconUrl = "https://cdn.cnn.com/cnn/.e/img/3.0/global/misc/cnn-logo.png",
                isDefault = true
            ),
            Feed(
                name = "BBC News",
                url = "http://feeds.bbci.co.uk/news/rss.xml",
                category = FeedCategory.News,
                iconUrl = "https://news.bbcimg.co.uk/nol/shared/img/bbc_news_120x60.gif",
                isDefault = true
            ),

            // Technology
            Feed(
                name = "The Verge",
                url = "https://www.theverge.com/rss/index.xml",
                category = FeedCategory.Tech,
                iconUrl = "https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395359/favicon-16x16.0.png",
                isDefault = true
            ),
            Feed(
                name = "TechCrunch",
                url = "https://techcrunch.com/feed/",
                category = FeedCategory.Tech,
                iconUrl = "https://techcrunch.com/wp-content/uploads/2015/02/cropped-cropped-favicon-gradient.png",
                isDefault = true
            ),

            // Science
            Feed(
                name = "Scientific American",
                url = "https://rss.sciam.com/ScientificAmerican-Global",
                category = FeedCategory.Science,
                iconUrl = "https://static.scientificamerican.com/sciam/cache/file/15E43C18-0CBC-4EAD-96C4E8F8CA0C0AB4_source.png",
                isDefault = true
            ),
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
            ),

            // Sports
            Feed(
                name = "ESPN",
                url = "https://www.espn.com/espn/rss/news",
                category = FeedCategory.Sports,
                iconUrl = "https://a.espncdn.com/i/espn/teamlogos/lrg/trans/espn_dotcom_black.gif",
                isDefault = true
            )
        )

        saveFeeds(defaultFeeds)
    }

    companion object {
        private const val PREFS_NAME = "novascope_feeds"
        private const val KEY_FEEDS = "feeds"
    }
}