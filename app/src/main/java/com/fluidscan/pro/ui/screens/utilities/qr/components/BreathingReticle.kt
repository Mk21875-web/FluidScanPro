package com.fluidscan.pro.ui.screens.utilities.qr.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fluidscan.pro.ui.theme.FluidScanMotion
import kotlin.math.min

/**
 * A scanning reticle that "breathes" (pulses) while searching, then springs into a circular
 * checkmark on detection (damping 0.6 overshoot). The haptic is fired by the caller.
 */
@Composable
fun BreathingReticle(
    detected: Boolean,
    idleColor: Color,
    successColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "reticleBreath")
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val detect by animateFloatAsState(
        targetValue = if (detected) 1f else 0f,
        animationSpec = FluidScanMotion.Springs.snap(),
        label = "detectMorph"
    )

    Canvas(modifier) {
        val side = min(size.width, size.height) * 0.6f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val pulse = if (detected) 0f else breath * (side * 0.03f)
        val cornerLen = side * 0.22f
        val stroke = side * 0.018f

        val idle = idleColor.copy(alpha = (1f - detect).coerceIn(0f, 1f))
        if (idle.alpha > 0.01f) {
            drawCornerBrackets(left - pulse, top - pulse, side + pulse * 2, cornerLen, stroke, idle)
        }

        // Detection: a circle closes in and a checkmark strokes on.
        if (detect > 0.01f) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = side / 2f
            drawCircle(
                color = successColor,
                radius = radius * (0.75f + 0.25f * detect),
                center = Offset(cx, cy),
                style = Stroke(width = stroke * 1.4f)
            )
            val check = Path().apply {
                moveTo(cx - radius * 0.32f, cy + radius * 0.02f)
                lineTo(cx - radius * 0.08f, cy + radius * 0.26f)
                lineTo(cx + radius * 0.36f, cy - radius * 0.26f)
            }
            drawCheck(check, detect, successColor, stroke * 1.6f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerBrackets(
    left: Float,
    top: Float,
    side: Float,
    len: Float,
    stroke: Float,
    color: Color
) {
    val right = left + side
    val bottom = top + side
    // top-left
    drawLine(color, Offset(left, top), Offset(left + len, top), stroke, StrokeCap.Round)
    drawLine(color, Offset(left, top), Offset(left, top + len), stroke, StrokeCap.Round)
    // top-right
    drawLine(color, Offset(right, top), Offset(right - len, top), stroke, StrokeCap.Round)
    drawLine(color, Offset(right, top), Offset(right, top + len), stroke, StrokeCap.Round)
    // bottom-left
    drawLine(color, Offset(left, bottom), Offset(left + len, bottom), stroke, StrokeCap.Round)
    drawLine(color, Offset(left, bottom), Offset(left, bottom - len), stroke, StrokeCap.Round)
    // bottom-right
    drawLine(color, Offset(right, bottom), Offset(right - len, bottom), stroke, StrokeCap.Round)
    drawLine(color, Offset(right, bottom), Offset(right, bottom - len), stroke, StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCheck(
    path: Path,
    progress: Float,
    color: Color,
    stroke: Float
) {
    // Reveal the checkmark by scaling it up from the center as it snaps in.
    drawPath(
        path = path,
        color = color.copy(alpha = progress.coerceIn(0f, 1f)),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}
