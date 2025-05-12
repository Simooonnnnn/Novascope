package com.example.novascope.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.novascope.ui.theme.NovascopeTheme

@Composable
fun ExploreScreen() {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Explore Screen",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun SavedScreen() {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Saved Articles",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
fun SettingsScreen() {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Preview
@Composable
fun OtherScreensPreview() {
    NovascopeTheme {
        ExploreScreen()
    }
}