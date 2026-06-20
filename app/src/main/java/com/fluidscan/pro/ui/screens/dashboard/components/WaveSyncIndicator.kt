package com.fluidscan.pro.ui.screens.dashboard.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * Fluid, wave-like progress indicator for cloud sync. Two phase-shifted sine waves drift
 * across the bounds, evoking water filling up — far more "alive" than a flat progress bar.
 */
@Composable
fun WaveSyncIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    waves: Int = 2
) {
    val transition = rememberInfiniteTransition(label = "waveSync")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    // Gentle bob of the water level.
    val level by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "level"
    )

    Canvas(modifier = modifier) {
        drawWave(color.copy(alpha = 0.35f), phase, level, amplitudeFraction = 0.10f)
        if (waves > 1) drawWave(color.copy(alpha = 0.6f), phase + PI.toFloat(), level, amplitudeFraction = 0.07f)
    }
}

private fun DrawScope.drawWave(color: Color, phase: Float, level: Float, amplitudeFraction: Float) {
    val w = size.width
    val h = size.height
    val baseY = h * (1f - level)
    val amp = h * amplitudeFraction
    val path = Path().apply {
        moveTo(0f, baseY)
        var x = 0f
        val step = (w / 24f).coerceAtLeast(1f)
        while (x <= w) {
            val y = baseY + amp * sin((x / w) * 2f * PI.toFloat() * 1.5f + phase)
            lineTo(x, y)
            x += step
        }
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(path, color)
}
