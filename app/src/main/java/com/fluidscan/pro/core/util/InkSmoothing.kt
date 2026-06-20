package com.fluidscan.pro.core.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path

/**
 * Pen ink smoothing. Raw touch samples are noisy and polyline-jagged; we convert them into
 * a continuous cubic-Bézier path using **Catmull-Rom → Bézier** conversion, which passes
 * through every input point while producing smooth, natural curves (the feel of a real pen).
 *
 * Inputs are normalized (0..1) page coordinates; [canvas] scales them to pixels so the same
 * stroke renders crisply at preview size and at full PDF DPI.
 */
object InkSmoothing {

    /** Catmull-Rom tension → Bézier. 1/6 is the standard uniform Catmull-Rom factor. */
    private const val ALPHA = 1f / 6f

    fun buildPath(points: List<Offset>, canvas: Size): Path {
        val path = Path()
        if (points.isEmpty()) return path

        val pts = points.map { Offset(it.x * canvas.width, it.y * canvas.height) }
        if (pts.size == 1) {
            // A dot — draw a tiny segment so the stroke is visible.
            path.moveTo(pts[0].x, pts[0].y)
            path.lineTo(pts[0].x + 0.01f, pts[0].y + 0.01f)
            return path
        }
        if (pts.size == 2) {
            path.moveTo(pts[0].x, pts[0].y)
            path.lineTo(pts[1].x, pts[1].y)
            return path
        }

        path.moveTo(pts[0].x, pts[0].y)
        for (i in 0 until pts.size - 1) {
            val p0 = pts[if (i == 0) 0 else i - 1]
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts[if (i + 2 < pts.size) i + 2 else pts.size - 1]

            val c1 = Offset(p1.x + (p2.x - p0.x) * ALPHA, p1.y + (p2.y - p0.y) * ALPHA)
            val c2 = Offset(p2.x - (p3.x - p1.x) * ALPHA, p2.y - (p3.y - p1.y) * ALPHA)

            path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }
        return path
    }

    /**
     * Optional pre-pass: exponential moving average to tame finger jitter before smoothing.
     * Kept separate so the raw points remain the source of truth in the model.
     */
    fun denoise(points: List<Offset>, factor: Float = 0.4f): List<Offset> {
        if (points.size < 3) return points
        val out = ArrayList<Offset>(points.size)
        var prev = points.first()
        out += prev
        for (i in 1 until points.size) {
            val cur = points[i]
            prev = Offset(
                prev.x + (cur.x - prev.x) * factor,
                prev.y + (cur.y - prev.y) * factor
            )
            out += prev
        }
        return out
    }
}
