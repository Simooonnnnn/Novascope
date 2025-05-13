// app/src/main/java/com/example/novascope/ui/screens/SavedScreen.kt
package com.example.novascope.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    onNewsItemClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // Use derived state to avoid unnecessary recompositions when only other parts of uiState change
    val bookmarkedItems by remember(uiState.bookmarkedItems) {
        derivedStateOf { uiState.bookmarkedItems }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Articles") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (bookmarkedItems.isEmpty()) {
            // Show empty state
            EmptyBookmarksView(modifier = Modifier.padding(innerPadding))
        } else {
            // Show bookmarked items with optimized LazyColumn
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Add header to indicate count
                item {
                    Text(
                        text = "Saved Articles (${bookmarkedItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Use itemsIndexed with keys for stable identity
                itemsIndexed(
                    items = bookmarkedItems,
                    key = { _, item -> item.id } // Use article ID as stable key
                ) { _, item ->
                    SmallNewsCard(
                        newsItem = item,
                        onBookmarkClick = {
                            viewModel.toggleBookmark(item.id)
                        },
                        onCardClick = { onNewsItemClick(item.id) }
                    )
                }

                // Extra space at bottom
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyBookmarksView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Pre-computed icon size
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
                text = "Articles you bookmark will appear here for easier access",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}