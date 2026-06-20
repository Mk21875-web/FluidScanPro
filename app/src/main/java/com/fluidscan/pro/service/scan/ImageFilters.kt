package com.fluidscan.pro.service.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.fluidscan.pro.domain.model.ScanFilter
import javax.inject.Inject

/**
 * Document filters implemented as GPU-friendly [ColorMatrix] passes (plus an adaptive
 * threshold for crisp B&W). All operations allocate a fresh output bitmap and never
 * mutate the input; callers recycle intermediates.
 *
 * For the *live preview*, the UI applies the matrices directly via a Compose
 * `ColorFilter`, so the cross-fade carousel never decodes a new bitmap per filter.
 */
class ImageFilters @Inject constructor() {

    fun apply(source: Bitmap, filter: ScanFilter): Bitmap = when (filter) {
        ScanFilter.ORIGINAL -> source.copy(Bitmap.Config.ARGB_8888, false)
        ScanFilter.GRAYSCALE -> source.withMatrix(grayscaleMatrix())
        ScanFilter.MAGIC_COLOR -> source.withMatrix(magicColorMatrix())
        ScanFilter.LIGHTEN -> source.withMatrix(lightenMatrix())
        ScanFilter.BLACK_WHITE -> source.toBlackAndWhite()
    }

    private fun Bitmap.withMatrix(matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(this, 0f, 0f, paint)
        return out
    }

    /** Adaptive-ish threshold: grayscale then per-pixel binarize against a local-mean proxy. */
    private fun Bitmap.toBlackAndWhite(): Bitmap {
        val gray = withMatrix(grayscaleMatrix())
        val w = gray.width
        val h = gray.height
        val pixels = IntArray(w * h)
        gray.getPixels(pixels, 0, w, 0, 0, w, h)
        gray.recycle()

        // Global mean as the threshold baseline + bias toward white background.
        var sum = 0L
        for (p in pixels) sum += (p and 0xFF)
        val mean = (sum / pixels.size).toInt()
        val threshold = (mean * 0.92f).toInt()

        for (i in pixels.indices) {
            val lum = pixels[i] and 0xFF
            pixels[i] = if (lum >= threshold) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    companion object {
        fun grayscaleMatrix() = ColorMatrix().apply { setSaturation(0f) }

        /** Boost saturation + contrast for the "Magic Color" pop. */
        fun magicColorMatrix(): ColorMatrix {
            val saturation = ColorMatrix().apply { setSaturation(1.35f) }
            val contrast = 1.25f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            return ColorMatrix().apply {
                postConcat(saturation)
                postConcat(contrastMatrix)
            }
        }

        /** Brightens shadows for faint pencil/receipt scans. */
        fun lightenMatrix(): ColorMatrix {
            val scale = 1.18f
            val bias = 18f
            return ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, bias,
                    0f, scale, 0f, 0f, bias,
                    0f, 0f, scale, 0f, bias,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
    }
}
