package com.fluidscan.pro.service.scan

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.fluidscan.pro.domain.model.Quadrilateral
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.math.max

/**
 * Applies perspective (keystone) correction: takes the four detected corners of a
 * document inside [source] and warps them to a clean upright rectangle.
 *
 * Uses [Matrix.setPolyToPoly] which solves the 4-point homography for us — no native
 * dependency required. Heavy; always call off the UI thread.
 */
class PerspectiveTransformer @Inject constructor() {

    /**
     * @param source full-resolution captured bitmap (not recycled here).
     * @param quad   document corners in **normalized** (0..1) coordinates.
     * @return a new upright bitmap sized to the document's real aspect ratio.
     */
    fun correct(source: Bitmap, quad: Quadrilateral): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()
        val px = quad.toPixels(androidx.compose.ui.geometry.Size(w, h))

        val tl = px.topLeft; val tr = px.topRight; val br = px.bottomRight; val bl = px.bottomLeft

        // Estimate output size from the average of opposite edges.
        val widthTop = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble())
        val widthBottom = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble())
        val heightLeft = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble())
        val heightRight = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble())

        val outW = max(1.0, max(widthTop, widthBottom)).toInt()
        val outH = max(1.0, max(heightLeft, heightRight)).toInt()

        val src = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )
        val dst = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix().apply { setPolyToPoly(src, 0, dst, 0, 4) }

        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }
}
