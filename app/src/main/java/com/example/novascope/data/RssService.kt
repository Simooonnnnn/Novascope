package com.example.novascope.data

import android.util.Log
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import com.example.novascope.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class RssService {
    private val rssParser = RssParser()

    suspend fun fetchFeed(url: String): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                val channel = rssParser.getRssChannel(url)
                channel.items.mapIndexed { index, item ->
                    NewsItem(
                        id = item.guid ?: UUID.randomUUID().toString(),
                        title = item.title ?: "No title",
                        imageUrl = item.image ?: findImageInContent(item.content ?: ""),
                        sourceIconUrl = channel.image?.url,
                        sourceName = channel.title ?: "Unknown Source",
                        publishTime = formatPublishDate(item.pubDate ?: ""),
                        content = item.content,
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

    // Extract first image URL from HTML content
    private fun findImageInContent(content: String): String? {
        val imgPattern = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex()
        val match = imgPattern.find(content)
        return match?.groupValues?.getOrNull(1)
    }

    // Format publish date to relative time (e.g. "2h ago")
    private fun formatPublishDate(pubDate: String): String {
        // For now, just return placeholder
        // TODO: Implement proper date parsing and relative time
        return "2h"
    }
}