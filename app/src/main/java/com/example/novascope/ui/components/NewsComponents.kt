// app/src/main/java/com/example/novascope/ui/components/NewsComponents.kt
package com.example.novascope.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.novascope.model.NewsItem
import com.example.novascope.model.SampleData
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.theme.NovascopeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeNewsCard(
    newsItem: NewsItem,
    onBookmarkClick: (NewsItem) -> Unit,
    onCardClick: (NewsItem) -> Unit,
    onShareClick: (NewsItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(newsItem.isBookmarked) }
    var isPressed by remember { mutableStateOf(false) }

    // Enhanced animations with Material motion
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card scale"
    )

    // Elevation animation for Material depth effect
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 4f,
        animationSpec = tween(
            durationMillis = MaterialMotion.DURATION_SHORT,
            easing = MaterialMotion.StandardEasing
        ),
        label = "card elevation"
    )

    LaunchedEffect(newsItem.isBookmarked) {
        isBookmarked = newsItem.isBookmarked
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            isPressed = true
            // Simulate press and release
            onCardClick(newsItem)
            isPressed = false
        }
    ) {
        Column {
            Box {
                // Article image
                newsItem.imageUrl?.let { url ->
                    Image(
                        painter = rememberAsyncImagePainter(url),
                        contentDescription = "Article image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(186.dp),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                )

                // Actions overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share button with ripple effect
                    FilledIconButton(
                        onClick = { onShareClick(newsItem) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share article",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // News source with icon
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    newsItem.sourceIconUrl?.let { url ->
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = "Source icon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = newsItem.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Article title
            Text(
                text = newsItem.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Bottom row with time and bookmark
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = newsItem.publishTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Enhanced bookmark button with better animation
                var bookmarkPressed by remember { mutableStateOf(false) }
                val bookmarkScale by animateFloatAsState(
                    targetValue = if (bookmarkPressed) 0.7f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "bookmark scale"
                )

                val bookmarkColor by animateColorAsState(
                    targetValue = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(
                        durationMillis = MaterialMotion.DURATION_SHORT,
                        easing = MaterialMotion.StandardEasing
                    ),
                    label = "bookmark color"
                )

                IconButton(
                    onClick = {
                        bookmarkPressed = true
                        onBookmarkClick(newsItem)
                        isBookmarked = !isBookmarked
                        bookmarkPressed = false
                    },
                    modifier = Modifier.scale(bookmarkScale)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = bookmarkColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallNewsCard(
    newsItem: NewsItem,
    onBookmarkClick: (NewsItem) -> Unit,
    onCardClick: (NewsItem) -> Unit,
    onMoreClick: (NewsItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(newsItem.isBookmarked) }
    var isPressed by remember { mutableStateOf(false) }

    // Enhanced animations
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card scale"
    )

    // Elevation animation
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 3f,
        animationSpec = tween(
            durationMillis = MaterialMotion.DURATION_SHORT,
            easing = MaterialMotion.StandardEasing
        ),
        label = "card elevation"
    )

    // Bookmark color animation
    val bookmarkColor by animateColorAsState(
        targetValue = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(
            durationMillis = MaterialMotion.DURATION_SHORT,
            easing = MaterialMotion.StandardEasing
        ),
        label = "bookmark color"
    )

    LaunchedEffect(newsItem.isBookmarked) {
        isBookmarked = newsItem.isBookmarked
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = elevation
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            isPressed = true
            onCardClick(newsItem)
            isPressed = false
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // News source with icon in a Row
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    newsItem.sourceIconUrl?.let { url ->
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = "Source icon",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = newsItem.sourceName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                // More options button
                IconButton(
                    onClick = { onMoreClick(newsItem) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Content row with title and thumbnail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Article title
                Text(
                    text = newsItem.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )

                // Article thumbnail with rounded corners
                newsItem.imageUrl?.let { url ->
                    Surface(
                        modifier = Modifier
                            .size(width = 100.dp, height = 72.dp),
                        shape = RoundedCornerShape(15.dp),
                        shadowElevation = 2.dp
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = "Article thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Bottom row with time and bookmark
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = newsItem.publishTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Enhanced bookmark button with spring animation
                var bookmarkPressed by remember { mutableStateOf(false) }
                val bookmarkScale by animateFloatAsState(
                    targetValue = if (bookmarkPressed) 0.7f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "bookmark scale"
                )

                IconButton(
                    onClick = {
                        bookmarkPressed = true
                        onBookmarkClick(newsItem)
                        isBookmarked = !isBookmarked
                        bookmarkPressed = false
                    },
                    modifier = Modifier.scale(bookmarkScale)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = bookmarkColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LargeNewsCardPreview() {
    NovascopeTheme {
        LargeNewsCard(
            newsItem = SampleData.newsItems[0],
            onBookmarkClick = {},
            onCardClick = {},
            onShareClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SmallNewsCardPreview() {
    NovascopeTheme {
        SmallNewsCard(
            newsItem = SampleData.newsItems[1],
            onBookmarkClick = {},
            onCardClick = {},
            onMoreClick = {}
        )
    }
}