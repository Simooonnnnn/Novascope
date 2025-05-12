// app/src/main/java/com/example/novascope/ui/screens/ExploreScreen.kt
package com.example.novascope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.novascope.model.SampleData
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.components.AddFeedDialog
import com.example.novascope.ui.components.FeedCategory
import com.example.novascope.ui.components.SmallNewsCard
import com.example.novascope.ui.theme.NovascopeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onAddFeed: (String, FeedCategory) -> Unit = { _, _ -> },
    onNewsItemClick: (String) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddFeedDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Sample categories
    val categories = remember {
        listOf("All", "News", "Technology", "Science", "Finance", "Sports", "Entertainment")
    }

    // Sample popular feeds
    val popularFeeds = remember {
        listOf(
            "The New York Times" to "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
            "BBC News" to "http://feeds.bbci.co.uk/news/rss.xml",
            "TechCrunch" to "https://techcrunch.com/feed/",
            "Wired" to "https://www.wired.com/feed/rss",
            "Scientific American" to "https://rss.sciam.com/ScientificAmerican-Global",
            "ESPN" to "https://www.espn.com/espn/rss/news",
            "The Verge" to "https://www.theverge.com/rss/index.xml"
        )
    }

    // Sample search results
    val searchResults = remember {
        if (searchQuery.isNotEmpty()) {
            SampleData.newsItems.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.sourceName.contains(searchQuery, ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Explore") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { showAddFeedDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
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
                onValueChange = {
                    searchQuery = it
                    isLoading = it.isNotEmpty()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search for feeds or news...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (isLoading && searchQuery.isNotEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // Category filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory || (category == "All" && selectedCategory == null)
                    val elevation by animateDpAsState(
                        targetValue = if (isSelected) 4.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "category elevation"
                    )

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = if (category == "All") null else category
                        },
                        label = {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Main content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show search results if query is not empty
                if (searchQuery.isNotEmpty()) {
                    item {
                        Text(
                            text = "Search Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(searchResults) { newsItem ->
                        SmallNewsCard(
                            newsItem = newsItem,
                            onBookmarkClick = { /* Handle bookmark */ },
                            onCardClick = { onNewsItemClick(newsItem.id) }
                            // Removed the onMoreClick parameter that was causing the error
                        )
                    }

                    if (searchResults.isEmpty()) {
                        item {
                            NoResultsMessage(searchQuery)
                        }
                    }
                } else {
                    // Default explore view
                    item {
                        Text(
                            text = "Popular Feeds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Popular feeds section
                    items(popularFeeds) { (name, url) ->
                        PopularFeedItem(
                            name = name,
                            url = url,
                            onClick = { /* Handle feed selection */ }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Discover by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                    }

                    // Category cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CategoryCard(
                                title = "News",
                                itemCount = 42,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            CategoryCard(
                                title = "Technology",
                                itemCount = 28,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CategoryCard(
                                title = "Science",
                                itemCount = 17,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            CategoryCard(
                                title = "Finance",
                                itemCount = 23,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Add more categories as needed

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        // Add Feed Dialog
        if (showAddFeedDialog) {
            AddFeedDialog(
                onDismiss = { showAddFeedDialog = false },
                onAddFeed = { url, category ->
                    onAddFeed(url, category)
                    showAddFeedDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularFeedItem(
    name: String,
    url: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add feed",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .padding(vertical = 8.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { /* Handle category selection */ },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$itemCount feeds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NoResultsMessage(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No results found for \"$query\"",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Try searching for something else or add a custom feed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExploreScreenPreview() {
    NovascopeTheme {
        ExploreScreen()
    }
}