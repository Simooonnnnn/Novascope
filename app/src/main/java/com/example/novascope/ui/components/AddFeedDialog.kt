// app/src/main/java/com/example/novascope/ui/components/AddFeedDialog.kt
package com.example.novascope.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.theme.NovascopeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AddFeedDialog(
    onDismiss: () -> Unit,
    onAddFeed: (String, FeedCategory) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FeedCategory.News) }
    var isUrlValid by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(300) // Short delay to ensure the dialog is visible
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add RSS Feed",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Feed URL input
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        isUrlValid = validateUrl(it)
                    },
                    label = { Text("Feed URL") },
                    placeholder = { Text("https://example.com/rss") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.RssFeed,
                            contentDescription = null
                        )
                    },
                    isError = !isUrlValid,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (!isUrlValid) {
                    Text(
                        text = "Please enter a valid RSS feed URL",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category selection
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    CategoryChipGroup(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button
                    var cancelPressed by remember { mutableStateOf(false) }
                    val cancelScale by animateFloatAsState(
                        targetValue = if (cancelPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "cancel scale"
                    )

                    TextButton(
                        onClick = {
                            cancelPressed = true
                            scope.launch {
                                delay(100)
                                cancelPressed = false
                                onDismiss()
                            }
                        },
                        modifier = Modifier.scale(cancelScale)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Add button
                    var addPressed by remember { mutableStateOf(false) }
                    val addScale by animateFloatAsState(
                        targetValue = if (addPressed) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "add scale"
                    )

                    Button(
                        onClick = {
                            if (url.isNotBlank() && isUrlValid) {
                                addPressed = true
                                scope.launch {
                                    delay(100)
                                    addPressed = false
                                    onAddFeed(url, selectedCategory)
                                    onDismiss()
                                }
                            }
                        },
                        enabled = url.isNotBlank() && isUrlValid,
                        modifier = Modifier.scale(addScale)
                    ) {
                        Text("Add Feed")
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChipGroup(
    selectedCategory: FeedCategory,
    onCategorySelected: (FeedCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeedCategory.values().forEach { category ->
            CategoryChip(
                category = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    category: FeedCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    var chipPressed by remember { mutableStateOf(false) }
    val chipScale by animateFloatAsState(
        targetValue = if (chipPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chip scale"
    )

    FilterChip(
        selected = selected,
        onClick = {
            chipPressed = true
            onClick()
            chipPressed = false
        },
        label = {
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        modifier = Modifier.scale(chipScale),
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null
    )
}

enum class FeedCategory(val title: String, val icon: ImageVector) {
    News("News", Icons.Rounded.RssFeed),
    Tech("Technology", Icons.Rounded.RssFeed),
    Science("Science", Icons.Rounded.RssFeed),
    Sports("Sports", Icons.Rounded.RssFeed),
    Finance("Finance", Icons.Rounded.RssFeed)
}

private fun validateUrl(url: String): Boolean {
    // Simple URL validation for demonstration
    return url.isEmpty() || url.startsWith("http") && url.contains(".")
}

@Preview(showBackground = true)
@Composable
fun AddFeedDialogPreview() {
    NovascopeTheme {
        AddFeedDialog(
            onDismiss = {},
            onAddFeed = { _, _ -> }
        )
    }
}