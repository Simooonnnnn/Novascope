package com.example.novascope.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.novascope.model.NewsItem
import com.example.novascope.ui.components.SmallNewsCard
import com.example.novascope.viewmodel.NovascopeViewModel

/**
 * Optimized screen for displaying saved/bookmarked articles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: NovascopeViewModel,
    onNewsItemClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Use derived state to avoid unnecessary recompositions
    val bookmarkedItems by remember(uiState.bookmarkedItems) {
        derivedStateOf { uiState.bookmarkedItems }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Articles") }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            if (bookmarkedItems.isEmpty()) {
                EmptyBookmarksView()
            } else {
                SavedArticlesList(
                    bookmarkedItems = bookmarkedItems,
                    onNewsItemClick = onNewsItemClick,
                    onBookmarkClick = { viewModel.toggleBookmark(it.id) }
                )
            }
        }
    }
}

@Composable
private fun SavedArticlesList(
    bookmarkedItems: List<NewsItem>,
    onNewsItemClick: (String) -> Unit,
    onBookmarkClick: (NewsItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Saved Articles (${bookmarkedItems.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Use stable keys for items
        items(
            items = bookmarkedItems,
            key = { it.id }
        ) { item ->
            SmallNewsCard(
                newsItem = item,
                onBookmarkClick = onBookmarkClick,
                onCardClick = { onNewsItemClick(item.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmptyBookmarksView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkRemove,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No saved articles yet",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Articles you bookmark will appear here",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}