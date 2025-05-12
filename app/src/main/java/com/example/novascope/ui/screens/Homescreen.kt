package com.example.novascope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.novascope.model.NewsItem
import com.example.novascope.ui.components.LargeNewsCard
import com.example.novascope.ui.components.SmallNewsCard
import com.example.novascope.viewmodel.NovascopeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NovascopeViewModel,
    onNewsItemClick: (String) -> Unit = {},
    onAddFeedClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val newsItems = uiState.newsItems
    val isLoading = uiState.isLoading
    val isRefreshing = uiState.isRefreshing
    val errorMessage = uiState.errorMessage

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novascope", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.loadFeeds(true)
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Feeds")
                    }
                    IconButton(onClick = onAddFeedClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Feed")
                    }
                    IconButton(onClick = { /* Handle notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            if (isLoading && !isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (newsItems.isEmpty() && !isLoading) {
                EmptyFeedView(onAddFeedClick)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
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

                    // Refresh indicator
                    if (isRefreshing) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Error message
                    if (errorMessage != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    // Featured article
                    val featuredArticle = newsItems.firstOrNull { it.isBigArticle }
                    if (featuredArticle != null) {
                        item {
                            LargeNewsCard(
                                newsItem = featuredArticle,
                                onBookmarkClick = {
                                    viewModel.toggleBookmark(it.id)
                                },
                                onCardClick = { onNewsItemClick(it.id) }
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
                                viewModel.toggleBookmark(it.id)
                            },
                            onCardClick = { onNewsItemClick(it.id) }
                        )
                    }

                    // Extra space at bottom
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFeedView(onAddFeedClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Articles Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add some RSS feeds to start reading news articles in your feed",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddFeedClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add RSS Feed")
        }
    }
}