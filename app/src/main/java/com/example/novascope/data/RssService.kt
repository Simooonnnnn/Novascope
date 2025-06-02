// app/src/main/java/com/example/novascope/data/RssService.kt
package com.example.novascope.data

import android.util.Log
import com.example.novascope.model.NewsItem
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class RssService {
    private val rssParser = RssParser()
    private val webScraper = WebScraper() // Add web scraper

    // Simple cache with timestamp
    private data class CacheEntry(
        val items: List<NewsItem>,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val CACHE_DURATION = 10 * 60 * 1000L // 10 minutes

    // Multiple date formatters to handle various RSS date formats
    private val dateFormatters = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    )

    // Comprehensive regex patterns for image extraction
    private val imageRegexes = listOf(
        // Standard img tags
        """<img[^>]+src\s*=\s*['"]([^'"]+)['"][^>]*>""".toRegex(RegexOption.IGNORE_CASE),
        // Media content
        """<media:content[^>]+url\s*=\s*['"]([^'"]+)['"][^>]*>""".toRegex(RegexOption.IGNORE_CASE),
        // Enclosure tags
        """<enclosure[^>]+url\s*=\s*['"]([^'"]+)['"][^>]*type\s*=\s*['"]image/[^'"]*['"][^>]*>""".toRegex(RegexOption.IGNORE_CASE),
        // Another enclosure pattern
        """<enclosure[^>]+type\s*=\s*['"]image/[^'"]*['"][^>]+url\s*=\s*['"]([^'"]+)['"][^>]*>""".toRegex(RegexOption.IGNORE_CASE),
        // Figure tags
        """<figure[^>]*>[\s\S]*?<img[^>]+src\s*=\s*['"]([^'"]+)['"]""".toRegex(RegexOption.IGNORE_CASE),
        // Picture tags
        """<picture[^>]*>[\s\S]*?<img[^>]+src\s*=\s*['"]([^'"]+)['"]""".toRegex(RegexOption.IGNORE_CASE)
    )

    // Common image file extensions
    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")

    suspend fun fetchFeed(url: String, forceRefresh: Boolean = false): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("RssService", "Fetching feed: $url")

                // Check cache first
                if (!forceRefresh) {
                    cache[url]?.let { entry ->
                        if (System.currentTimeMillis() - entry.timestamp < CACHE_DURATION) {
                            Log.d("RssService", "Returning cached results for: $url")
                            return@withContext entry.items
                        }
                    }
                }

                // Fetch with timeout
                val channel = withTimeoutOrNull(15000L) { // Increased timeout
                    rssParser.getRssChannel(url)
                }

                if (channel == null) {
                    Log.e("RssService", "Timeout fetching feed: $url")
                    return@withContext cache[url]?.items ?: emptyList()
                }

                Log.d("RssService", "Successfully fetched channel: ${channel.title}")
                Log.d("RssService", "Number of items: ${channel.items.size}")

                val feedIcon = channel.image?.url
                val feedTitle = cleanText(channel.title ?: "Unknown Source")

                // Process items efficiently without web scraping (original behavior)
                val items = processItemsRegular(channel.items, feedIcon, feedTitle)

                Log.d("RssService", "Successfully processed ${items.size} items")

                // Update cache
                cache[url] = CacheEntry(items, System.currentTimeMillis())
                items

            } catch (e: Exception) {
                Log.e("RssService", "Error fetching feed $url: ${e.message}", e)
                // Return cached items if available, otherwise empty list
                cache[url]?.items ?: emptyList()
            }
        }
    }

    suspend fun fetchFeedWithScraping(url: String, forceRefresh: Boolean = false): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("RssService", "Fetching feed with scraping: $url")

                // Check cache first
                if (!forceRefresh) {
                    cache[url]?.let { entry ->
                        if (System.currentTimeMillis() - entry.timestamp < CACHE_DURATION) {
                            Log.d("RssService", "Returning cached results for: $url")
                            return@withContext entry.items
                        }
                    }
                }

                // Fetch with timeout
                val channel = withTimeoutOrNull(15000L) {
                    rssParser.getRssChannel(url)
                }

                if (channel == null) {
                    Log.e("RssService", "Timeout fetching feed: $url")
                    return@withContext cache[url]?.items ?: emptyList()
                }

                Log.d("RssService", "Successfully fetched channel: ${channel.title}")
                Log.d("RssService", "Number of items: ${channel.items.size}")

                val feedIcon = channel.image?.url
                val feedTitle = cleanText(channel.title ?: "Unknown Source")

                // Process items with web scraping
                val items = processItemsWithScraping(channel.items, feedIcon, feedTitle)

                Log.d("RssService", "Successfully processed ${items.size} items with scraping")

                // Update cache
                cache[url] = CacheEntry(items, System.currentTimeMillis())
                items

            } catch (e: Exception) {
                Log.e("RssService", "Error fetching feed with scraping $url: ${e.message}", e)
                // Return cached items if available, otherwise empty list
                cache[url]?.items ?: emptyList()
            }
        }
    }

    /**
     * Process RSS items without web scraping (original behavior)
     */
    private fun processItemsRegular(
        rssItems: List<com.prof18.rssparser.model.RssItem>,
        feedIcon: String?,
        feedTitle: String
    ): List<NewsItem> {
        return rssItems.mapNotNull { item ->
            processItemRegular(item, feedIcon, feedTitle)
        }.sortedByDescending { it.publishTimeMillis }
    }

    /**
     * Process RSS items with web scraping for full content
     */
    private suspend fun processItemsWithScraping(
        rssItems: List<com.prof18.rssparser.model.RssItem>,
        feedIcon: String?,
        feedTitle: String
    ): List<NewsItem> = coroutineScope {
        // Process first 5-10 items with scraping to avoid overwhelming the server
        val itemsToScrape = rssItems.take(10)
        val remainingItems = rssItems.drop(10)

        // Process items with scraping in parallel (but limited)
        val scrapedItems = itemsToScrape.map { item ->
            async(Dispatchers.IO) {
                processItemWithScraping(item, feedIcon, feedTitle)
            }
        }.awaitAll()

        // Process remaining items without scraping (fallback to RSS content)
        val regularItems = remainingItems.map { item ->
            processItemRegular(item, feedIcon, feedTitle)
        }

        (scrapedItems + regularItems)
            .filterNotNull()
            .sortedByDescending { it.publishTimeMillis }
    }

    /**
     * Process individual RSS item with web scraping
     */
    private suspend fun processItemWithScraping(
        item: com.prof18.rssparser.model.RssItem,
        feedIcon: String?,
        feedTitle: String
    ): NewsItem? {
        return try {
            val publishTimeMillis = parseDate(item.pubDate)
            val stableId = generateStableId(item.title, item.link, item.pubDate)

            // Try to scrape full content if URL is available
            var finalContent = item.content ?: item.description ?: ""
            var finalTitle = item.title ?: "No title"
            var finalImageUrl = extractBestImage(item.image, finalContent, item.link)

            if (!item.link.isNullOrBlank()) {
                Log.d("RssService", "Attempting to scrape: ${item.link}")

                val scrapedArticle = withTimeoutOrNull(20000L) { // 20 second timeout per article
                    webScraper.scrapeArticle(item.link!!)
                }

                if (scrapedArticle?.success == true && !scrapedArticle.content.isNullOrBlank()) {
                    Log.d("RssService", "Successfully scraped content: ${scrapedArticle.content!!.length} chars")
                    finalContent = scrapedArticle.content!!

                    // Use scraped title if better than RSS title
                    if (!scrapedArticle.title.isNullOrBlank() && scrapedArticle.title!!.length > finalTitle.length) {
                        finalTitle = scrapedArticle.title!!
                    }

                    // Use scraped image if available and better
                    if (!scrapedArticle.imageUrl.isNullOrBlank()) {
                        finalImageUrl = scrapedArticle.imageUrl
                    }
                } else {
                    Log.d("RssService", "Scraping failed for ${item.link}, using RSS content")
                    finalContent = cleanHtmlContent(finalContent)
                }
            } else {
                finalContent = cleanHtmlContent(finalContent)
            }

            NewsItem(
                id = stableId,
                title = cleanText(finalTitle),
                imageUrl = finalImageUrl,
                sourceIconUrl = feedIcon,
                sourceName = feedTitle,
                publishTime = formatRelativeTime(publishTimeMillis),
                publishTimeMillis = publishTimeMillis,
                content = finalContent,
                url = item.link ?: "",
                feedId = null,
                isBookmarked = false,
                isBigArticle = false
            )
        } catch (e: Exception) {
            Log.e("RssService", "Error processing RSS item with scraping: ${e.message}")
            null
        }
    }

    /**
     * Process RSS item without scraping (fallback method)
     */
    private fun processItemRegular(
        item: com.prof18.rssparser.model.RssItem,
        feedIcon: String?,
        feedTitle: String
    ): NewsItem? {
        return try {
            val publishTimeMillis = parseDate(item.pubDate)
            val rawContent = item.content ?: item.description ?: ""
            val cleanContent = cleanHtmlContent(rawContent)

            // Generate more stable ID
            val stableId = generateStableId(item.title, item.link, item.pubDate)

            // Try multiple methods to extract image
            val imageUrl = extractBestImage(item.image, rawContent, item.link)

            NewsItem(
                id = stableId,
                title = cleanText(item.title ?: "No title"),
                imageUrl = imageUrl,
                sourceIconUrl = feedIcon,
                sourceName = feedTitle,
                publishTime = formatRelativeTime(publishTimeMillis),
                publishTimeMillis = publishTimeMillis,
                content = cleanContent,
                url = item.link ?: "",
                feedId = null,
                isBookmarked = false,
                isBigArticle = false
            )
        } catch (e: Exception) {
            Log.e("RssService", "Error processing RSS item: ${e.message}")
            null
        }
    }

    private fun generateStableId(title: String?, link: String?, pubDate: String?): String {
        val content = "${title ?: ""}_${link ?: ""}_${pubDate ?: ""}"
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(content.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to hashCode if MD5 fails
            content.hashCode().toString()
        }
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()

        // Try each date formatter
        for (formatter in dateFormatters) {
            try {
                val date = formatter.parse(dateStr.trim())
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Continue to next formatter
            }
        }

        Log.w("RssService", "Could not parse date: $dateStr")
        return System.currentTimeMillis()
    }

    private fun extractBestImage(itemImage: String?, content: String?, itemLink: String?): String? {
        // First try the item's direct image
        itemImage?.let { img ->
            if (isValidImageUrl(img)) {
                return resolveImageUrl(img, itemLink)
            }
        }

        // Skip processing for very large content to avoid performance issues
        if (content != null && content.length < 10000) {
            // Try each regex pattern
            for (regex in imageRegexes) {
                val match = regex.find(content)
                match?.groupValues?.getOrNull(1)?.let { imageUrl ->
                    val cleanUrl = cleanImageUrl(imageUrl)
                    if (isValidImageUrl(cleanUrl)) {
                        return resolveImageUrl(cleanUrl, itemLink)
                    }
                }
            }
        }

        return null
    }

    private fun cleanImageUrl(url: String): String {
        return url.trim()
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
    }

    private fun isValidImageUrl(url: String): Boolean {
        if (url.isBlank()) return false

        // Check if it's a valid URL structure
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("//")) {
            return false
        }

        // Check file extension
        val lowerUrl = url.lowercase()
        val hasImageExtension = imageExtensions.any { ext ->
            lowerUrl.contains(".$ext") || lowerUrl.endsWith(ext)
        }

        // Also accept URLs that might have image content type or are from known image hosting services
        val isImageService = lowerUrl.contains("imgur.com") ||
                lowerUrl.contains("flickr.com") ||
                lowerUrl.contains("picsum.photos") ||
                lowerUrl.contains("unsplash.com") ||
                lowerUrl.contains("pexels.com")

        return hasImageExtension || isImageService || lowerUrl.contains("image")
    }

    private fun resolveImageUrl(imageUrl: String, baseUrl: String?): String {
        return when {
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            imageUrl.startsWith("//") -> "https:$imageUrl"
            imageUrl.startsWith("/") && baseUrl != null -> {
                try {
                    val url = URL(baseUrl)
                    "${url.protocol}://${url.host}$imageUrl"
                } catch (e: Exception) {
                    imageUrl
                }
            }
            else -> imageUrl
        }
    }

    private fun cleanText(text: String): String {
        return text.trim()
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("\n+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun cleanHtmlContent(content: String?): String {
        if (content.isNullOrBlank()) return ""

        return content
            .replace("<[^>]*>".toRegex(), "") // Remove HTML tags
            .replace("&nbsp;", " ")           // Replace &nbsp; with space
            .replace("&lt;", "<")             // Replace &lt; with <
            .replace("&gt;", ">")             // Replace &gt; with >
            .replace("&amp;", "&")            // Replace &amp; with &
            .replace("&quot;", "\"")          // Replace &quot; with "
            .replace("&apos;", "'")           // Replace &apos; with '
            .replace("\n+".toRegex(), "\n\n") // Normalize line breaks
            .replace("\\s+".toRegex(), " ")   // Normalize whitespace
            .trim()
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
        // Also clear web scraper cache
        webScraper.clearCache()
    }
}