package com.fluidscan.pro.ui.screens.scanner.components

import android.net.Uri
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluidscan.pro.domain.model.Quadrilateral
import com.fluidscan.pro.ui.theme.EdgeDetectStroke
import com.fluidscan.pro.ui.theme.FluidScanMotion
import kotlin.math.hypot

/**
 * Manual crop editor with **magnetic snap** handles. The actual snapping logic + haptic
 * lives in the ViewModel ([com.fluidscan.pro.ui.screens.scanner.ScannerViewModel.moveCropCorner]);
 * this view just reports normalized drag positions and renders the (snapped) quad with a
 * gentle spring so the handle visibly "clicks" onto guides.
 */
@Composable
fun CropEditor(
    imageUri: Uri,
    quad: Quadrilateral,
    onCornerMove: (cornerIndex: Int, normalized: Offset) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val activeCorner = remember { intArrayOf(-1) }

    val spring = FluidScanMotion.Springs.lift<Offset>()
    val tl by animateValueAsState(quad.topLeft, Offset.VectorConverter, spring, label = "ctl")
    val tr by animateValueAsState(quad.topRight, Offset.VectorConverter, spring, label = "ctr")
    val br by animateValueAsState(quad.bottomRight, Offset.VectorConverter, spring, label = "cbr")
    val bl by animateValueAsState(quad.bottomLeft, Offset.VectorConverter, spring, label = "cbl")
    val animated = listOf(tl, tr, br, bl)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .onSizeChanged { canvasSize = it }
                .pointerInput(canvasSize) {
                    if (canvasSize == IntSize.Zero) return@pointerInput
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat()
                    detectDragGestures(
                        onDragStart = { start ->
                            activeCorner[0] = nearestCorner(start, quad, w, h)
                        },
                        onDragEnd = { activeCorner[0] = -1 },
                        onDragCancel = { activeCorner[0] = -1 }
                    ) { change, _ ->
                        change.consume()
                        val idx = activeCorner[0]
                        if (idx in 0..3) {
                            val n = Offset(
                                (change.position.x / w).coerceIn(0f, 1f),
                                (change.position.y / h).coerceIn(0f, 1f)
                            )
                            onCornerMove(idx, n)
                        }
                    }
                }
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Page to crop",
                modifier = Modifier.fillMaxSize()
            )

            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                fun p(o: Offset) = Offset(o.x * w, o.y * h)
                val pts = animated.map { p(it) }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, EdgeDetectStroke.copy(alpha = 0.15f))
                drawPath(path, EdgeDetectStroke, style = Stroke(width = 2.5.dp.toPx()))
                pts.forEach { c ->
                    drawCircle(Color.White, radius = 11.dp.toPx(), center = c)
                    drawCircle(EdgeDetectStroke, radius = 8.dp.toPx(), center = c)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            Button(onClick = onConfirm) { Text("Apply crop") }
        }
    }
}

private fun nearestCorner(point: Offset, quad: Quadrilateral, w: Float, h: Float): Int {
    val px = quad.corners.map { Offset(it.x * w, it.y * h) }
    var best = -1
    var bestDist = Float.MAX_VALUE
    px.forEachIndexed { i, c ->
        val d = hypot(point.x - c.x, point.y - c.y)
        if (d < bestDist) { bestDist = d; best = i }
    }
    return best
}
