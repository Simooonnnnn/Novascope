// app/src/main/java/com/example/novascope/navigation/NovascopeNavigation.kt
package com.example.novascope.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
        }
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

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        NavigationBar(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .height(65.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 4.dp,
            windowInsets = WindowInsets(0)
        ) {
            bottomNavItems.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                NavigationBarItem(
                    icon = {
                        AnimatedVisibility(
                            visible = true,
                            enter = MaterialMotion.fadeInTransition,
                            exit = MaterialMotion.fadeOutTransition
                        ) {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                            )
                        }
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}