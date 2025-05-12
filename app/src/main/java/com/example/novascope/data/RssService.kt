package com.example.novascope.data

import android.util.Log
import com.example.novascope.model.NewsItem
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class RssService {
    private val rssParser = RssParser()

    suspend fun fetchFeed(url: String): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("RssService", "Fetching feed from $url")
                val channel = rssParser.getRssChannel(url)

                // Extract feed icon if available
                val feedIcon = channel.image?.url

                channel.items.mapIndexed { index, item ->
                    // Calculate publish time in millis for sorting
                    val publishDateMillis = parsePublishDate(item.pubDate ?: "")

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
            } catch (e: Exception) {
                Log.e("RssService", "Error fetching feed: ${e.message}")
                emptyList()
            }
        }
    }

    // Extract image URL from HTML content with improved pattern matching
    private fun findImageInContent(content: String): String? {
        // If content is empty, return null
        if (content.isBlank()) return null

        // Try to find an image tag
        val imgPattern = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex()
        val match = imgPattern.find(content)
        return match?.groupValues?.getOrNull(1)
    }

    // Parse publish date into milliseconds for easier handling
    private fun parsePublishDate(pubDate: String): Long {
        if (pubDate.isBlank()) return System.currentTimeMillis()

        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(pubDate)
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

    // Format relative time string (e.g., "2h ago", "3d ago")
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
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }
}