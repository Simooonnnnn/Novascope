// app/src/main/java/com/example/novascope/data/RssService.kt
package com.example.novascope.data

import android.util.Log
import com.example.novascope.model.NewsItem
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class RssService {
    private val rssParser = RssParser()

    // Simple cache with timestamp
    private data class CacheEntry(
        val items: List<NewsItem>,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val CACHE_DURATION = 10 * 60 * 1000L // 10 minutes

    // Single date formatter reused
    private val dateParser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Precompiled regex for image extraction
    private val imgRegex = """<img[^>]+src\s*=\s*['"]([^'"]+)['"][^>]*>""".toRegex()

    suspend fun fetchFeed(url: String, forceRefresh: Boolean = false): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                if (!forceRefresh) {
                    cache[url]?.let { entry ->
                        if (System.currentTimeMillis() - entry.timestamp < CACHE_DURATION) {
                            return@withContext entry.items
                        }
                    }
                }

                // Fetch with timeout
                val channel = withTimeoutOrNull(10000L) {
                    rssParser.getRssChannel(url)
                }

                if (channel == null) {
                    Log.e("RssService", "Timeout fetching feed: $url")
                    return@withContext cache[url]?.items ?: emptyList()
                }

                val feedIcon = channel.image?.url
                val feedTitle = channel.title ?: "Unknown Source"

                // Process items efficiently
                val items = channel.items.mapIndexed { index, item ->
                    val publishTimeMillis = parseDate(item.pubDate)
                    val content = item.content ?: item.description ?: ""

                    NewsItem(
                        id = "${item.title}_${item.link}".hashCode().toString(),
                        title = item.title ?: "No title",
                        imageUrl = item.image ?: extractImage(content),
                        sourceIconUrl = feedIcon,
                        sourceName = feedTitle,
                        publishTime = formatRelativeTime(publishTimeMillis),
                        publishTimeMillis = publishTimeMillis,
                        content = content,
                        url = item.link ?: "",
                        feedId = null,
                        isBookmarked = false,
                        isBigArticle = index == 0
                    )
                }

                // Update cache
                cache[url] = CacheEntry(items, System.currentTimeMillis())
                items

            } catch (e: Exception) {
                Log.e("RssService", "Error fetching feed: ${e.message}")
                cache[url]?.items ?: emptyList()
            }
        }
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()

        return try {
            dateParser.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            // Try alternative formats if needed
            System.currentTimeMillis()
        }
    }

    private fun extractImage(content: String): String? {
        if (content.length > 5000) return null // Skip large content
        return imgRegex.find(content)?.groupValues?.getOrNull(1)
    }

    private fun formatRelativeTime(timeMillis: Long): String {
        val diff = System.currentTimeMillis() - timeMillis

        return when {
            diff < 0 -> "Just now"
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMillis))
            }
        }
    }

    fun clearCache(url: String? = null) {
        if (url != null) {
            cache.remove(url)
        } else {
            cache.clear()
        }
    }
}