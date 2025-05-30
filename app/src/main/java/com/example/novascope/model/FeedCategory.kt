package com.example.novascope.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article  // Updated import
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

enum class FeedCategory(val title: String, val icon: ImageVector) {
    News("News", Icons.AutoMirrored.Filled.Article),  // Updated reference
    Tech("Technology", Icons.Filled.SmartToy),
    Science("Science", Icons.Filled.Science),
    Sports("Sports", Icons.Filled.SportsBasketball),
    Finance("Finance", Icons.Filled.AttachMoney)
}