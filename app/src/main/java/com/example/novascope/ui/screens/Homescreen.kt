// app/src/main/java/com/example/novascope/ui/screens/HomeScreen.kt
package com.example.novascope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.novascope.model.NewsItem
import com.example.novascope.model.SampleData
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.components.LargeNewsCard
import com.example.novascope.ui.components.SmallNewsCard

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNewsItemClick: (NewsItem) -> Unit = {},
    onBookmarkClick: (NewsItem) -> Unit = {},
    onAddFeedClick: () -> Unit = {}
) {
    var newsItems by remember { mutableStateOf(SampleData.newsItems) }
    var isLoading by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Animation values
    var addButtonPressed by remember { mutableStateOf(false) }
    val addButtonScale by animateFloatAsState(
        targetValue = if (addButtonPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "add button scale"
    )

    var notificationButtonPressed by remember { mutableStateOf(false) }
    val notificationButtonScale by animateFloatAsState(
        targetValue = if (notificationButtonPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "notification button scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                top = 75.dp,  // Align with Figma (75px padding top)
                bottom = 83.dp, // Space for bottom navigation
                start = 20.dp,  // Align with Figma (20px padding)
                end = 20.dp     // Align with Figma (20px padding)
            ),
            verticalArrangement = Arrangement.spacedBy(15.dp), // Align with Figma (15px gap)
            modifier = Modifier.fillMaxSize()
        ) {
            // App Header with Novascope title and icons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Novascope title - using exact Figma size
                    Text(
                        text = "Novascope",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Color(0xFF1D1B20), // Matching Figma's #1D1B20
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp, // Matching large title in Figma
                            letterSpacing = 0.sp
                        )
                    )

                    // Right side icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp) // Figma has 10px gap
                    ) {
                        // Add button
                        IconButton(
                            onClick = {
                                addButtonPressed = true
                                onAddFeedClick()
                                addButtonPressed = false
                            },
                            modifier = Modifier
                                .size(30.dp) // Matching Figma's 30x30 size
                                .scale(addButtonScale)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add feed",
                                tint = Color(0xFF1D1B20), // Matching Figma's #1D1B20
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Notification button
                        IconButton(
                            onClick = {
                                notificationButtonPressed = true
                                notificationButtonPressed = false
                            },
                            modifier = Modifier
                                .size(30.dp) // Matching Figma's 30x30 size
                                .scale(notificationButtonScale)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF1D1B20), // Matching Figma's #1D1B20
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // For You section title
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = MaterialMotion.DURATION_MEDIUM,
                            easing = MaterialMotion.EmphasizedDecelerateEasing
                        )
                    ) + expandVertically(
                        animationSpec = tween(
                            durationMillis = MaterialMotion.DURATION_MEDIUM,
                            easing = MaterialMotion.EmphasizedDecelerateEasing
                        )
                    )
                ) {
                    Text(
                        text = "For You",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color(0xFF1D1B20), // Matching Figma's #1D1B20
                            fontWeight = FontWeight.Normal,
                            fontSize = 24.sp // Matching Figma's headline/small
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            // First item is large card - featured article
            item {
                newsItems.firstOrNull { it.isBigArticle }?.let { item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = MaterialMotion.DURATION_MEDIUM,
                                easing = MaterialMotion.EmphasizedDecelerateEasing
                            )
                        ) + expandVertically(
                            animationSpec = tween(
                                durationMillis = MaterialMotion.DURATION_MEDIUM,
                                easing = MaterialMotion.EmphasizedDecelerateEasing
                            )
                        )
                    ) {
                        LargeNewsCard(
                            newsItem = item,
                            onBookmarkClick = {
                                newsItems = newsItems.map {
                                    if (it.id == item.id) it.copy(isBookmarked = !it.isBookmarked)
                                    else it
                                }
                                onBookmarkClick(item)
                            },
                            onCardClick = { onNewsItemClick(item) }
                        )
                    }
                }
            }

            // Smaller news cards
            items(
                items = newsItems.filterNot { it.isBigArticle },
                key = { it.id }
            ) { item ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = MaterialMotion.DURATION_MEDIUM,
                            easing = MaterialMotion.EmphasizedDecelerateEasing
                        )
                    ) + expandVertically(
                        animationSpec = tween(
                            durationMillis = MaterialMotion.DURATION_MEDIUM,
                            easing = MaterialMotion.EmphasizedDecelerateEasing
                        )
                    )
                ) {
                    SmallNewsCard(
                        newsItem = item,
                        onBookmarkClick = {
                            newsItems = newsItems.map {
                                if (it.id == item.id) it.copy(isBookmarked = !it.isBookmarked)
                                else it
                            }
                            onBookmarkClick(item)
                        },
                        onCardClick = { onNewsItemClick(item) },
                        modifier = Modifier.height(150.dp) // Matching Figma's 150px height
                    )
                }
            }
        }

        // Pull-to-refresh progress indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 75.dp) // Positioned below the app bar
            )
        }
    }
}