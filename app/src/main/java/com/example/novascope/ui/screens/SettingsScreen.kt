package com.example.novascope.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Optimized settings screen with better performance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    // Consolidated state management with a single state variable
    val settingsState = remember {
        mutableStateOf(
            SettingsState(
                themeMode = ThemeMode.SYSTEM,
                useDynamicColor = true,
                enableNotifications = true,
                enableAiSummary = true,
                textSize = TextSize.MEDIUM
            )
        )
    }

    // Dialog states managed separately to only render when needed
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Use LazyColumn for better scrolling performance
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            // Better performance by using less spacing
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Appearance section
            item(key = "appearance_header") {
                SettingsSectionHeader("Appearance")
            }

            item(key = "theme") {
                SettingsItem(
                    title = "Theme",
                    subtitle = settingsState.value.themeMode.title,
                    icon = when (settingsState.value.themeMode) {
                        ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        ThemeMode.DARK -> Icons.Outlined.DarkMode
                        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                    },
                    onClick = { showThemeDialog = true }
                )
            }

            item(key = "dynamic_color") {
                SettingsToggleItem(
                    title = "Dynamic Color",
                    subtitle = "Use colors from your wallpaper (Android 12+)",
                    icon = Icons.Outlined.Palette,
                    checked = settingsState.value.useDynamicColor,
                    onToggle = {
                        settingsState.value = settingsState.value.copy(useDynamicColor = it)
                    }
                )
            }

            item(key = "text_size") {
                SettingsItem(
                    title = "Text Size",
                    subtitle = settingsState.value.textSize.title,
                    icon = Icons.Outlined.FormatSize,
                    onClick = {
                        // Cycle through text sizes
                        val newSize = when (settingsState.value.textSize) {
                            TextSize.SMALL -> TextSize.MEDIUM
                            TextSize.MEDIUM -> TextSize.LARGE
                            TextSize.LARGE -> TextSize.SMALL
                        }
                        settingsState.value = settingsState.value.copy(textSize = newSize)
                    }
                )
            }

            item(key = "divider1") {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Features section
            item(key = "features_header") {
                SettingsSectionHeader("Features")
            }

            item(key = "ai_summaries") {
                SettingsToggleItem(
                    title = "AI Summaries",
                    subtitle = "Generate summaries using on-device AI",
                    icon = Icons.Outlined.Psychology,
                    checked = settingsState.value.enableAiSummary,
                    onToggle = {
                        settingsState.value = settingsState.value.copy(enableAiSummary = it)
                    }
                )
            }

            item(key = "notifications") {
                SettingsToggleItem(
                    title = "Notifications",
                    subtitle = "Receive alerts for new articles",
                    icon = Icons.Outlined.Notifications,
                    checked = settingsState.value.enableNotifications,
                    onToggle = {
                        settingsState.value = settingsState.value.copy(enableNotifications = it)
                    }
                )
            }

            item(key = "language") {
                SettingsItem(
                    title = "Default Language",
                    subtitle = "English",
                    icon = Icons.Outlined.Language,
                    onClick = { /* Open language selector */ }
                )
            }

            item(key = "divider2") {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Data section
            item(key = "data_header") {
                SettingsSectionHeader("Data & Storage")
            }

            item(key = "clear_cache") {
                SettingsItem(
                    title = "Clear Cache",
                    subtitle = "Free up space used by images and data",
                    icon = Icons.Outlined.Delete,
                    onClick = { showClearCacheDialog = true }
                )
            }

            item(key = "refresh_feeds") {
                SettingsItem(
                    title = "Refresh All Feeds",
                    subtitle = "Update all feeds manually",
                    icon = Icons.Outlined.Refresh,
                    onClick = { /* Refresh all feeds */ }
                )
            }

            item(key = "divider3") {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // About section
            item(key = "about_header") {
                SettingsSectionHeader("About")
            }

            item(key = "about_app") {
                SettingsItem(
                    title = "Novascope",
                    subtitle = "Version 1.0.0",
                    icon = Icons.Outlined.Info,
                    onClick = { /* Show about dialog */ }
                )
            }

            item(key = "footer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Only render dialogs when they're actually needed
        if (showThemeDialog) {
            ThemeDialog(
                currentTheme = settingsState.value.themeMode,
                onThemeSelected = { theme ->
                    settingsState.value = settingsState.value.copy(themeMode = theme)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("Clear Cache") },
                text = { Text("This will clear all cached images and data. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Clear cache functionality
                            showClearCacheDialog = false
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// More efficient dialog implementation
@Composable
fun ThemeDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                // Cache the theme values to avoid recreation during composition
                val themes = remember { ThemeMode.values() }

                themes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentTheme,
                            onClick = { onThemeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Memoized section header component
@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

// Optimized settings item with better click handling
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    // Simpler Card implementation with fewer nested layouts
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// Simpler toggle item implementation
@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        }
    }
}

// Use a data class to group related settings
data class SettingsState(
    val themeMode: ThemeMode,
    val useDynamicColor: Boolean,
    val enableNotifications: Boolean,
    val enableAiSummary: Boolean,
    val textSize: TextSize
)

// Theme mode enum
enum class ThemeMode(val title: String, val description: String) {
    LIGHT("Light", "Always use light theme"),
    DARK("Dark", "Always use dark theme"),
    SYSTEM("System", "Follow system settings")
}

// Text size enum
enum class TextSize(val title: String, val scaleFactor: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f)
}