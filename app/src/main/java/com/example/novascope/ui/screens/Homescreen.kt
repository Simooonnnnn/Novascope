package com.example.novascope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.novascope.R
import com.example.novascope.model.NewsItem
import com.example.novascope.model.SampleData
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.components.LargeNewsCard
import com.example.novascope.ui.components.SmallNewsCard
import com.example.novascope.ui.theme.NovascopeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    var newsItems by remember { mutableStateOf(SampleData.newsItems) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()

    // Button animations with Material motion
    var refreshButtonPressed by remember { mutableStateOf(false) }
    val refreshScale by animateFloatAsState(
        targetValue = if (refreshButtonPressed) 0.8f else 1f,
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "refresh button scale"
    )

    var filterButtonPressed by remember { mutableStateOf(false) }
    val filterScale by animateFloatAsState(
        targetValue = if (filterButtonPressed) 0.8f else 1f,
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "filter button scale"
    )

    var addButtonPressed by remember { mutableStateOf(false) }
    val addButtonScale by animateFloatAsState(
        targetValue = if (addButtonPressed) 0.8f else 1f,
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "add button scale"
    )

    var searchButtonPressed by remember { mutableStateOf(false) }
    val searchButtonScale by animateFloatAsState(
        targetValue = if (searchButtonPressed) 0.8f else 1f,
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "search button scale"
    )

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // Simulate loading
            delay(1500)
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_novascope_logo),
                        contentDescription = "Novascope Logo",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.height(24.dp)
                    )
                },
                actions = {
                    // Search button with animation
                    IconButton(
                        onClick = {
                            searchButtonPressed = true
                            // Simulate press animation
                            scope.launch {
                                delay(100)
                                searchButtonPressed = false
                            }
                            // TODO: Open search
                        },
                        modifier = Modifier.scale(searchButtonScale)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Filter button
                    IconButton(
                        onClick = {
                            filterButtonPressed = true
                            scope.launch {
                                delay(100)
                                filterButtonPressed = false
                            }
                            showFilterMenu = true
                        },
                        modifier = Modifier.scale(filterScale)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Filter feeds",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Refresh button
                    IconButton(
                        onClick = {
                            refreshButtonPressed = true
                            scope.launch {
                                delay(100)
                                refreshButtonPressed = false
                                isRefreshing = true
                            }
                        },
                        modifier = Modifier.scale(refreshScale)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh feeds",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (isRefreshing) 180f else 0f
                            }
                        )
                    }

                    // Add button with animation
                    IconButton(
                        onClick = {
                            addButtonPressed = true
                            // Simulate press animation
                            scope.launch {
                                delay(100)
                                addButtonPressed = false
                            }
                            // TODO: Add feed dialog
                        },
                        modifier = Modifier.scale(addButtonScale)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add feed",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Pull-to-refresh indicator
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = innerPadding.calculateTopPadding())
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = "For You",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                // First item is large card
                item {
                    newsItems.firstOrNull { it.isBigArticle }?.let { item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LargeNewsCard(
                                newsItem = item,
                                onBookmarkClick = { newsItem ->
                                    // Update bookmarked state
                                    newsItems = newsItems.map {
                                        if (it.id == newsItem.id) it.copy(isBookmarked = !it.isBookmarked)
                                        else it
                                    }
                                },
                                onCardClick = { /* TODO: Open article detail */ },
                                onShareClick = { /* TODO: Share article */ }
                            )
                        }
                    }
                }

                // Rest are small cards
                items(newsItems.filterNot { it.isBigArticle }) { item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SmallNewsCard(
                            newsItem = item,
                            onBookmarkClick = { newsItem ->
                                // Update bookmarked state
                                newsItems = newsItems.map {
                                    if (it.id == newsItem.id) it.copy(isBookmarked = !it.isBookmarked)
                                    else it
                                }
                            },
                            onCardClick = { /* TODO: Open article detail */ },
                            onMoreClick = { /* TODO: Show more options */ }
                        )
                    }
                }
            }

            // Filter menu dropdown
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false },
                modifier = Modifier
                    .padding(16.dp)
                    .width(200.dp)
            ) {
                Text(
                    text = "Filter Feeds",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("All Feeds") },
                    onClick = {
                        showFilterMenu = false
                        // TODO: Filter by all feeds
                    }
                )
                DropdownMenuItem(
                    text = { Text("News") },
                    onClick = {
                        showFilterMenu = false
                        // TODO: Filter by news category
                    }
                )
                DropdownMenuItem(
                    text = { Text("Technology") },
                    onClick = {
                        showFilterMenu = false
                        // TODO: Filter by technology category
                    }
                )
                DropdownMenuItem(
                    text = { Text("Science") },
                    onClick = {
                        showFilterMenu = false
                        // TODO: Filter by science category
                    }
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