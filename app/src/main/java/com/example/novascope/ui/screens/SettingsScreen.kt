// app/src/main/java/com/example/novascope/ui/screens/SettingsScreen.kt
package com.example.novascope.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.theme.NovascopeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {}
) {
    var useDynamicColor by remember { mutableStateOf(true) }
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var enableNotifications by remember { mutableStateOf(true) }
    var enableAiSummary by remember { mutableStateOf(true) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var textSize by remember { mutableStateOf(TextSize.MEDIUM) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Appearance section
            item {
                SettingsSectionHeader(title = "Appearance")

                SettingsItem(
                    title = "Theme",
                    subtitle = themeMode.title,
                    icon = when (themeMode) {
                        ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        ThemeMode.DARK -> Icons.Outlined.DarkMode
                        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                    },
                    onClick = { showThemeDialog = true }
                )

                SettingsToggleItem(
                    title = "Dynamic Color",
                    subtitle = "Use colors from your wallpaper (Android 12+)",
                    icon = Icons.Outlined.Palette,
                    checked = useDynamicColor,
                    onToggle = { useDynamicColor = it }
                )

                SettingsItem(
                    title = "Text Size",
                    subtitle = textSize.title,
                    icon = Icons.Outlined.FormatSize,
                    onClick = {
                        // Cycle through text sizes
                        textSize = when (textSize) {
                            TextSize.SMALL -> TextSize.MEDIUM
                            TextSize.MEDIUM -> TextSize.LARGE
                            TextSize.LARGE -> TextSize.SMALL
                        }
                    }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Features section
            item {
                SettingsSectionHeader(title = "Features")

                SettingsToggleItem(
                    title = "AI Summaries",
                    subtitle = "Generate summaries using on-device AI",
                    icon = Icons.Outlined.Psychology,
                    checked = enableAiSummary,
                    onToggle = { enableAiSummary = it }
                )

                SettingsToggleItem(
                    title = "Notifications",
                    subtitle = "Receive alerts for new articles",
                    icon = Icons.Outlined.Notifications,
                    checked = enableNotifications,
                    onToggle = { enableNotifications = it }
                )

                SettingsItem(
                    title = "Default Language",
                    subtitle = "English",
                    icon = Icons.Outlined.Language,
                    onClick = { /* Open language selector */ }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Data section
            item {
                SettingsSectionHeader(title = "Data & Storage")

                SettingsItem(
                    title = "Clear Cache",
                    subtitle = "Free up space used by images and data",
                    icon = Icons.Outlined.Delete,
                    onClick = { showClearCacheDialog = true }
                )

                SettingsItem(
                    title = "Refresh All Feeds",
                    subtitle = "Update all feeds manually",
                    icon = Icons.Outlined.Refresh,
                    onClick = { /* Refresh all feeds */ }
                )

                SettingsItem(
                    title = "Storage Usage",
                    subtitle = "42 MB used",
                    icon = Icons.Outlined.Storage,
                    onClick = { /* Show storage details */ }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // About section
            item {
                SettingsSectionHeader(title = "About")

                SettingsItem(
                    title = "Novascope",
                    subtitle = "Version 1.0.0",
                    icon = Icons.Outlined.Info,
                    onClick = { /* Show about dialog */ }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Theme Selection Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme") },
                text = {
                    Column {
                        ThemeMode.values().forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = mode == themeMode,
                                    onClick = {
                                        themeMode = mode
                                        showThemeDialog = false
                                    }
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
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Clear Cache Confirmation Dialog
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("Clear Cache") },
                text = {
                    Text("This will clear all cached images and data. This action cannot be undone.")
                },
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
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AnimatedVisibility(
                    visible = subtitle.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(4.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            AnimatedVisibility(
                visible = subtitle.isNotEmpty(),
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = MaterialMotion.DURATION_MEDIUM,
                        easing = MaterialMotion.EmphasizedDecelerateEasing
                    )
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = MaterialMotion.DURATION_MEDIUM,
                        easing = MaterialMotion.EmphasizedDecelerateEasing
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = MaterialMotion.DURATION_SHORT,
                        easing = MaterialMotion.EmphasizedAccelerateEasing
                    )
                ) + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = MaterialMotion.DURATION_SHORT,
                        easing = MaterialMotion.EmphasizedAccelerateEasing
                    )
                )
            ) {
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NovascopeTheme {
        SettingsScreen()
    }
}