package com.fluidscan.pro.service.scan

import com.fluidscan.pro.domain.model.Quadrilateral

/**
 * A grayscale document frame: a row-major luminance grid (0..255) of [width]x[height].
 * The camera frame's Y plane is downscaled into this cheap structure so detection runs
 * in a few milliseconds per frame, well inside the 90fps budget.
 */
data class LumaFrame(
    val luma: IntArray,
    val width: Int,
    val height: Int
) {
    fun at(x: Int, y: Int): Int = luma[y * width + x]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LumaFrame) return false
        return width == other.width && height == other.height && luma.contentEquals(other.luma)
    }

    override fun hashCode(): Int = 31 * (31 * width + height) + luma.contentHashCode()
}

/** Result of one detection pass. [confidence] in 0f..1f gates the spring-snap. */
data class EdgeResult(
    val quad: Quadrilateral?,
    val confidence: Float
) {
    val hasDocument: Boolean get() = quad != null && confidence >= MIN_CONFIDENCE

    companion object {
        const val MIN_CONFIDENCE = 0.35f
        val NONE = EdgeResult(null, 0f)
    }
}

/**
 * Strategy interface for real-time document boundary detection. Implementations must be
 * pure/stateless w.r.t. the input frame and fast enough to run per-frame on a worker
 * thread. Default impl is [LuminanceEdgeDetector]; swap for OpenCV/ML Kit via Hilt.
 */
interface EdgeDetector {
    fun detect(frame: LumaFrame): EdgeResult
}
