// app/src/main/java/com/example/novascope/navigation/NovascopeNavigation.kt
package com.example.novascope.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.novascope.ui.animations.MaterialMotion
import com.example.novascope.ui.screens.ExploreScreen
import com.example.novascope.ui.screens.HomeScreen
import com.example.novascope.ui.screens.SavedScreen
import com.example.novascope.ui.screens.SettingsScreen

sealed class Screen(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val title: String
) {
    object Home : Screen(
        "home",
        Icons.Filled.Home,
        Icons.Rounded.Home,
        "Home"
    )
    object Explore : Screen(
        "explore",
        Icons.Filled.Explore,
        Icons.Rounded.Explore,
        "Explore"
    )
    object Saved : Screen(
        "saved",
        Icons.Filled.Bookmark,
        Icons.Rounded.BookmarkBorder,
        "Saved"
    )
    object Settings : Screen(
        "settings",
        Icons.Filled.Settings,
        Icons.Rounded.Settings,
        "Settings"
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Explore,
    Screen.Saved,
    Screen.Settings
)

@Composable
fun NovascopeNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NovascopeBottomNavBar(navController = navController)
        },
        // Remove insets to match Figma's edge-to-edge design
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { MaterialMotion.fadeInTransition },
            exitTransition = { MaterialMotion.fadeOutTransition },
            popEnterTransition = { MaterialMotion.fadeInTransition },
            popExitTransition = { MaterialMotion.fadeOutTransition }
        ) {
            composable(
                route = Screen.Home.route,
                enterTransition = { MaterialMotion.scaleInTransition },
                exitTransition = { MaterialMotion.scaleOutTransition },
                popEnterTransition = { MaterialMotion.scaleInTransition },
                popExitTransition = { MaterialMotion.scaleOutTransition }
            ) {
                HomeScreen()
            }
            composable(
                route = Screen.Explore.route,
                enterTransition = { MaterialMotion.slideUpTransition },
                exitTransition = { MaterialMotion.slideDownTransition },
                popEnterTransition = { MaterialMotion.slideUpTransition },
                popExitTransition = { MaterialMotion.slideDownTransition }
            ) {
                ExploreScreen()
            }
            composable(
                route = Screen.Saved.route,
                enterTransition = { MaterialMotion.slideUpTransition },
                exitTransition = { MaterialMotion.slideDownTransition },
                popEnterTransition = { MaterialMotion.slideUpTransition },
                popExitTransition = { MaterialMotion.slideDownTransition }
            ) {
                SavedScreen()
            }
            composable(
                route = Screen.Settings.route,
                enterTransition = { MaterialMotion.slideUpTransition },
                exitTransition = { MaterialMotion.slideDownTransition },
                popEnterTransition = { MaterialMotion.slideUpTransition },
                popExitTransition = { MaterialMotion.slideDownTransition }
            ) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun NovascopeBottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Bottom navigation bar - exactly matching Figma specifications
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(83.dp) // Figma's 83px height
            .background(Color(0xFFFEF7FF)) // Figma's background color
            .padding(horizontal = 20.dp, vertical = 8.dp), // Figma's padding
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main navigation bar
        NavigationBar(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp)) // Figma's 20px corner radius
                .fillMaxWidth()
                .height(65.dp), // Figma's nav height
            containerColor = Color(0xFFFEF7FF), // Match Figma's background color
            contentColor = Color(0xFF1D1B20), // Match Figma's text color
            tonalElevation = 0.dp, // No elevation in Figma
            windowInsets = WindowInsets(0) // No insets in Figma
        ) {
            bottomNavItems.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any {
                    it.route == screen.route
                } == true

                // Normal navigation items
                NavigationBarItem(
                    icon = {
                        AnimatedVisibility(
                            visible = true,
                            enter = MaterialMotion.fadeInTransition,
                            exit = MaterialMotion.fadeOutTransition
                        ) {
                            // For selected item, add a background highlight matching Figma
                            if (selected) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFE8DEF8)) // Figma's highlight color
                                            .width(58.33.dp) // Figma width
                                            .height(28.54.dp), // Figma height
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title,
                                            tint = Color(0xFF1D1B20), // Figma's icon color
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    tint = if (selected) Color(0xFF1D1B20) else Color(0xFF49454F), // Figma's colors
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 14.3.sp, // 119.19% in Figma
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (selected) Color(0xFF1D1B20) else Color(0xFF49454F) // Figma's colors
                        )
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D1B20), // Figma's selected color
                        selectedTextColor = Color(0xFF1D1B20), // Figma's selected color
                        indicatorColor = Color.Transparent, // No indicator in Figma
                        unselectedIconColor = Color(0xFF49454F), // Figma's unselected color
                        unselectedTextColor = Color(0xFF49454F) // Figma's unselected color
                    )
                )
            }
        }

        // Bottom system bar indicator - matches Figma
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.85.dp)
                .width(66.93.dp)
                .height(2.85.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(Color.Black.copy(alpha = 0.7f))
        )
    }
}