package com.example.novascope.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.novascope.model.NewsItem
import com.example.novascope.model.SampleData
import com.example.novascope.ui.components.LargeNewsCard
import com.example.novascope.ui.components.SmallNewsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNewsItemClick: (NewsItem) -> Unit = {},
    onBookmarkClick: (NewsItem) -> Unit = {},
    onAddFeedClick: () -> Unit = {}
) {
    var newsItems by remember { mutableStateOf(SampleData.newsItems) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novascope", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = onAddFeedClick) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Feed")
                    }
                    IconButton(onClick = { /* Handle notifications */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = padding.calculateTopPadding())
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "For You",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // Featured article
            item {
                newsItems.firstOrNull { it.isBigArticle }?.let { item ->
                    LargeNewsCard(
                        newsItem = item,
                        onBookmarkClick = {
                            newsItems = newsItems.map { news ->
                                if (news.id == item.id) news.copy(isBookmarked = !news.isBookmarked)
                                else news
                            }
                            onBookmarkClick(item)
                        },
                        onCardClick = { onNewsItemClick(item) }
                    )
                }
            }

            // Regular articles
            items(
                items = newsItems.filterNot { it.isBigArticle },
                key = { it.id }
            ) { item ->
                SmallNewsCard(
                    newsItem = item,
                    onBookmarkClick = {
                        newsItems = newsItems.map { news ->
                            if (news.id == item.id) news.copy(isBookmarked = !news.isBookmarked)
                            else news
                        }
                        onBookmarkClick(item)
                    },
                    onCardClick = { onNewsItemClick(item) }
                )
            }
        }
    }
}