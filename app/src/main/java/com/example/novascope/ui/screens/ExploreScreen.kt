package com.example.novascope.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.novascope.model.Feed
import com.example.novascope.model.FeedCategory
import com.example.novascope.ui.components.AddFeedDialog
import com.example.novascope.viewmodel.NovascopeViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: NovascopeViewModel,
    onNewsItemClick: (String) -> Unit = {}
) {
    val feeds by viewModel.feeds.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Local UI states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FeedCategory?>(null) }
    var showAddFeedDialog by remember { mutableStateOf(false) }

    // Derived state for filtered feeds - only recalculated when inputs change
    val displayFeeds = remember(feeds, selectedCategory, searchQuery) {
        feeds.filter { feed ->
            (selectedCategory == null || feed.category == selectedCategory) &&
                    (searchQuery.isEmpty() || feed.name.contains(searchQuery, ignoreCase = true) ||
                            feed.url.contains(searchQuery, ignoreCase = true))
        }
    }

    // Derived values instead of additional state variables
    val isSearching = searchQuery.isNotEmpty()
    val isEmpty = displayFeeds.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Feeds") },
                actions = {
                    IconButton(onClick = { showAddFeedDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Feed"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search for feeds...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // Category filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" category
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                }

                // Feed categories - use efficient key-based rendering
                items(
                    items = FeedCategory.values(),
                    key = { it.name }
                ) { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { selectedCategory = if (category == selectedCategory) null else category },
                        label = { Text(category.title) },
                        leadingIcon = if (category == selectedCategory) {
                            { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Main content
            if (isEmpty) {
                if (isSearching) {
                    NoSearchResultsMessage(searchQuery)
                } else {
                    EmptyFeedsMessage(
                        category = selectedCategory?.title,
                        onAddFeedClick = { showAddFeedDialog = true }
                    )
                }
            } else {
                FeedsList(
                    feeds = displayFeeds,
                    onToggleEnabled = { id, enabled -> viewModel.toggleFeedEnabled(id, enabled) },
                    onDelete = { viewModel.deleteFeed(it) }
                )
            }
        }

        // Add Feed Dialog - only render when visible
        if (showAddFeedDialog) {
            AddFeedDialog(
                onDismiss = { showAddFeedDialog = false },
                onAddFeed = { url, category ->
                    scope.launch {
                        val feedName = viewModel.getFeedInfoFromUrl(url)
                        viewModel.addFeed(feedName, url, category)
                    }
                    showAddFeedDialog = false
                }
            )
        }
    }
}

@Composable
private fun FeedsList(
    feeds: List<Feed>,
    onToggleEnabled: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header") {
            Text(
                text = "Your Feeds (${feeds.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(
            items = feeds,
            key = { it.id }
        ) { feed ->
            key(feed.id) {  // Additional key for better recomposition control
                FeedItem(
                    feed = feed,
                    onToggleEnabled = onToggleEnabled,
                    onDelete = onDelete
                )
            }
        }

        item(key = "footer") {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* Add feed handled by dialog */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Add New Feed")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedItem(
    feed: Feed,
    onToggleEnabled: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Feed icon - use more efficient AsyncImage with crossfade
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (feed.iconUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(feed.iconUrl)
                            .crossfade(true)
                            .size(72, 72)
                            .build(),
                        contentDescription = "Feed icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.RssFeed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = feed.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = feed.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = feed.category.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = feed.isEnabled,
                    onCheckedChange = { onToggleEnabled(feed.id, it) }
                )

                // Delete button (only for non-default feeds)
                if (!feed.isDefault) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete feed",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog - only render when needed
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Feed") },
            text = { Text("Are you sure you want to delete \"${feed.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(feed.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyFeedsMessage(
    category: String? = null,
    onAddFeedClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.RssFeed,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (category != null) "No $category feeds" else "No feeds yet",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add feeds to keep up with your favorite sources",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onAddFeedClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Feed")
            }
        }
    }
}

@Composable
fun NoSearchResultsMessage(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No feeds found for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Try a different search term or add a new feed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}