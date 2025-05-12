package com.yourdomain.novascope.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.yourdomain.novascope.model.NewsItem
import com.yourdomain.novascope.model.SampleData
import com.yourdomain.novascope.ui.theme.NovascopeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeNewsCard(
    newsItem: NewsItem,
    onBookmarkClick: (NewsItem) -> Unit,
    onCardClick: (NewsItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(newsItem.isBookmarked) }
    var isPressed by remember { mutableStateOf(false) }

    // Verwende animateFloatAsState statt Animatable für einfachere Implementierung
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "card scale"
    )

    LaunchedEffect(newsItem.isBookmarked) {
        isBookmarked = newsItem.isBookmarked
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            isPressed = true
            // Simuliere drücken und loslassen
            onCardClick(newsItem)
            isPressed = false
        }
    ) {
        Column {
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
            }

            // News source with icon
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
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
                Text(
                    text = newsItem.publishTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Einfacherer Bookmark-Button mit Animation
                var bookmarkPressed by remember { mutableStateOf(false) }
                val bookmarkScale by animateFloatAsState(
                    targetValue = if (bookmarkPressed) 0.8f else 1f,
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
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(newsItem.isBookmarked) }
    var isPressed by remember { mutableStateOf(false) }

    // Vereinfachte Animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "card scale"
    )

    LaunchedEffect(newsItem.isBookmarked) {
        isBookmarked = newsItem.isBookmarked
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            isPressed = true
            onCardClick(newsItem)
            isPressed = false
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // News source with icon
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
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

            // Content row with title and thumbnail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                // Article thumbnail
                newsItem.imageUrl?.let { url ->
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 72.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                Text(
                    text = newsItem.publishTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Einfacherer Bookmark-Button
                var bookmarkPressed by remember { mutableStateOf(false) }
                val bookmarkScale by animateFloatAsState(
                    targetValue = if (bookmarkPressed) 0.8f else 1f,
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
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
            onCardClick = {}
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
            onCardClick = {}
        )
    }
}