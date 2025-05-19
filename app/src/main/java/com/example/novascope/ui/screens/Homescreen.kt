// app/src/main/java/com/example/novascope/ui/screens/HomeScreen.kt
package com.example.novascope.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.novascope.ui.components.LargeNewsCard
import com.example.novascope.ui.components.SmallNewsCard
import com.example.novascope.viewmodel.NovascopeViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.Image
import com.example.novascope.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NovascopeViewModel,
    onNewsItemClick: (String) -> Unit = {},
    onAddFeedClick: () -> Unit = {}
) {
    // Collect the entire state instead of selective parts
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // LaunchedEffect to load feeds if needed
    LaunchedEffect(Unit) {
        if (uiState.newsItems.isEmpty()) {
            scope.launch {
                viewModel.loadFeeds(false)
            }
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Show loading indicator at the top
            if (uiState.isLoading && !uiState.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (uiState.newsItems.isEmpty() && !uiState.isLoading && !uiState.isRefreshing) {
                EmptyFeedView(onAddFeedClick)
            } else {
                NewsContent(
                    newsItems = uiState.newsItems,
                    isRefreshing = uiState.isRefreshing,
                    errorMessage = uiState.errorMessage,
                    onNewsItemClick = onNewsItemClick,
                    onBookmarkClick = { viewModel.toggleBookmark(it.id) },
                    bottomPadding = padding.calculateBottomPadding()
                )
            }
        }
    }
}

@Composable
private fun NewsContent(
    newsItems: List<NewsItem>,
    isRefreshing: Boolean,
    errorMessage: String?,
    onNewsItemClick: (String) -> Unit,
    onBookmarkClick: (NewsItem) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    LazyColumn(
        contentPadding = PaddingValues(
            bottom = bottomPadding + 16.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Title header
        item(key = "header") {
            Text(
                text = "For You",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Only show refresh indicator when actually refreshing
        if (isRefreshing) {
            item(key = "refreshing") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Only show error message when present
        errorMessage?.let {
            item(key = "error") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Optimized item rendering with stable keys
        itemsIndexed(
            items = newsItems,
            key = { _, item -> item.id }
        ) { index, item ->
            val useBigCard = index == 0 || (index + 1) % 5 == 0

            if (useBigCard) {
                LargeNewsCard(
                    newsItem = item,
                    onBookmarkClick = onBookmarkClick,
                    onCardClick = { onNewsItemClick(it.id) }
                )
            } else {
                SmallNewsCard(
                    newsItem = item,
                    onBookmarkClick = onBookmarkClick,
                    onCardClick = { onNewsItemClick(it.id) }
                )
            }
        }

        item(key = "footer") {
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