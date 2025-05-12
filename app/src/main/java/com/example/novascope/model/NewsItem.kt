package com.example.novascope.model

data class NewsItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val sourceIconUrl: String?,
    val sourceName: String,
    val publishTime: String,
    val isBookmarked: Boolean = false,
    val isBigArticle: Boolean = false,
    val content: String? = null,
    val url: String? = null
)

// Sample data for preview
object SampleData {
    val newsItems = listOf(
        NewsItem(
            id = "1",
            title = "Lorem ipsum dolor sit amet bla bla bla random news artikel wo sowieso vo AI gschribe worde isch oder so",
            imageUrl = "https://picsum.photos/seed/1/800/400",
            sourceIconUrl = "https://picsum.photos/seed/icon1/50/50",
            sourceName = "News Source",
            publishTime = "2h",
            isBookmarked = false,
            isBigArticle = true
        ),
        NewsItem(
            id = "2",
            title = "Lorem ipsum dolor sit amet bla bla bla random news artikel wo sowieso vo AI gschribe worde isch oder so",
            imageUrl = "https://picsum.photos/seed/2/300/200",
            sourceIconUrl = "https://picsum.photos/seed/icon2/50/50",
            sourceName = "News Source",
            publishTime = "2h",
            isBookmarked = false,
            isBigArticle = false
        ),
        NewsItem(
            id = "3",
            title = "Lorem ipsum dolor sit amet bla bla bla random news artikel wo sowieso vo AI gschribe worde isch oder so",
            imageUrl = "https://picsum.photos/seed/3/300/200",
            sourceIconUrl = "https://picsum.photos/seed/icon3/50/50",
            sourceName = "News Source",
            publishTime = "2h",
            isBookmarked = false,
            isBigArticle = false
        )
    )
}