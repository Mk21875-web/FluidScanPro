package com.fluidscan.pro.service.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.fluidscan.pro.core.util.ImageProcessing
import com.fluidscan.pro.domain.model.Annotation
import com.fluidscan.pro.domain.model.EditablePage
import com.fluidscan.pro.domain.model.ShapeKind
import com.fluidscan.pro.domain.model.StampSource
import java.io.File
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders an [EditablePage] (base image + annotation layer) into a single flattened
 * [Bitmap] suitable for embedding into a PDF page. Annotation geometry is normalized, so
 * it scales to whatever export resolution [maxDimension] yields.
 *
 * Pure pixel work — call from `Dispatchers.IO`. Caller owns/recycles the returned bitmap.
 */
class PageFlattener @Inject constructor() {

    fun flatten(page: EditablePage, maxDimension: Int = 2200): Bitmap {
        val base = ImageProcessing
            .decodeSampled(File(requireNotNull(page.imageUri.path)), maxDimension)
        val rotated = ImageProcessing.rotate(base, page.rotationDegrees)

        val w = rotated.width
        val h = rotated.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(rotated, 0f, 0f, null)
        rotated.recycle()

        page.annotations.sortedBy { it.z }.forEach { ann ->
            when (ann) {
                is Annotation.Ink -> drawInk(canvas, ann, w, h)
                is Annotation.Shape -> drawShape(canvas, ann, w, h)
                is Annotation.Text -> drawText(canvas, ann, w, h)
                is Annotation.Stamp -> drawStamp(canvas, ann, w, h)
            }
        }
        return out
    }

    private fun drawInk(canvas: Canvas, ink: Annotation.Ink, w: Int, h: Int) {
        if (ink.points.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = ink.colorArgb.toInt()
            strokeWidth = ink.strokeWidth * w
            if (ink.isHighlighter) alpha = 90
        }
        val pts = ink.points.map { android.graphics.PointF(it.x * w, it.y * h) }
        val path = Path()
        path.moveTo(pts[0].x, pts[0].y)
        if (pts.size < 3) {
            pts.drop(1).forEach { path.lineTo(it.x, it.y) }
        } else {
            val a = 1f / 6f
            for (i in 0 until pts.size - 1) {
                val p0 = pts[if (i == 0) 0 else i - 1]
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val p3 = pts[if (i + 2 < pts.size) i + 2 else pts.size - 1]
                val c1x = p1.x + (p2.x - p0.x) * a
                val c1y = p1.y + (p2.y - p0.y) * a
                val c2x = p2.x - (p3.x - p1.x) * a
                val c2y = p2.y - (p3.y - p1.y) * a
                path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawShape(canvas: Canvas, shape: Annotation.Shape, w: Int, h: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shape.colorArgb.toInt()
            strokeWidth = shape.strokeWidth * w
            style = if (shape.filled) Paint.Style.FILL else Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val sx = shape.start.x * w; val sy = shape.start.y * h
        val ex = shape.end.x * w; val ey = shape.end.y * h
        when (shape.kind) {
            ShapeKind.RECTANGLE -> canvas.drawRect(
                minOf(sx, ex), minOf(sy, ey), maxOf(sx, ex), maxOf(sy, ey), paint
            )
            ShapeKind.OVAL -> canvas.drawOval(
                RectF(minOf(sx, ex), minOf(sy, ey), maxOf(sx, ex), maxOf(sy, ey)), paint
            )
            ShapeKind.LINE -> canvas.drawLine(sx, sy, ex, ey, paint)
            ShapeKind.ARROW -> drawArrow(canvas, sx, sy, ex, ey, paint)
        }
    }

    private fun drawArrow(canvas: Canvas, sx: Float, sy: Float, ex: Float, ey: Float, paint: Paint) {
        canvas.drawLine(sx, sy, ex, ey, paint)
        val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
        val headLen = paint.strokeWidth * 6f
        val a1 = angle - Math.toRadians(28.0)
        val a2 = angle + Math.toRadians(28.0)
        canvas.drawLine(ex, ey, ex - (headLen * cos(a1)).toFloat(), ey - (headLen * sin(a1)).toFloat(), paint)
        canvas.drawLine(ex, ey, ex - (headLen * cos(a2)).toFloat(), ey - (headLen * sin(a2)).toFloat(), paint)
    }

    private fun drawText(canvas: Canvas, text: Annotation.Text, w: Int, h: Int) {
        if (text.text.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = text.colorArgb.toInt()
            textSize = text.fontSizeSp / 100f * w
            typeface = Typeface.DEFAULT
        }
        val x = text.position.x * w
        var y = text.position.y * h + paint.textSize
        val maxWidth = text.widthFraction * w
        wrap(text.text, paint, maxWidth).forEach { line ->
            canvas.drawText(line, x, y, paint)
            y += paint.textSize * 1.3f
        }
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = ArrayList<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    private fun drawStamp(canvas: Canvas, stamp: Annotation.Stamp, w: Int, h: Int) {
        val cx = stamp.center.x * w
        val cy = stamp.center.y * h
        canvas.save()
        canvas.rotate(stamp.rotationDegrees, cx, cy)
        when (val src = stamp.source) {
            is StampSource.Image -> {
                val bmp = runCatching {
                    ImageProcessing.decodeSampled(File(src.uri), 1200)
                }.getOrNull()
                if (bmp != null) {
                    val sw = bmp.width * stamp.scale
                    val sh = bmp.height * stamp.scale
                    val dst = RectF(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2)
                    canvas.drawBitmap(bmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
                    bmp.recycle()
                }
            }
            is StampSource.Builtin -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = src.tintArgb.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 0.004f * w
                }
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = src.tintArgb.toInt()
                    textSize = 0.05f * w * stamp.scale
                    typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val tw = textPaint.measureText(src.label)
                val pad = textPaint.textSize * 0.4f
                val rect = RectF(cx - tw / 2 - pad, cy - textPaint.textSize / 2 - pad,
                    cx + tw / 2 + pad, cy + textPaint.textSize / 2 + pad)
                canvas.drawRoundRect(rect, pad, pad, paint)
                canvas.drawText(src.label, cx, cy + textPaint.textSize / 3, textPaint)
            }
        }
        canvas.restore()
    }

    private companion object {
        @Suppress("unused") val TRANSPARENT = Color.TRANSPARENT
    }
}
