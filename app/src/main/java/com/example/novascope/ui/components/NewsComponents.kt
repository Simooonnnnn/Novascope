// app/src/main/java/com/example/novascope/ui/components/NewsComponents.kt
package com.example.novascope.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onShareClick: (NewsItem) -> Unit = {}, // Optional parameter
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(newsItem.isBookmarked) }
    var isPressed by remember { mutableStateOf(false) }

    // Card press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card scale"
    )

    // Bookmark button animation
    var bookmarkPressed by remember { mutableStateOf(false) }
    val bookmarkScale by animateFloatAsState(
        targetValue = if (bookmarkPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bookmark scale"
    )

    val bookmarkColor by animateColorAsState(
        targetValue = if (isBookmarked) MaterialTheme.colorScheme.primary else Color(0xFF49454F), // Figma's color
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "bookmark color"
    )

    LaunchedEffect(newsItem.isBookmarked) {
        isBookmarked = newsItem.isBookmarked
    }

    // Main card - following Figma design
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(15.dp), // Figma uses 15px border radius
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            isPressed = true
            onCardClick(newsItem)
            isPressed = false
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Article image - full width with 186px height (from Figma)
            newsItem.imageUrl?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(186.dp) // Figma height
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(url),
                        contentDescription = "Article image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .background(Color(0xFFE7E0EC)) // Light background from Figma
                    .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
            )

            // News source with icon - matching Figma layout
            Row(
                modifier = Modifier.padding(start = 15.dp, top = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source icon - 12px diameter circle in Figma
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

                // Source name - small label size in Figma (11px)
                Text(
                    text = newsItem.sourceName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF1D1B20) // Figma's text color
                )
            }

            // Article title - using exact Figma styling
            Text(
                text = newsItem.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                ),
                color = Color(0xFF1D1B20), // Figma's text color
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
            )

            // Bottom row with time and bookmark - matching Figma layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Publish time
                Text(
                    text = newsItem.publishTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF49454F) // Figma's color
                )

                // Bookmark icon - 20x20dp in Figma
                IconButton(
                    onClick = {
                        bookmarkPressed = true
                        onBookmarkClick(newsItem)
                        isBookmarked = !isBookmarked
                        bookmarkPressed = false
                    },
                    modifier = Modifier
                        .size(20.dp)
                        .scale(bookmarkScale)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = bookmarkColor,
                        modifier = Modifier.size(16.dp) // Smaller icon size to match Figma
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
    onMoreClick: ((NewsItem) -> Unit)? = null, // Make this parameter optional with a default value of null
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(newsItem.isBookmarked) }
    var isPressed by remember { mutableStateOf(false) }

    // Card press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card scale"
    )

    // Bookmark animation
    var bookmarkPressed by remember { mutableStateOf(false) }
    val bookmarkScale by animateFloatAsState(
        targetValue = if (bookmarkPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bookmark scale"
    )

    val bookmarkColor by animateColorAsState(
        targetValue = if (isBookmarked) MaterialTheme.colorScheme.primary else Color(0xFF49454F), // Figma's color
        animationSpec = tween(durationMillis = MaterialMotion.DURATION_SHORT),
        label = "bookmark color"
    )

    LaunchedEffect(newsItem.isBookmarked) {
        isBookmarked = newsItem.isBookmarked
    }

    // Main container
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .defaultMinSize(minHeight = 150.dp), // Figma's 150px height
        shape = RoundedCornerShape(15.dp), // Figma uses 15px border radius
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            isPressed = true
            onCardClick(newsItem)
            isPressed = false
        }
    ) {
        Column(
            modifier = Modifier.padding(5.dp) // Figma's 5px padding
        ) {
            // News source with icon
            Row(
                modifier = Modifier.padding(start = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source icon - 12px diameter circle in Figma
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

                // Source name - small label in Figma
                Text(
                    text = newsItem.sourceName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF1D1B20) // Figma's text color
                )

                Spacer(modifier = Modifier.weight(1f))

                // Only show the More button if onMoreClick is provided
                if (onMoreClick != null) {
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
            }

            // Content row with title and image - from Figma
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Article title - using Figma's styling
                Text(
                    text = newsItem.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    ),
                    color = Color(0xFF1D1B20), // Figma's text color
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )

                // Article thumbnail - 100x72px in Figma
                newsItem.imageUrl?.let { url ->
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 72.dp)
                            .clip(RoundedCornerShape(15.dp)) // Figma's border radius
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
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Publish time
                Text(
                    text = newsItem.publishTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF49454F) // Figma's color
                )

                // Bookmark icon - 20x20dp in Figma
                IconButton(
                    onClick = {
                        bookmarkPressed = true
                        onBookmarkClick(newsItem)
                        isBookmarked = !isBookmarked
                        bookmarkPressed = false
                    },
                    modifier = Modifier
                        .size(20.dp)
                        .scale(bookmarkScale)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = bookmarkColor,
                        modifier = Modifier.size(16.dp) // Smaller icon size to match Figma
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
            onCardClick = {}
        )
    }
}