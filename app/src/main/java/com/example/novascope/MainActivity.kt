// app/src/main/java/com/example/novascope/MainActivity.kt
package com.example.novascope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.example.novascope.ui.screens.HomeScreen
import com.example.novascope.ui.screens.SavedScreen
import com.example.novascope.ui.screens.SettingsScreen
import com.example.novascope.ui.theme.NovascopeTheme
import com.example.novascope.viewmodel.NovascopeViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {

    // Create a ViewModel instance at the activity level
    private lateinit var viewModel: NovascopeViewModel

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
            val windowSizeClass = calculateWindowSizeClass(this)
            NovascopeApp(viewModel, windowSizeClass)
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
        Icons.Filled.Home, // Not shown in navigation
        Icons.Outlined.Home, // Not shown in navigation
        "Article"
    ) {
        fun createRoute(articleId: String) = "article/$articleId"
    }
}

// List of navigation items
private val navigationItems = listOf(
    Screen.Home,
    Screen.Explore,
    Screen.Saved,
    Screen.Settings
)

@Composable
fun NovascopeApp(
    providedViewModel: NovascopeViewModel? = null,
    windowSizeClass: WindowSizeClass
) {
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

        // Determine if we should use tablet layout
        val useTabletLayout = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

        // Show navigation only on main screens
        val showNavigation = remember(currentRoute) {
            navigationItems.any {
                currentRoute == it.route ||
                        (it.route.contains("{") && currentRoute?.startsWith(it.route.substringBefore("{")) == true)
            }
        }

        // Get local context for ViewModel
        val context = LocalContext.current

        // Create or use provided ViewModel
        val viewModel = providedViewModel ?: viewModel { NovascopeViewModel(context) }

        if (useTabletLayout) {
            // Tablet layout with navigation rail
            Row(modifier = Modifier.fillMaxSize()) {
                if (showNavigation) {
                    NovascopeNavigationRail(
                        navController = navController,
                        currentRoute = currentRoute
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    NovascopeNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        useTabletLayout = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Phone layout with bottom navigation
            Scaffold(
                bottomBar = {
                    if (showNavigation) {
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
                    NovascopeNavHost(
                        navController = navController,
                        viewModel = viewModel,
                        useTabletLayout = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun NovascopeNavigationRail(
    navController: NavHostController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = NavigationRailDefaults.windowInsets,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            navigationItems.forEach { screen ->
                val selected = currentRoute == screen.route

                NavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.title
                        )
                    },
                    label = { Text(screen.title) },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NovascopeBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        navigationItems.forEach { screen ->
            val selected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun NovascopeNavHost(
    navController: NavHostController,
    viewModel: NovascopeViewModel,
    useTabletLayout: Boolean,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNewsItemClick = { articleId ->
                    navController.navigate(Screen.ArticleDetail.createRoute(articleId))
                },
                onAddFeedClick = {
                    navController.navigate(Screen.Explore.route)
                },
                useTabletLayout = useTabletLayout
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
                onBackClick = { /* No op - navigation handles it */ }
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
                    onBackClick = { navController.popBackStack() },
                    useTabletLayout = useTabletLayout
                )
            } else {
                // Handle invalid article ID
                navController.popBackStack()
            }
        }
    }
}

