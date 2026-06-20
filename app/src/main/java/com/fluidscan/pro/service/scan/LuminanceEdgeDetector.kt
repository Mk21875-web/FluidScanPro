package com.fluidscan.pro.service.scan

import androidx.compose.ui.geometry.Offset
import com.fluidscan.pro.domain.model.Quadrilateral
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

/**
 * Lightweight on-device document detector.
 *
 * It runs a Sobel-style gradient over the downscaled luminance frame, thresholds the
 * result into an edge mask, then estimates the document boundary by percentile-trimmed
 * projection (robust to a few noisy edge pixels). Returns a normalized [Quadrilateral].
 *
 * This is intentionally dependency-free so the engine works offline with zero native
 * libs. It is bound behind [EdgeDetector], so an OpenCV/ML Kit contour detector can
 * replace it later without touching the camera pipeline or UI.
 */
class LuminanceEdgeDetector @Inject constructor() : EdgeDetector {

    override fun detect(frame: LumaFrame): EdgeResult {
        val w = frame.width
        val h = frame.height
        if (w < 8 || h < 8) return EdgeResult.NONE

        // 1) Gradient magnitude (Sobel) into an edge mask.
        val mag = IntArray(w * h)
        var maxMag = 1
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = (frame.at(x + 1, y - 1) + 2 * frame.at(x + 1, y) + frame.at(x + 1, y + 1)) -
                    (frame.at(x - 1, y - 1) + 2 * frame.at(x - 1, y) + frame.at(x - 1, y + 1))
                val gy = (frame.at(x - 1, y + 1) + 2 * frame.at(x, y + 1) + frame.at(x + 1, y + 1)) -
                    (frame.at(x - 1, y - 1) + 2 * frame.at(x, y - 1) + frame.at(x + 1, y - 1))
                val m = abs(gx) + abs(gy)
                mag[y * w + x] = m
                if (m > maxMag) maxMag = m
            }
        }

        // 2) Adaptive threshold relative to the strongest gradient in the frame.
        val threshold = (maxMag * EDGE_THRESHOLD_RATIO).toInt().coerceAtLeast(MIN_ABS_THRESHOLD)

        // 3) Per-axis edge histograms (count of strong-edge pixels per column / row).
        val colHits = IntArray(w)
        val rowHits = IntArray(h)
        var totalHits = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                if (mag[y * w + x] >= threshold) {
                    colHits[x]++
                    rowHits[y]++
                    totalHits++
                }
            }
        }
        if (totalHits < (w * h) * MIN_EDGE_DENSITY) return EdgeResult.NONE

        // 4) Percentile-trimmed bounds: ignore the sparse tails so stray edges don't
        //    blow up the box. Find first/last axis bins exceeding a fraction of the peak.
        val colThresh = (colHits.max() * PROJECTION_FRACTION).toInt().coerceAtLeast(1)
        val rowThresh = (rowHits.max() * PROJECTION_FRACTION).toInt().coerceAtLeast(1)

        val left = colHits.indexOfFirst { it >= colThresh }
        val right = colHits.indexOfLast { it >= colThresh }
        val top = rowHits.indexOfFirst { it >= rowThresh }
        val bottom = rowHits.indexOfLast { it >= rowThresh }

        if (left < 0 || right <= left || top < 0 || bottom <= top) return EdgeResult.NONE

        val boxW = (right - left).toFloat()
        val boxH = (bottom - top).toFloat()
        // Reject tiny/degenerate detections.
        if (boxW < w * MIN_BOX_RATIO || boxH < h * MIN_BOX_RATIO) return EdgeResult.NONE

        // 5) Normalize to 0..1 and build a (axis-aligned) quad.
        val nl = left / w.toFloat()
        val nr = right / w.toFloat()
        val nt = top / h.toFloat()
        val nb = bottom / h.toFloat()
        val quad = Quadrilateral(
            topLeft = Offset(nl, nt),
            topRight = Offset(nr, nt),
            bottomRight = Offset(nr, nb),
            bottomLeft = Offset(nl, nb)
        )

        // 6) Confidence blends coverage (area filled) and edge strength density.
        val coverage = (boxW * boxH) / (w * h)
        val density = min(1f, totalHits.toFloat() / (2f * (boxW + boxH)))
        val confidence = (0.5f * coverage + 0.5f * density).coerceIn(0f, 1f)

        return EdgeResult(quad, confidence)
    }

    private companion object {
        const val EDGE_THRESHOLD_RATIO = 0.28f
        const val MIN_ABS_THRESHOLD = 60
        const val MIN_EDGE_DENSITY = 0.01f
        const val PROJECTION_FRACTION = 0.18f
        const val MIN_BOX_RATIO = 0.25f
    }
}
