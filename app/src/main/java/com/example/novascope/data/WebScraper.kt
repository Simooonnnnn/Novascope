// app/src/main/java/com/example/novascope/data/WebScraper.kt
package com.example.novascope.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class WebScraper {
    companion object {
        private const val TAG = "WebScraper"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        private const val TIMEOUT_MS = 15000L
        private const val MAX_CONTENT_LENGTH = 1024 * 1024 // 1MB limit
    }

    // Simple cache to avoid re-scraping the same articles
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes

    data class CacheEntry(
        val content: String,
        val timestamp: Long
    )

    data class ScrapedArticle(
        val title: String?,
        val content: String?,
        val author: String?,
        val publishDate: String?,
        val imageUrl: String?,
        val success: Boolean,
        val error: String? = null
    )

    /**
     * Scrape full article content from a URL
     */
    suspend fun scrapeArticle(url: String): ScrapedArticle = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Scraping article from: $url")

            // Check cache first
            cache[url]?.let { entry ->
                if (System.currentTimeMillis() - entry.timestamp < CACHE_DURATION) {
                    Log.d(TAG, "Returning cached content for: $url")
                    return@withContext parseHtmlContent(entry.content, url)
                }
            }

            // Fetch with timeout
            val htmlContent = withTimeoutOrNull(TIMEOUT_MS) {
                fetchWebContent(url)
            }

            if (htmlContent == null) {
                Log.e(TAG, "Timeout fetching content from: $url")
                return@withContext ScrapedArticle(
                    title = null,
                    content = null,
                    author = null,
                    publishDate = null,
                    imageUrl = null,
                    success = false,
                    error = "Request timed out"
                )
            }

            // Cache the raw HTML
            cache[url] = CacheEntry(htmlContent, System.currentTimeMillis())

            // Parse and extract article content
            parseHtmlContent(htmlContent, url)

        } catch (e: Exception) {
            Log.e(TAG, "Error scraping article from $url: ${e.message}", e)
            ScrapedArticle(
                title = null,
                content = null,
                author = null,
                publishDate = null,
                imageUrl = null,
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Fetch raw HTML content from URL
     */
    private suspend fun fetchWebContent(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                setRequestProperty("Accept-Encoding", "gzip, deflate")
                setRequestProperty("Connection", "keep-alive")
                connectTimeout = 10000
                readTimeout = 15000
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code for $url: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP $responseCode")
            }

            val contentLength = connection.contentLength
            if (contentLength > MAX_CONTENT_LENGTH) {
                throw Exception("Content too large: $contentLength bytes")
            }

            val inputStream = if (connection.contentEncoding == "gzip") {
                java.util.zip.GZIPInputStream(connection.inputStream)
            } else {
                connection.inputStream
            }

            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                val content = StringBuilder()
                var line: String?
                var totalChars = 0

                while (reader.readLine().also { line = it } != null) {
                    content.append(line).append("\n")
                    totalChars += line!!.length

                    // Safety check to prevent memory issues
                    if (totalChars > MAX_CONTENT_LENGTH) {
                        Log.w(TAG, "Content too large, truncating")
                        break
                    }
                }

                content.toString()
            }

        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parse HTML and extract article content using various strategies
     */
    private fun parseHtmlContent(html: String, url: String): ScrapedArticle {
        try {
            // Extract title
            val title = extractTitle(html)

            // Extract main content using multiple strategies
            val content = extractMainContent(html)

            // Extract metadata
            val author = extractAuthor(html)
            val publishDate = extractPublishDate(html)
            val imageUrl = extractMainImage(html, url)

            Log.d(TAG, "Extracted content length: ${content?.length ?: 0} characters")

            return ScrapedArticle(
                title = title,
                content = content,
                author = author,
                publishDate = publishDate,
                imageUrl = imageUrl,
                success = true
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HTML content: ${e.message}", e)
            return ScrapedArticle(
                title = null,
                content = null,
                author = null,
                publishDate = null,
                imageUrl = null,
                success = false,
                error = "Failed to parse content"
            )
        }
    }

    /**
     * Extract title from HTML
     */
    private fun extractTitle(html: String): String? {
        // Try different title extraction methods
        val titlePatterns = listOf(
            // Open Graph title
            """<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            // Twitter title
            """<meta\s+name=["']twitter:title["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            // Article title
            """<meta\s+property=["']article:title["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            // Regular title tag
            """<title[^>]*>([^<]+)</title>""".toRegex(RegexOption.IGNORE_CASE),
            // H1 tags
            """<h1[^>]*>([^<]+)</h1>""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in titlePatterns) {
            val match = pattern.find(html)
            if (match != null) {
                return cleanText(match.groupValues[1])
            }
        }

        return null
    }

    /**
     * Extract main article content using multiple strategies
     */
    private fun extractMainContent(html: String): String? {
        // Try different content extraction strategies
        val strategies = listOf(
            ::extractByCommonSelectors,
            ::extractByStructureAnalysis,
            ::extractByTextDensity
        )

        for (strategy in strategies) {
            val content = strategy(html)
            if (!content.isNullOrBlank() && content.length > 200) {
                return content
            }
        }

        return null
    }

    /**
     * Extract content using common article selectors
     */
    private fun extractByCommonSelectors(html: String): String? {
        val commonSelectors = listOf(
            // Common article containers
            """<article[^>]*>(.*?)</article>""",
            """<div[^>]*class=["'][^"']*article[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*class=["'][^"']*content[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*class=["'][^"']*post[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*class=["'][^"']*story[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*class=["'][^"']*entry[^"']*["'][^>]*>(.*?)</div>""",

            // Specific news site patterns
            """<div[^>]*class=["'][^"']*article-body[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*class=["'][^"']*post-content[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*class=["'][^"']*entry-content[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*class=["'][^"']*story-body[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*id=["'][^"']*content[^"']*["'][^>]*>(.*?)</div>""",
            """<div[^>]*id=["'][^"']*article[^"']*["'][^>]*>(.*?)</div>""",

            // Main content areas
            """<main[^>]*>(.*?)</main>""",
            """<section[^>]*class=["'][^"']*content[^"']*["'][^>]*>(.*?)</section>"""
        )

        for (selector in commonSelectors) {
            val regex = selector.toRegex(RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(html)
            if (match != null) {
                val rawContent = match.groupValues[1]
                val cleanedContent = extractTextFromHtml(rawContent)
                if (cleanedContent.length > 200) {
                    return cleanedContent
                }
            }
        }

        return null
    }

    /**
     * Extract content by analyzing HTML structure
     */
    private fun extractByStructureAnalysis(html: String): String? {
        // Look for the largest paragraph container
        val paragraphPattern = """<p[^>]*>([^<]+(?:<[^>]+>[^<]*)*)</p>""".toRegex(RegexOption.IGNORE_CASE)
        val paragraphs = paragraphPattern.findAll(html).map {
            extractTextFromHtml(it.groupValues[1])
        }.filter { it.length > 50 }.toList()

        if (paragraphs.size >= 3) {
            return paragraphs.joinToString("\n\n")
        }

        return null
    }

    /**
     * Extract content by finding areas with highest text density
     */
    private fun extractByTextDensity(html: String): String? {
        // Simple approach: find div with most text content
        val divPattern = """<div[^>]*>(.*?)</div>""".toRegex(RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
        val divs = divPattern.findAll(html).map { match ->
            val content = extractTextFromHtml(match.groupValues[1])
            content to content.length
        }.filter { it.second > 300 }
            .sortedByDescending { it.second }
            .take(1)

        return divs.firstOrNull()?.first
    }

    /**
     * Extract author information
     */
    private fun extractAuthor(html: String): String? {
        val authorPatterns = listOf(
            """<meta\s+name=["']author["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<meta\s+property=["']article:author["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<span[^>]*class=["'][^"']*author[^"']*["'][^>]*>([^<]+)</span>""".toRegex(RegexOption.IGNORE_CASE),
            """<div[^>]*class=["'][^"']*author[^"']*["'][^>]*>([^<]+)</div>""".toRegex(RegexOption.IGNORE_CASE),
            """<a[^>]*class=["'][^"']*author[^"']*["'][^>]*>([^<]+)</a>""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in authorPatterns) {
            val match = pattern.find(html)
            if (match != null) {
                return cleanText(match.groupValues[1])
            }
        }

        return null
    }

    /**
     * Extract publish date
     */
    private fun extractPublishDate(html: String): String? {
        val datePatterns = listOf(
            """<meta\s+property=["']article:published_time["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<meta\s+name=["']publish_date["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<time[^>]*datetime=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<span[^>]*class=["'][^"']*date[^"']*["'][^>]*>([^<]+)</span>""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in datePatterns) {
            val match = pattern.find(html)
            if (match != null) {
                return cleanText(match.groupValues[1])
            }
        }

        return null
    }

    /**
     * Extract main image URL
     */
    private fun extractMainImage(html: String, baseUrl: String): String? {
        val imagePatterns = listOf(
            """<meta\s+property=["']og:image["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<meta\s+name=["']twitter:image["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<img[^>]*class=["'][^"']*featured[^"']*["'][^>]*src=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE),
            """<img[^>]*src=["']([^"']+)["'][^>]*class=["'][^"']*featured[^"']*["']""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in imagePatterns) {
            val match = pattern.find(html)
            if (match != null) {
                val imageUrl = match.groupValues[1]
                return resolveImageUrl(imageUrl, baseUrl)
            }
        }

        return null
    }

    /**
     * Resolve relative image URLs to absolute URLs
     */
    private fun resolveImageUrl(imageUrl: String, baseUrl: String): String {
        return when {
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            imageUrl.startsWith("//") -> "https:$imageUrl"
            imageUrl.startsWith("/") -> {
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

    /**
     * Extract clean text from HTML content
     */
    private fun extractTextFromHtml(html: String): String {
        return html
            // Remove script and style content
            .replace("""<script[^>]*>.*?</script>""".toRegex(RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL), "")
            .replace("""<style[^>]*>.*?</style>""".toRegex(RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL), "")
            // Remove comments
            .replace("""<!--.*?-->""".toRegex(RegexOption.DOT_MATCHES_ALL), "")
            // Convert br tags to line breaks
            .replace("""<br[^>]*>""".toRegex(RegexOption.IGNORE_CASE), "\n")
            // Convert p tags to paragraphs
            .replace("""</p>""".toRegex(RegexOption.IGNORE_CASE), "\n\n")
            // Remove all other HTML tags
            .replace("""<[^>]+>""".toRegex(), "")
            // Clean up HTML entities
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#8217;", "'")
            .replace("&#8220;", """)
            .replace("&#8221;", """)
            // Normalize whitespace
            .replace("""\s+""".toRegex(), " ")
            .replace("""\n\s*\n""".toRegex(), "\n\n")
            .trim()
    }

    /**
     * Clean extracted text
     */
    private fun cleanText(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("""\s+""".toRegex(), " ")
            .trim()
    }

    /**
     * Clear the cache
     */
    fun clearCache() {
        cache.clear()
    }
}