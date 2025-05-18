// app/src/main/java/com/example/novascope/data/RssService.kt
package com.example.novascope.data

import android.util.Log
import com.example.novascope.model.NewsItem
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for fetching and parsing RSS feeds with optimized performance
 */
class RssService {
    private val rssParser = RssParser()

    // Cache for already fetched feeds to avoid redundant network calls
    private val feedCache = ConcurrentHashMap<String, CacheEntry>()

    // Cache expiration time (10 minutes)
    private val CACHE_EXPIRATION_MS = 10 * 60 * 1000L

    // Date format parsers - created once and reused
    private val dateFormats = listOf(
        ThreadLocal.withInitial { SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } },
        ThreadLocal.withInitial { SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } },
        ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } },
        ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } },
        ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } },
        ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    )

    // Relative time formatter - created once
    private val relativeTimeFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

    // Image URL regex pattern - compiled once
    private val imgPattern = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex()

    suspend fun fetchFeed(url: String, forceRefresh: Boolean = false): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first if not forcing refresh
                if (!forceRefresh) {
                    val cachedEntry = feedCache[url]
                    if (cachedEntry != null && !isCacheExpired(cachedEntry.timestamp)) {
                        Log.d("RssService", "Using cached feed for $url")
                        return@withContext cachedEntry.items
                    }
                }

                Log.d("RssService", "Fetching feed from $url")

                // Add timeout to prevent hanging on bad connections
                val channel = withTimeoutOrNull(15000L) {
                    try {
                        rssParser.getRssChannel(url)
                    } catch (e: Exception) {
                        Log.e("RssService", "Error parsing RSS: ${e.message}")
                        // Erstelle einen leeren Channel als Fallback
                        RssChannel(
                            title = "Unknown Feed",
                            link = url,
                            description = "",
                            lastBuildDate = "",
                            image = null,
                            items = emptyList(),
                            updatePeriod = null,
                            itunesChannelData = null
                        )
                    }
                } ?: RssChannel(
                    title = "Unknown Feed",
                    link = url,
                    description = "",
                    lastBuildDate = "",
                    image = null,
                    items = emptyList(),
                    updatePeriod = null,
                    itunesChannelData = null
                )

                // Extract feed icon if available
                val feedIcon = channel.image?.url

                val items = channel.items.mapIndexedTo(ArrayList(channel.items.size)) { index, item ->
                    // Calculate publish time in millis for sorting
                    val publishDateMillis = parsePublishDate(item.pubDate ?: "")
                    val title = item.title?.takeIf { it.isNotBlank() } ?: "No title"



                    NewsItem(
                        id = item.guid ?: UUID.randomUUID().toString(),
                        title = item.title ?: "No title",
                        imageUrl = item.image ?: findImageInContent(item.content ?: item.description ?: ""),
                        sourceIconUrl = feedIcon,
                        sourceName = channel.title ?: "Unknown Source",
                        publishTime = formatRelativeTime(publishDateMillis),
                        publishTimeMillis = publishDateMillis,
                        content = item.content ?: item.description,
                        url = item.link,
                        isBigArticle = index == 0 // First item is displayed as big article
                    )
                }

                // Cache the results
                feedCache[url] = CacheEntry(items, System.currentTimeMillis())

                items
            } catch (e: Exception) {
                Log.e("RssService", "Error fetching feed: ${e.message}")
                // Return cached entry if it exists, even if expired
                return@withContext feedCache[url]?.items ?: emptyList()
            }
        }
    }

    // Check if cache has expired
    private fun isCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_MS
    }

    // Extract image URL from HTML content with improved pattern matching
    private fun findImageInContent(content: String): String? {
        // If content is empty, return null
        if (content.isBlank()) return null

        // Try to find an image tag using the pre-compiled regex
        val match = imgPattern.find(content)
        return match?.groupValues?.getOrNull(1)
    }

    // Parse publish date into milliseconds with thread-safe date formatters
    private fun parsePublishDate(pubDate: String): Long {
        if (pubDate.isBlank()) return System.currentTimeMillis()

        for (formatThreadLocal in dateFormats) {
            try {
                val format = formatThreadLocal.get()
                val date = format.parse(pubDate)
                if (date != null) {
                    return date.time
                }
            } catch (e: ParseException) {
                // Try next format
            }
        }

        // If no format matches, return current time
        return System.currentTimeMillis()
    }

    // Format relative time string (e.g., "2h ago", "3d ago") with optimizations
    private fun formatRelativeTime(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMillis

        // Handle cases of incorrect future dates
        if (diff < 0) return "Just now"

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d"
            diff < 30 * 24 * 60 * 60 * 1000 -> "${diff / (7 * 24 * 60 * 60 * 1000)}w"
            else -> {
                val date = Date(timeMillis)
                relativeTimeFormatter.format(date)
            }
        }
    }

    // Clear cache for a specific URL or all URLs
    fun clearCache(url: String? = null) {
        if (url != null) {
            feedCache.remove(url)
        } else {
            feedCache.clear()
        }
    }

    // Data class for caching feed entries
    private data class CacheEntry(
        val items: List<NewsItem>,
        val timestamp: Long
    )
}