package com.yourdomain.novascope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourdomain.novascope.R
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
    var isRefreshing by remember { mutableStateOf(false) }

    // Vereinfachte Button-Animationen
    var addButtonPressed by remember { mutableStateOf(false) }
    val addButtonScale by animateFloatAsState(
        targetValue = if (addButtonPressed) 0.8f else 1f,
        label = "add button scale"
    )

    var notificationButtonPressed by remember { mutableStateOf(false) }
    val notificationButtonScale by animateFloatAsState(
        targetValue = if (notificationButtonPressed) 0.8f else 1f,
        label = "notification button scale"
    )

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // Simuliere ein Laden
            kotlinx.coroutines.delay(1500)
            isRefreshing = false
        }
    }

    Scaffold(
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
                    IconButton(
                        onClick = {
                            /* TODO: Add feed dialog */
                            addButtonPressed = true
                            // Simuliere eine Drück-Animation
                            addButtonPressed = false
                        },
                        modifier = Modifier.scale(addButtonScale)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add feed"
                        )
                    }
                    IconButton(
                        onClick = {
                            /* TODO: Show notifications */
                            notificationButtonPressed = true
                            // Simuliere eine Drück-Animation
                            notificationButtonPressed = false
                        },
                        modifier = Modifier.scale(notificationButtonScale)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
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
        // Pull-to-refresh Simulation
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
                start = 20.dp,
                end = 20.dp
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
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
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
                            onCardClick = { /* TODO: Open article detail */ }
                        )
                    }
                }
            }

            // Rest are small cards
            items(newsItems.filterNot { it.isBigArticle }) { item ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
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
                        onCardClick = { /* TODO: Open article detail */ }
                    )
                }
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