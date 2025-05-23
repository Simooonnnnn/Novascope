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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.novascope.model.NewsItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeNewsCard(
    newsItem: NewsItem,
    onBookmarkClick: (NewsItem) -> Unit,
    onCardClick: (NewsItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = remember(newsItem.id) { { onCardClick(newsItem) } },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp
        )
    ) {
        Column {
            // Article image
            newsItem.imageUrl?.let { url ->
                AsyncImage(
                    model = remember(url) {
                        ImageRequest.Builder(context)
                            .data(url)
                            .crossfade(true)
                            .size(800, 400)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Source row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    newsItem.sourceIconUrl?.let { iconUrl ->
                        AsyncImage(
                            model = remember(iconUrl) {
                                ImageRequest.Builder(context)
                                    .data(iconUrl)
                                    .crossfade(true)
                                    .size(40, 40)
                                    .build()
                            },
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

                Text(
                    text = newsItem.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row
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
                        onClick = remember(newsItem) { { onBookmarkClick(newsItem) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (newsItem.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (newsItem.isBookmarked) "Remove bookmark" else "Add bookmark",
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
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = remember(newsItem.id) { { onCardClick(newsItem) } },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Content column
            Column(modifier = Modifier.weight(1f)) {
                // Source row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    newsItem.sourceIconUrl?.let { iconUrl ->
                        AsyncImage(
                            model = remember(iconUrl) {
                                ImageRequest.Builder(context)
                                    .data(iconUrl)
                                    .crossfade(true)
                                    .size(32, 32)
                                    .build()
                            },
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

                Spacer(modifier = Modifier.height(8.dp))

                // Time and bookmark row
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
                        onClick = remember(newsItem) { { onBookmarkClick(newsItem) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (newsItem.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (newsItem.isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Article thumbnail
            newsItem.imageUrl?.let { url ->
                Spacer(modifier = Modifier.width(16.dp))
                AsyncImage(
                    model = remember(url) {
                        ImageRequest.Builder(context)
                            .data(url)
                            .crossfade(true)
                            .size(200, 200)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}