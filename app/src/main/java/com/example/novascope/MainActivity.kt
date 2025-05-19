// app/src/main/java/com/example/novascope/MainActivity.kt
package com.example.novascope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.novascope.ui.screens.ArticleDetailScreen
import com.example.novascope.ui.screens.ExploreScreen
import com.example.novascope.ui.screens.HomeScreen  // Make sure this import is correct
import com.example.novascope.ui.screens.SavedScreen
import com.example.novascope.ui.screens.SettingsScreen
import com.example.novascope.ui.theme.NovascopeTheme
import com.example.novascope.viewmodel.NovascopeViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {

    // Create a ViewModel instance at the activity level
    private lateinit var viewModel: NovascopeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge display
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Set up window to draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize the ViewModel
        viewModel = NovascopeViewModel(this)

        // Register the activity with the ViewModel for file picking
        viewModel.registerActivity(this)

        setContent {
            NovascopeApp(viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister the activity when it's destroyed
        viewModel.unregisterActivity()
    }
}

// Define our navigation destinations
sealed class Screen(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val title: String
) {
    object Home : Screen(
        "home",
        Icons.Filled.Home,
        Icons.Outlined.Home,
        "Home"
    )
    object Explore : Screen(
        "explore",
        Icons.Filled.Explore,
        Icons.Outlined.Explore,
        "Explore"
    )
    object Saved : Screen(
        "saved",
        Icons.Filled.Bookmark,
        Icons.Outlined.BookmarkBorder,
        "Saved"
    )
    object Settings : Screen(
        "settings",
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        "Settings"
    )

    // Detail screen with argument
    object ArticleDetail : Screen(
        "article/{articleId}",
        Icons.Filled.Home, // Not shown in bottom nav
        Icons.Outlined.Home, // Not shown in bottom nav
        "Article"
    ) {
        fun createRoute(articleId: String) = "article/$articleId"
    }
}

// List of bottom navigation items
private val bottomNavItems = listOf(
    Screen.Home,
    Screen.Explore,
    Screen.Saved,
    Screen.Settings
)

@Composable
fun NovascopeApp(providedViewModel: NovascopeViewModel? = null) {
    NovascopeTheme {
        // Set up transparent status bar
        val systemUiController = rememberSystemUiController()
        val useDarkIcons = !isSystemInDarkTheme()

        // Make status bar transparent with appropriate icon colors
        DisposableEffect(systemUiController, useDarkIcons) {
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = useDarkIcons
            )
            onDispose {}
        }

        // Set up navigation
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Show bottom nav only on main screens
        val showBottomNav = remember(currentRoute) {
            bottomNavItems.any {
                currentRoute == it.route ||
                        // Handle pattern matching for routes with arguments
                        (it.route.contains("{") && currentRoute?.startsWith(it.route.substringBefore("{")) == true)
            }
        }

        // Get local context for ViewModel
        val context = LocalContext.current

        // Create or use provided ViewModel
        val viewModel = providedViewModel ?: viewModel { NovascopeViewModel(context) }

        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    NovascopeBottomNav(navController)
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.surface
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onNewsItemClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            },
                            onAddFeedClick = {
                                navController.navigate(Screen.Explore.route)
                            }
                        )
                    }
                    composable(Screen.Explore.route) {
                        ExploreScreen(
                            viewModel = viewModel,
                            onNewsItemClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }
                    composable(Screen.Saved.route) {
                        SavedScreen(
                            viewModel = viewModel,
                            onNewsItemClick = { articleId ->
                                navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                            }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBackClick = { /* No op - bottom nav handles it */ }
                        )
                    }

                    // Article detail screen with arguments
                    composable(
                        route = Screen.ArticleDetail.route,
                        arguments = listOf(
                            navArgument("articleId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
                        if (articleId.isNotEmpty()) {
                            ArticleDetailScreen(
                                articleId = articleId,
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        } else {
                            // Handle invalid article ID
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NovascopeBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        // Using default container color from Material3
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    // Simple icon without animation for now
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = selected,
                // Let Material 3 handle colors by default
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
                }
            )
        }
    }
}