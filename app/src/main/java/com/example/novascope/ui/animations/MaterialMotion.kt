package com.example.novascope.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Material Motion utilities for standardized animations across the app.
 * Based on Material Design 3 motion guidelines.
 */
object MaterialMotion {
    // Standard durations
    const val DURATION_SHORTEST = 150
    const val DURATION_SHORT = 250
    const val DURATION_MEDIUM = 350
    const val DURATION_LONG = 450

    // Material Design recommended easing curves
    val StandardEasing = FastOutSlowInEasing

    // Spring specs for dynamic animations
    val SpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val MediumSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 350f
    )

    val TweenSpec = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = StandardEasing
    )

    val ShortTweenSpec = tween<Float>(
        durationMillis = DURATION_SHORT,
        easing = StandardEasing
    )
}