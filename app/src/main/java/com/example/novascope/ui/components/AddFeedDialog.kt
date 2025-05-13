package com.example.novascope.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.novascope.model.FeedCategory
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch

/**
 * Optimized dialog for adding RSS feeds
 */
@Composable
fun AddFeedDialog(
    onDismiss: () -> Unit,
    onAddFeed: (String, FeedCategory) -> Unit
) {
    // State management
    var url by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FeedCategory>(FeedCategory.News) }
    var isUrlValid by remember { mutableStateOf(true) }
    var isValidating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Memoize dialog content colors for better performance
    val dialogContentColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    // Auto-focus on URL field
    LaunchedEffect(Unit) {
        delay(100)  // Small delay to ensure proper focus
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with pre-calculated layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add RSS Feed",
                        style = MaterialTheme.typography.titleLarge,
                        color = dialogContentColor
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = dialogContentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // URL field with optimized error handling
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        // Only validate when user types
                        if (!isUrlValid) {
                            isUrlValid = true
                            errorMessage = ""
                        }
                    },
                    label = { Text("Feed URL") },
                    placeholder = { Text("https://example.com/rss") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.RssFeed,
                            contentDescription = null,
                            tint = if (isUrlValid) MaterialTheme.colorScheme.primary else errorColor
                        )
                    },
                    isError = !isUrlValid,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                // Error message - only show when invalid
                AnimatedVisibility(visible = !isUrlValid) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 8.dp, top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = errorColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = errorMessage.ifEmpty { "Please enter a valid RSS feed URL" },
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                // Use LazyRow for better performance with many categories
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(FeedCategory.values()) { category ->
                        FilterChip(
                            selected = category == selectedCategory,
                            onClick = { selectedCategory = category },
                            label = { Text(text = category.title) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Basic validation first
                            if (url.isBlank()) {
                                isUrlValid = false
                                errorMessage = "URL cannot be empty"
                                return@Button
                            }

                            if (!validateBasicUrl(url)) {
                                isUrlValid = false
                                errorMessage = "Invalid URL format"
                                return@Button
                            }

                            // Attempt to validate against standard URL schemes
                            val formattedUrl = formatUrl(url)

                            // Add the feed
                            onAddFeed(formattedUrl, selectedCategory)
                            onDismiss()
                        },
                        enabled = !isValidating
                    ) {
                        Text("Add Feed")
                    }
                }
            }
        }
    }
}

// More robust URL validation
private fun validateBasicUrl(url: String): Boolean {
    // Basic URL validation - should start with http:// or https:// and contain at least one dot
    return url.isNotBlank() &&
            (url.startsWith("http://") || url.startsWith("https://")) &&
            url.contains(".")
}

// Format URL to ensure proper scheme
private fun formatUrl(url: String): String {
    var formattedUrl = url.trim()

    // Add https:// if no scheme is present
    if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
        formattedUrl = "https://$formattedUrl"
    }

    return formattedUrl
}