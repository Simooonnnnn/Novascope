// app/src/main/java/com/example/novascope/MainActivity.kt
package com.example.novascope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.example.novascope.navigation.NovascopeNavigation
import com.example.novascope.ui.theme.NovascopeTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge display to match Figma design
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Set up window to draw behind system bars (match Figma's full-screen design)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NovascopeApp()
        }
    }
}

@Composable
fun NovascopeApp() {
    NovascopeTheme {
        // Set up transparent status bar to match Figma design
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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NovascopeNavigation()
        }
    }
}