// app/src/main/java/com/example/novascope/ui/screens/ArticleDetailScreen.kt
package com.example.novascope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.novascope.model.NewsItem
import com.example.novascope.model.SampleData
import com.example.novascope.ui.theme.NovascopeTheme
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    newsItem: NewsItem,
    onBackClick: () -> Unit,
    onBookmarkClick: (NewsItem) -> Unit,
    onShareClick: (NewsItem) -> Unit,
    onOpenInBrowserClick: (NewsItem) -> Unit,
    showAiSummary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isBookmarked by remember { mutableStateOf(newsItem.isBookmarked) }
    var showSummary by remember { mutableStateOf(showAiSummary) }
    val scope = rememberCoroutineScope()

    // Calculate the parallax effect for the header image
    val headerImageHeight = 240.dp
    val headerImageHeightPx = with(LocalDensity.current) { headerImageHeight.toPx() }
    val parallaxOffset = (scrollState.value * 0.15f).coerceIn(0f, headerImageHeightPx / 2)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(  // Changed from SmallTopAppBar
                title = {
                    // Empty title - we'll show it in the content to achieve
                    // a more custom scrolling effect
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,  // Updated reference
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // AI summary toggle
                    IconButton(onClick = { showSummary = !showSummary }) {
                        Icon(
                            imageVector = if (showSummary) Icons.Rounded.Psychology else Icons.Rounded.PsychologyAlt,
                            contentDescription = if (showSummary) "Hide AI Summary" else "Show AI Summary",
                            tint = if (showSummary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Bookmark button
                    IconButton(onClick = {
                        isBookmarked = !isBookmarked
                        onBookmarkClick(newsItem)
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark Article",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share button
                    IconButton(onClick = { onShareClick(newsItem) }) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share Article"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenInBrowserClick(newsItem) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInBrowser,
                    contentDescription = "Open in Browser"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Image Header with Parallax Effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerImageHeight)
                ) {
                    newsItem.imageUrl?.let { url ->
                        Image(
                            painter = rememberAsyncImagePainter(url),
                            contentDescription = "Article featured image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = -parallaxOffset
                                    alpha = 1f - (scrollState.value / 1000f).coerceIn(0f, 0.7f)
                                }
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )

                    // Gradient overlay for better text visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )

                    // Source and title overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        // Source with icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
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
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = newsItem.publishTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Title
                        Text(
                            text = newsItem.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }

                // AI Summary Card (if enabled)
                AnimatedVisibility(
                    visible = showSummary,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 300),
                                initialOffsetY = { -it }
                            ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                            slideOutVertically(
                                animationSpec = tween(durationMillis = 200),
                                targetOffsetY = { -it }
                            )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "AI Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Text(
                                text = "This article discusses the latest advancements in AI technology, focusing on small language models that can run efficiently on mobile devices. These models offer privacy benefits by processing data locally without sending it to remote servers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Article content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = newsItem.content ?: getPlaceholderContent(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                }
            }
        }
    }
}

private fun getPlaceholderContent(): String {
    return """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec auctor, nisl eget ultricies lacinia, nisl nisl aliquam nisl, eget ultricies nisl nisl eget nisl. Donec auctor, nisl eget ultricies lacinia, nisl nisl aliquam nisl, eget ultricies nisl nisl eget nisl.
        
        Artificial intelligence (AI) has seen tremendous growth over the past decade, with applications spanning across industries from healthcare to finance. One of the most exciting developments is the ability to run powerful language models directly on mobile devices.
        
        These on-device models, often referred to as "small language models" or SLMs, represent a significant shift in how AI can be deployed. Unlike their larger counterparts that require cloud infrastructure, these compact models can operate efficiently within the constraints of a smartphone's processing capabilities.
        
        The primary advantage of on-device AI is privacy. Since data never leaves the user's device, sensitive information remains protected. This aligns with growing consumer concerns about data privacy and security in an increasingly connected world.
        
        Performance is another consideration. By eliminating the need for network requests, on-device models can respond instantly, even when internet connectivity is limited or unavailable. This makes them ideal for applications like real-time text summarization, language translation, and content recommendation.
        
        However, challenges remain. These smaller models don't match the capabilities of larger cloud-based systems, requiring careful optimization to balance performance with accuracy. Developers must make thoughtful trade-offs between model size, speed, and quality of results.
        
        Despite these challenges, the future looks promising for on-device AI. As mobile hardware continues to advance and model optimization techniques improve, we can expect these systems to become increasingly capable while maintaining their privacy and performance advantages.
    """.trimIndent()
}

@Preview(showBackground = true)
@Composable
fun ArticleDetailScreenPreview() {
    NovascopeTheme {
        ArticleDetailScreen(
            newsItem = SampleData.newsItems[0].copy(content = getPlaceholderContent()),
            onBackClick = {},
            onBookmarkClick = {},
            onShareClick = {},
            onOpenInBrowserClick = {},
            showAiSummary = true
        )
    }
}