package com.fluidscan.pro.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.fluidscan.pro.service.scan.LumaFrame
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Frame & bitmap helpers used by the scan pipeline.
 *
 * IMPORTANT (blueprint: zero memory leaks): callers own every [Bitmap] returned here and
 * must `recycle()` intermediates they no longer need. Nothing here caches bitmaps.
 */
object ImageProcessing {

    /** Target width of the downscaled luminance grid fed to the edge detector. */
    private const val LUMA_TARGET_WIDTH = 200

    /**
     * Extracts a small luminance grid from a YUV [ImageProxy]'s Y plane. We only read the
     * Y (luma) plane and subsample it — no allocation-heavy YUV→RGB conversion — keeping
     * per-frame cost tiny. The ImageProxy is NOT closed here; the analyzer owns it.
     */
    fun toLumaFrame(image: ImageProxy): LumaFrame {
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride

        val srcW = image.width
        val srcH = image.height
        val step = max(1, srcW / LUMA_TARGET_WIDTH)

        val outW = srcW / step
        val outH = srcH / step
        val luma = IntArray(outW * outH)

        var oy = 0
        var sy = 0
        while (oy < outH) {
            val rowBase = sy * rowStride
            var ox = 0
            var sx = 0
            while (ox < outW) {
                val idx = rowBase + sx * pixelStride
                luma[oy * outW + ox] = buffer.get(idx).toInt() and 0xFF
                ox++
                sx += step
            }
            oy++
            sy += step
        }
        return LumaFrame(luma, outW, outH)
    }

    /** Decodes a file to a mutable ARGB_8888 bitmap, optionally downsampling to [maxDim]. */
    fun decodeSampled(file: File, maxDim: Int = 2400): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val largest = max(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: error("Failed to decode ${file.absolutePath}")
    }

    /** Returns a rotated copy and recycles the source if a rotation actually happens. */
    fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (rotated != src) src.recycle()
        return rotated
    }

    /** Writes [bitmap] as JPEG to [file] and returns it. Does not recycle [bitmap]. */
    fun saveJpeg(bitmap: Bitmap, file: File, quality: Int = 90): File {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
        }
        return file
    }
}
