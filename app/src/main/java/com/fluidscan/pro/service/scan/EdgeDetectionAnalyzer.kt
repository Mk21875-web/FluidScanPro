package com.fluidscan.pro.service.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.fluidscan.pro.core.util.ImageProcessing

/**
 * CameraX [ImageAnalysis.Analyzer] that runs [EdgeDetector] on each frame and reports the
 * result via [onResult]. Configure the bound `ImageAnalysis` with
 * `STRATEGY_KEEP_ONLY_LATEST` so we always analyze the freshest frame and never queue —
 * this is what keeps the live border tracking snappy.
 *
 * The analyzer accounts for sensor rotation so the emitted quad is already in upright,
 * preview-space normalized coordinates.
 */
class EdgeDetectionAnalyzer(
    private val detector: EdgeDetector,
    private val onResult: (EdgeResult) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val rotation = image.imageInfo.rotationDegrees
            val frame = ImageProcessing.toLumaFrame(image)
            val raw = detector.detect(frame)
            onResult(raw.rotateToUpright(rotation))
        } catch (t: Throwable) {
            // Never let a single bad frame kill the analysis stream.
            onResult(EdgeResult.NONE)
        } finally {
            image.close()
        }
    }
}

/** Rotates a normalized [EdgeResult] from sensor space into upright display space. */
private fun EdgeResult.rotateToUpright(rotationDegrees: Int): EdgeResult {
    val q = quad ?: return this
    val rotated = when (((rotationDegrees % 360) + 360) % 360) {
        90 -> q.mapCorners { o -> androidx.compose.ui.geometry.Offset(o.y, 1f - o.x) }
        180 -> q.mapCorners { o -> androidx.compose.ui.geometry.Offset(1f - o.x, 1f - o.y) }
        270 -> q.mapCorners { o -> androidx.compose.ui.geometry.Offset(1f - o.y, o.x) }
        else -> q
    }
    return copy(quad = rotated.normalizedClockwise())
}

private inline fun com.fluidscan.pro.domain.model.Quadrilateral.mapCorners(
    transform: (androidx.compose.ui.geometry.Offset) -> androidx.compose.ui.geometry.Offset
) = com.fluidscan.pro.domain.model.Quadrilateral(
    topLeft = transform(topLeft),
    topRight = transform(topRight),
    bottomRight = transform(bottomRight),
    bottomLeft = transform(bottomLeft)
)

/** Reorders the (possibly rotated) corners back into TL,TR,BR,BL by position. */
private fun com.fluidscan.pro.domain.model.Quadrilateral.normalizedClockwise():
    com.fluidscan.pro.domain.model.Quadrilateral {
    val pts = corners
    val cx = pts.sumOf { it.x.toDouble() }.toFloat() / pts.size
    val cy = pts.sumOf { it.y.toDouble() }.toFloat() / pts.size
    val tl = pts.filter { it.x <= cx && it.y <= cy }.minByOrNull { it.x + it.y }
    val tr = pts.filter { it.x >= cx && it.y <= cy }.maxByOrNull { it.x - it.y }
    val br = pts.filter { it.x >= cx && it.y >= cy }.maxByOrNull { it.x + it.y }
    val bl = pts.filter { it.x <= cx && it.y >= cy }.minByOrNull { it.x - it.y }
    return if (tl != null && tr != null && br != null && bl != null) {
        com.fluidscan.pro.domain.model.Quadrilateral(tl, tr, br, bl)
    } else this
}
