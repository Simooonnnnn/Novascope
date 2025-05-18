// app/src/main/java/com/example/novascope/ui/screens/ArticleDetailScreen.kt
package com.example.novascope.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.novascope.ai.SummaryState
import com.example.novascope.model.NewsItem
import com.example.novascope.ui.components.AiSummaryCard
import com.example.novascope.viewmodel.NovascopeViewModel

private const val TAG = "ArticleDetailScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    viewModel: NovascopeViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Add logging for debugging
    LaunchedEffect(articleId) {
        Log.d(TAG, "Loading article with ID: $articleId")
        viewModel.selectArticle(articleId)
    }

    // Find article with better fallback handling
    val article = remember(articleId, uiState.selectedArticle, uiState.newsItems, uiState.bookmarkedItems) {
        val selected = uiState.selectedArticle
        val fromNews = uiState.newsItems.find { it.id == articleId }
        val fromBookmarks = uiState.bookmarkedItems.find { it.id == articleId }

        // Log which source we found the article in
        when {
            selected != null && selected.id == articleId -> {
                Log.d(TAG, "Article found in selectedArticle")
                selected
            }
            fromNews != null -> {
                Log.d(TAG, "Article found in newsItems")
                fromNews
            }
            fromBookmarks != null -> {
                Log.d(TAG, "Article found in bookmarkedItems")
                fromBookmarks
            }
            else -> {
                Log.e(TAG, "Article not found anywhere: $articleId")
                null
            }
        }
    }

    // If article is null, show a loading state
    if (article == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading article...")
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onBackClick) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Log article content for debugging
    LaunchedEffect(article) {
        Log.d(TAG, "Article loaded: ${article.title}")
        Log.d(TAG, "Has content: ${!article.content.isNullOrBlank()}")
        Log.d(TAG, "Has image: ${!article.imageUrl.isNullOrBlank()}")
        Log.d(TAG, "Has URL: ${!article.url.isNullOrBlank()}")
    }

    val summaryState = uiState.summaryState

    // UI state
    var showSummary by remember { mutableStateOf(true) }
    val isBookmarked = remember(article.id, uiState.bookmarkedItems) {
        uiState.bookmarkedItems.any { it.id == article.id }
    }

    // Prepare share action outside of composable
    val shareArticle = {
        if (!article.url.isNullOrBlank()) {
            try {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, article.url)
                    putExtra(Intent.EXTRA_TITLE, article.title)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing article: ${e.message}")
            }
        }
    }

    // Prepare URL open action outside of composable
    val openArticleUrl = {
        if (!article.url.isNullOrBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening URL: ${e.message}")
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { /* Empty title */ },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // AI summary toggle
                    IconButton(onClick = { showSummary = !showSummary }) {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = if (showSummary) "Hide Summary" else "Show Summary",
                            tint = if (showSummary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Bookmark button
                    IconButton(onClick = { viewModel.toggleBookmark(article.id) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share button (only if article has URL)
                    if (!article.url.isNullOrBlank()) {
                        IconButton(onClick = shareArticle) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share"
                            )
                        }
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
            // Only show FAB if article has URL
            if (!article.url.isNullOrBlank()) {
                FloatingActionButton(
                    onClick = openArticleUrl,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInBrowser,
                        contentDescription = "Open in Browser"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Image Header
            item {
                ArticleHeaderImage(article)
            }

            // AI Summary Card (if enabled)
            if (showSummary) {
                item {
                    AiSummaryCard(
                        summaryState = summaryState,
                        onRetry = { viewModel.selectArticle(article.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Article Content
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Improved content display
                    if (article.content.isNullOrBlank()) {
                        // No content available, show message with button to open in browser
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No content available for this article.",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (!article.url.isNullOrBlank()) {
                                Button(onClick = openArticleUrl) {
                                    Icon(
                                        imageVector = Icons.Rounded.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open in Browser")
                                }
                            }
                        }
                    } else {
                        // Clean up the content for display
                        val cleanContent = cleanHtmlContent(article.content)

                        Text(
                            text = cleanContent,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Add "Read more" button if URL is available
                        if (!article.url.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = openArticleUrl,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Read Full Article")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// Helper function to clean HTML content
private fun cleanHtmlContent(content: String?): String {
    if (content.isNullOrBlank()) return ""

    return content
        .replace("<[^>]*>".toRegex(), "") // Remove HTML tags
        .replace("&nbsp;", " ")           // Replace &nbsp; with space
        .replace("&lt;", "<")             // Replace &lt; with
        .replace("&gt;", ">")             // Replace &gt; with >
        .replace("&amp;", "&")            // Replace &amp; with &
        .replace("&quot;", "\"")          // Replace &quot; with "
        .replace("&apos;", "'")           // Replace &apos; with '
        .replace("\n+".toRegex(), "\n\n") // Normalize line breaks
        .trim()
}

@Composable
fun ArticleHeaderImage(article: NewsItem) {
    val headerImageHeight = 240.dp
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerImageHeight)
    ) {
        // Default background in case image loading fails
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Article,
                contentDescription = "Article",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp)
            )
        }

        // Handle image loading separately
        if (!article.imageUrl.isNullOrBlank()) {
            // Prepare image loading outside try-catch
            val imageRequest = ImageRequest.Builder(context)
                .data(article.imageUrl)
                .crossfade(true)
                .build()

            val painter = rememberAsyncImagePainter(imageRequest)

            // Only show image when successfully loaded
            when (painter.state) {
                is AsyncImagePainter.State.Success -> {
                    Image(
                        painter = painter,
                        contentDescription = "Article image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is AsyncImagePainter.State.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                is AsyncImagePainter.State.Error -> {
                    Log.e(TAG, "Error loading article image: ${article.imageUrl}")
                    // Error state is handled by the default background already set
                }
                else -> {
                    // Other states handled by default background
                }
            }
        }

        // Gradient overlay for better text visibility - always present
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

        // Source and title overlay - always present
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
                // Source icon placeholder
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    // Handle source icon loading
                    if (!article.sourceIconUrl.isNullOrBlank()) {
                        val iconRequest = ImageRequest.Builder(context)
                            .data(article.sourceIconUrl)
                            .crossfade(true)
                            .build()

                        val iconPainter = rememberAsyncImagePainter(iconRequest)

                        // Only render when successful
                        if (iconPainter.state is AsyncImagePainter.State.Success) {
                            Image(
                                painter = iconPainter,
                                contentDescription = "Source icon",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Source name
                Text(
                    text = article.sourceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Publish time
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = article.publishTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Title - always present and reliable
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}