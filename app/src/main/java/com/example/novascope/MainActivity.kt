package com.yourdomain.novascope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourdomain.novascope.navigation.NovascopeBottomNavBar
import com.yourdomain.novascope.navigation.Screen
import com.yourdomain.novascope.ui.screens.ExploreScreen
import com.yourdomain.novascope.ui.screens.HomeScreen
import com.yourdomain.novascope.ui.screens.SavedScreen
import com.yourdomain.novascope.ui.screens.SettingsScreen
import com.yourdomain.novascope.ui.theme.NovascopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovascopeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NovascopeApp()
                }
            }
        }
    }
}

@Composable
fun NovascopeApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NovascopeBottomNavBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Explore.route) {
                ExploreScreen()
            }
            composable(Screen.Saved.route) {
                SavedScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}