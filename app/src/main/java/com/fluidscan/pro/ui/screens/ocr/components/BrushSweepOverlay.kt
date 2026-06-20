package com.fluidscan.pro.ui.screens.ocr.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A diagonal "brush stroke" of light that sweeps across the page while AI Cleanup runs,
 * evoking a brush wiping the document clean.
 */
@Composable
fun BrushSweepOverlay(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "brushSweep")
    val progress by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bandWidth = w * 0.34f
        val center = progress * (w + bandWidth) - bandWidth / 2f
        val brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, color.copy(alpha = 0.55f), Color.Transparent),
            start = Offset(center - bandWidth / 2f, 0f),
            end = Offset(center + bandWidth / 2f, h)
        )
        drawRect(brush = brush)
    }
}
