// app/src/main/java/com/example/novascope/ui/animations/MaterialMotion.kt
package com.example.novascope.ui.animations

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

/**
 * Material Motion utilities for standardized animations across the app.
 * Based on Material Design 3 motion guidelines.
 */
object MaterialMotion {
    // Standard durations based on Material Guidelines
    const val DURATION_SHORTEST = 150
    const val DURATION_SHORT = 250
    const val DURATION_MEDIUM = 350
    const val DURATION_LONG = 450
    const val DURATION_EXTRA_LONG = 600

    // Material Design recommended easing curves
    val StandardEasing = FastOutSlowInEasing
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    // Spring specs for dynamic animations
    val SpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val MediumSpring = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 350f
    )

    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val TweenSpec = tween<Float>(
        durationMillis = DURATION_MEDIUM,
        easing = StandardEasing
    )

    val ShortTweenSpec = tween<Float>(
        durationMillis = DURATION_SHORT,
        easing = StandardEasing
    )

    val EmphasizedTweenSpec = tween<Float>(
        durationMillis = DURATION_LONG,
        easing = EmphasizedEasing
    )

    // Transition animations for navigation
    val fadeInTransition: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EmphasizedDecelerateEasing
        )
    )

    val fadeOutTransition: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = DURATION_SHORT,
            easing = EmphasizedAccelerateEasing
        )
    )

    val scaleInTransition: EnterTransition = scaleIn(
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EmphasizedDecelerateEasing
        ),
        initialScale = 0.95f
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EmphasizedDecelerateEasing
        )
    )

    val scaleOutTransition: ExitTransition = scaleOut(
        animationSpec = tween(
            durationMillis = DURATION_SHORT,
            easing = EmphasizedAccelerateEasing
        ),
        targetScale = 0.95f
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = DURATION_SHORT,
            easing = EmphasizedAccelerateEasing
        )
    )

    val slideUpTransition: EnterTransition = slideInVertically(
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EmphasizedDecelerateEasing
        ),
        initialOffsetY = { it / 5 }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EmphasizedDecelerateEasing
        )
    )

    val slideDownTransition: ExitTransition = slideOutVertically(
        animationSpec = tween(
            durationMillis = DURATION_SHORT,
            easing = EmphasizedAccelerateEasing
        ),
        targetOffsetY = { it / 5 }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = DURATION_SHORT,
            easing = EmphasizedAccelerateEasing
        )
    )
}