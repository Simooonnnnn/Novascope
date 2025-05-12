package com.example.novascope.model

import java.util.UUID

data class Feed(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val category: FeedCategory,
    val iconUrl: String? = null,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)