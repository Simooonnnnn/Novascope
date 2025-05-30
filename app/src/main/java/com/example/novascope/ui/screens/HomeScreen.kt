// app/src/main/java/com/example/novascope/ui/screens/HomeScreen.kt
package com.example.novascope.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.text.font.FontWeight
import com.example.novascope.R
import com.example.novascope.model.NewsItem
import com.example.novascope.ui.components.LargeNewsCard
import com.example.novascope.ui.components.SmallNewsCard
import com.example.novascope.viewmodel.NovascopeViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NovascopeViewModel,
    onNewsItemClick: (String) -> Unit = {},
    onAddFeedClick: () -> Unit = {},
    useTabletLayout: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    // Swipe refresh state
    val swipeRefreshState = rememberSwipeRefreshState(
        isRefreshing = uiState.isRefreshing
    )

    // Load feeds only once when the screen is first shown
    LaunchedEffect(Unit) {
        if (uiState.newsItems.isEmpty() && !uiState.isLoading) {
            viewModel.loadFeeds(false)
        }
    }

    if (useTabletLayout) {
        // Tablet layout with grid
        Column(modifier = Modifier.fillMaxSize()) {
            // Header for tablet
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_novascope_logo),
                        contentDescription = "Novascope Logo",
                        modifier = Modifier
                            .height(32.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                    )
                }
            }

            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.refreshFeeds() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.newsItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading news...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    uiState.newsItems.isEmpty() && !uiState.isLoading -> {
                        EmptyFeedView(onAddFeedClick)
                    }
                    else -> {
                        TabletNewsContent(
                            newsItems = uiState.newsItems,
                            errorMessage = uiState.errorMessage,
                            onNewsItemClick = onNewsItemClick,
                            onBookmarkClick = viewModel::toggleBookmark,
                            lazyGridState = lazyGridState
                        )
                    }
                }
            }
        }
    } else {
        // Phone layout with single column
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_novascope_logo),
                            contentDescription = "Novascope Logo",
                            modifier = Modifier
                                .height(90.dp)
                                .padding(vertical = 4.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.refreshFeeds() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                when {
                    uiState.isLoading && uiState.newsItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading news...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    uiState.newsItems.isEmpty() && !uiState.isLoading -> {
                        EmptyFeedView(onAddFeedClick)
                    }
                    else -> {
                        PhoneNewsContent(
                            newsItems = uiState.newsItems,
                            errorMessage = uiState.errorMessage,
                            onNewsItemClick = onNewsItemClick,
                            onBookmarkClick = viewModel::toggleBookmark,
                            bottomPadding = padding.calculateBottomPadding(),
                            lazyListState = lazyListState
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletNewsContent(
    newsItems: List<NewsItem>,
    errorMessage: String?,
    onNewsItemClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    lazyGridState: androidx.compose.foundation.lazy.grid.LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = lazyGridState,
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text(
                text = "For You",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        errorMessage?.let { error ->
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // First item with large card spanning full width
        if (newsItems.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                LargeNewsCard(
                    newsItem = newsItems[0],
                    onBookmarkClick = { onBookmarkClick(newsItems[0].id) },
                    onCardClick = { onNewsItemClick(newsItems[0].id) }
                )
            }
        }

        // Remaining items in grid layout
        items(
            items = newsItems.drop(1), // Skip the first item as it's already shown
            key = { it.id }
        ) { item ->
            SmallNewsCard(
                newsItem = item,
                onBookmarkClick = { onBookmarkClick(it.id) },
                onCardClick = { onNewsItemClick(it.id) }
            )
        }

        // Add some bottom spacing for better UX
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PhoneNewsContent(
    newsItems: List<NewsItem>,
    errorMessage: String?,
    onNewsItemClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    lazyListState: LazyListState
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            bottom = bottomPadding + 16.dp,
            start = 16.dp,
            end = 16.dp,
            top = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") {
            Text(
                text = "For You",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }

        errorMessage?.let { error ->
            item(key = "error") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(
            items = newsItems,
            key = { it.id },
            contentType = { item ->
                if (newsItems.indexOf(item).let { it == 0 || (it + 1) % 5 == 0 }) "large" else "small"
            }
        ) { item ->
            val index = newsItems.indexOf(item)
            val useBigCard = index == 0 || (index + 1) % 5 == 0

            if (useBigCard) {
                LargeNewsCard(
                    newsItem = item,
                    onBookmarkClick = { onBookmarkClick(it.id) },
                    onCardClick = { onNewsItemClick(it.id) }
                )
            } else {
                SmallNewsCard(
                    newsItem = item,
                    onBookmarkClick = { onBookmarkClick(it.id) },
                    onCardClick = { onNewsItemClick(it.id) }
                )
            }
        }

        // Add some bottom spacing for better UX
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
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
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add some RSS feeds to start reading news",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pull down to refresh once you have feeds",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddFeedClick) {
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