package com.fluidscan.pro.ui.screens.editor.components

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.fluidscan.pro.core.util.InkSmoothing
import com.fluidscan.pro.domain.model.Annotation
import com.fluidscan.pro.domain.model.EditablePage
import com.fluidscan.pro.domain.model.ShapeKind
import com.fluidscan.pro.ui.screens.editor.EditorTool
import com.fluidscan.pro.ui.theme.FluidScanMotion
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The drawing surface for ink + shapes. Pointer handling is tool-aware:
 *  - PEN/HIGHLIGHTER → freehand drag, smoothed live with [InkSmoothing] (Bézier).
 *  - SHAPE → drag-out with a **spring-loaded preview** (the far corner springs/overshoots).
 *  - TEXT/STAMP → tap to place.
 *  - ERASER → tap nearest annotation to delete.
 *
 * Stamps & text are rendered/handled by [SignatureStampLayer] on top of this canvas.
 */
@Composable
fun AnnotationCanvas(
    page: EditablePage,
    tool: EditorTool,
    colorArgb: Long,
    strokeWidth: Float,
    liveInk: List<Offset>,
    liveShape: Annotation.Shape?,
    onInkStart: (Offset) -> Unit,
    onInkMove: (Offset) -> Unit,
    onInkEnd: () -> Unit,
    onShapeStart: (Offset) -> Unit,
    onShapeMove: (Offset) -> Unit,
    onShapeEnd: () -> Unit,
    onTapPlace: (Offset) -> Unit,
    onEraseAt: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Spring the live shape's moving corner for the signature overshoot feel.
    val springEnd by animateValueAsState(
        targetValue = liveShape?.end ?: Offset.Zero,
        typeConverter = Offset.VectorConverter,
        animationSpec = FluidScanMotion.Springs.snap(),
        label = "shapeEnd"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(tool, page.id) {
                when (tool) {
                    EditorTool.PEN, EditorTool.HIGHLIGHTER -> detectDragGestures(
                        onDragStart = { onInkStart(it.normalize(size.width, size.height)) },
                        onDragEnd = { onInkEnd() },
                        onDragCancel = { onInkEnd() },
                        onDrag = { change, _ ->
                            change.consume()
                            onInkMove(change.position.normalize(size.width, size.height))
                        }
                    )
                    EditorTool.SHAPE -> detectDragGestures(
                        onDragStart = { onShapeStart(it.normalize(size.width, size.height)) },
                        onDragEnd = { onShapeEnd() },
                        onDragCancel = { onShapeEnd() },
                        onDrag = { change, _ ->
                            change.consume()
                            onShapeMove(change.position.normalize(size.width, size.height))
                        }
                    )
                    EditorTool.TEXT, EditorTool.STAMP -> detectTapGestures(
                        onTap = { onTapPlace(it.normalize(size.width, size.height)) }
                    )
                    EditorTool.ERASER -> detectTapGestures(
                        onTap = { onEraseAt(it.normalize(size.width, size.height)) }
                    )
                    EditorTool.PAN -> { /* gestures handled by the page container */ }
                }
            }
    ) {
        // Committed ink + shapes (stamps/text are drawn by the overlay layer).
        page.annotations.sortedBy { it.z }.forEach { ann ->
            when (ann) {
                is Annotation.Ink -> drawInk(ann.points, Color(ann.colorArgb.toInt()), ann.strokeWidth, ann.isHighlighter)
                is Annotation.Shape -> drawShape(ann.kind, ann.start, ann.end, Color(ann.colorArgb.toInt()), ann.strokeWidth)
                else -> Unit
            }
        }
        // Live previews.
        if (liveInk.isNotEmpty()) {
            drawInk(liveInk, Color(colorArgb.toInt()), strokeWidth, tool == EditorTool.HIGHLIGHTER)
        }
        liveShape?.let {
            drawShape(it.kind, it.start, springEnd, Color(colorArgb.toInt()), strokeWidth)
        }
    }
}

private fun Offset.normalize(w: Int, h: Int) =
    Offset((x / w.toFloat()).coerceIn(0f, 1f), (y / h.toFloat()).coerceIn(0f, 1f))

private fun DrawScope.drawInk(points: List<Offset>, color: Color, width: Float, highlighter: Boolean) {
    if (points.isEmpty()) return
    val path: Path = InkSmoothing.buildPath(points, Size(size.width, size.height))
    drawPath(
        path = path,
        color = if (highlighter) color.copy(alpha = 0.35f) else color,
        style = Stroke(width = width * size.width, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawShape(kind: ShapeKind, start: Offset, end: Offset, color: Color, width: Float) {
    val s = Offset(start.x * size.width, start.y * size.height)
    val e = Offset(end.x * size.width, end.y * size.height)
    val stroke = Stroke(width = width * size.width, cap = StrokeCap.Round)
    when (kind) {
        ShapeKind.RECTANGLE -> drawRect(
            color = color,
            topLeft = Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
            size = Size(kotlin.math.abs(e.x - s.x), kotlin.math.abs(e.y - s.y)),
            style = stroke
        )
        ShapeKind.OVAL -> drawOval(
            color = color,
            topLeft = Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
            size = Size(kotlin.math.abs(e.x - s.x), kotlin.math.abs(e.y - s.y)),
            style = stroke
        )
        ShapeKind.LINE -> drawLine(color, s, e, strokeWidth = width * size.width, cap = StrokeCap.Round)
        ShapeKind.ARROW -> {
            drawLine(color, s, e, strokeWidth = width * size.width, cap = StrokeCap.Round)
            val angle = atan2((e.y - s.y), (e.x - s.x))
            val head = width * size.width * 6f
            val a1 = angle - 0.5f
            val a2 = angle + 0.5f
            drawLine(color, e, Offset(e.x - head * cos(a1), e.y - head * sin(a1)), strokeWidth = width * size.width, cap = StrokeCap.Round)
            drawLine(color, e, Offset(e.x - head * cos(a2), e.y - head * sin(a2)), strokeWidth = width * size.width, cap = StrokeCap.Round)
        }
    }
}

/** Hit-test helper used by the eraser: index of nearest annotation within [tolerance]. */
fun nearestAnnotationId(page: EditablePage, at: Offset, tolerance: Float = 0.04f): String? {
    var best: String? = null
    var bestDist = Float.MAX_VALUE
    page.annotations.forEach { ann ->
        val anchor = when (ann) {
            is Annotation.Ink -> ann.points.firstOrNull()
            is Annotation.Shape -> ann.start
            is Annotation.Text -> ann.position
            is Annotation.Stamp -> ann.center
        } ?: return@forEach
        val d = hypot(anchor.x - at.x, anchor.y - at.y)
        if (d < bestDist) { bestDist = d; best = ann.id }
    }
    return if (bestDist <= tolerance) best else null
}
