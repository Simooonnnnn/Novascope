package com.yourdomain.novascope.ui.screens

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourdomain.novascope.model.NewsItem
import com.yourdomain.novascope.model.SampleData
import com.yourdomain.novascope.ui.components.LargeNewsCard
import com.yourdomain.novascope.ui.components.SmallNewsCard
import com.yourdomain.novascope.ui.theme.NovascopeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    var newsItems by remember { mutableStateOf(SampleData.newsItems) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Novascope",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add feed dialog */ }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add feed"
                        )
                    }
                    IconButton(onClick = { /* TODO: Show notifications */ }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    text = "For You",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // First item is large card
            item {
                newsItems.firstOrNull { it.isBigArticle }?.let { item ->
                    LargeNewsCard(
                        newsItem = item,
                        onBookmarkClick = { newsItem ->
                            // Update bookmarked state
                            newsItems = newsItems.map {
                                if (it.id == newsItem.id) it.copy(isBookmarked = !it.isBookmarked)
                                else it
                            }
                        },
                        onCardClick = { /* TODO: Open article detail */ }
                    )
                }
            }

            // Rest are small cards
            items(newsItems.filterNot { it.isBigArticle }) { item ->
                SmallNewsCard(
                    newsItem = item,
                    onBookmarkClick = { newsItem ->
                        // Update bookmarked state
                        newsItems = newsItems.map {
                            if (it.id == newsItem.id) it.copy(isBookmarked = !it.isBookmarked)
                            else it
                        }
                    },
                    onCardClick = { /* TODO: Open article detail */ }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NovascopeTheme {
        HomeScreen()
    }
}