package com.fluidscan.pro.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.hypot

/**
 * A document boundary expressed as four corner points in **normalized** coordinates
 * (each component in 0f..1f relative to the analyzed frame). Keeping it normalized lets
 * us map the same quad onto the camera preview, the captured full-res bitmap, and the
 * crop editor without re-running detection.
 *
 * Corner order is always clockwise starting top-left:
 * [topLeft] → [topRight] → [bottomRight] → [bottomLeft].
 */
@Immutable
data class Quadrilateral(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset
) {
    val corners: List<Offset>
        get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    /** Replace a single corner by index (0=TL,1=TR,2=BR,3=BL). Used by manual crop drag. */
    fun withCorner(index: Int, value: Offset): Quadrilateral = when (index) {
        0 -> copy(topLeft = value)
        1 -> copy(topRight = value)
        2 -> copy(bottomRight = value)
        3 -> copy(bottomLeft = value)
        else -> this
    }

    /** Scales normalized corners into pixel coordinates for a given canvas/bitmap size. */
    fun toPixels(size: Size): Quadrilateral = Quadrilateral(
        topLeft = Offset(topLeft.x * size.width, topLeft.y * size.height),
        topRight = Offset(topRight.x * size.width, topRight.y * size.height),
        bottomRight = Offset(bottomRight.x * size.width, bottomRight.y * size.height),
        bottomLeft = Offset(bottomLeft.x * size.width, bottomLeft.y * size.height)
    )

    /** Average edge length — a cheap proxy for "how big / confident" the detection is. */
    fun perimeter(): Float =
        dist(topLeft, topRight) + dist(topRight, bottomRight) +
            dist(bottomRight, bottomLeft) + dist(bottomLeft, topLeft)

    /** Linear interpolation between two quads — drives the spring-snap morph. */
    fun lerp(target: Quadrilateral, t: Float): Quadrilateral = Quadrilateral(
        topLeft = lerpOffset(topLeft, target.topLeft, t),
        topRight = lerpOffset(topRight, target.topRight, t),
        bottomRight = lerpOffset(bottomRight, target.bottomRight, t),
        bottomLeft = lerpOffset(bottomLeft, target.bottomLeft, t)
    )

    companion object {
        /** Full-frame quad (used as the resting state when no document is detected). */
        val FULL = Quadrilateral(
            topLeft = Offset(0.04f, 0.04f),
            topRight = Offset(0.96f, 0.04f),
            bottomRight = Offset(0.96f, 0.96f),
            bottomLeft = Offset(0.04f, 0.96f)
        )

        private fun dist(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)
        private fun lerpOffset(a: Offset, b: Offset, t: Float) =
            Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }
}
