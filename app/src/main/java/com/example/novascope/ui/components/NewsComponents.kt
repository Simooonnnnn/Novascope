package com.example.novascope.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.novascope.model.NewsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeNewsCard(
    newsItem: NewsItem,
    onBookmarkClick: (NewsItem) -> Unit,
    onCardClick: (NewsItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Derive from newsItem to avoid unnecessary recompositions
    var isBookmarked by remember(newsItem.id) { mutableStateOf(newsItem.isBookmarked) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = { onCardClick(newsItem) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            hoveredElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Column {
            // Article image - only load if available
            newsItem.imageUrl?.let { url ->
                Box(
                    modifier = Modifier.height(180.dp)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Article image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Source row with icon if available
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    newsItem.sourceIconUrl?.let { iconUrl ->
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = newsItem.sourceName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Article title
                Text(
                    text = newsItem.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row with time and bookmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = newsItem.publishTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Optimize the bookmark button
                    val iconButtonModifier = Modifier.size(36.dp)
                    IconButton(
                        onClick = {
                            isBookmarked = !isBookmarked
                            onBookmarkClick(newsItem.copy(isBookmarked = !isBookmarked))
                        },
                        modifier = iconButtonModifier
                    ) {
                        val icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
                        Icon(
                            imageVector = icon,
                            contentDescription = "Bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
    // Derive from newsItem to avoid unnecessary recompositions
    var isBookmarked by remember(newsItem.id) { mutableStateOf(newsItem.isBookmarked) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = { onCardClick(newsItem) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            hoveredElevation = 2.dp,
            pressedElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row( // Inner Row for title, source, and image
                modifier = Modifier.fillMaxWidth()
            ) {
                // Content column
                Column(modifier = Modifier.weight(1f)) {
                    // Source row with icon if available
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        newsItem.sourceIconUrl?.let { iconUrl ->
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = newsItem.sourceName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = newsItem.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Article thumbnail - only load if available
                newsItem.imageUrl?.let { url ->
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Article thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom row with time and bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = newsItem.publishTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = {
                        isBookmarked = !isBookmarked
                        onBookmarkClick(newsItem.copy(isBookmarked = !isBookmarked))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    val icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
                    Icon(
                        imageVector = icon,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}