package com.fluidscan.pro.ui.screens.scanner.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fluidscan.pro.domain.model.Quadrilateral
import com.fluidscan.pro.service.scan.EdgeResult
import com.fluidscan.pro.ui.theme.EdgeDetectStroke
import com.fluidscan.pro.ui.theme.FluidScanMotion

/**
 * The signature spring-loaded document border. Each corner is independently animated to
 * its detected target with a **damping-ratio-0.6** spring, producing the lively
 * overshoot/settle ("border snapping") as the document moves under the camera.
 *
 * When no confident document is present the quad relaxes to [Quadrilateral.FULL] and the
 * stroke fades to a translucent "searching" state.
 */
@Composable
fun EdgeDetectionOverlay(
    edge: EdgeResult,
    modifier: Modifier = Modifier
) {
    val target = if (edge.hasDocument) edge.quad!! else Quadrilateral.FULL
    val locked = edge.hasDocument

    val snap = FluidScanMotion.Springs.snap<Offset>()
    val tl by animateValueAsState(target.topLeft, Offset.VectorConverter, snap, label = "tl")
    val tr by animateValueAsState(target.topRight, Offset.VectorConverter, snap, label = "tr")
    val br by animateValueAsState(target.bottomRight, Offset.VectorConverter, snap, label = "br")
    val bl by animateValueAsState(target.bottomLeft, Offset.VectorConverter, snap, label = "bl")

    val strokeColor by animateColorAsState(
        targetValue = if (locked) EdgeDetectStroke else EdgeDetectStroke.copy(alpha = 0.45f),
        label = "edgeColor"
    )
    val fillColor = strokeColor.copy(alpha = if (locked) 0.12f else 0.04f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun p(o: Offset) = Offset(o.x * w, o.y * h)

        val pTl = p(tl); val pTr = p(tr); val pBr = p(br); val pBl = p(bl)

        val path = Path().apply {
            moveTo(pTl.x, pTl.y)
            lineTo(pTr.x, pTr.y)
            lineTo(pBr.x, pBr.y)
            lineTo(pBl.x, pBl.y)
            close()
        }
        drawPath(path, color = fillColor)
        drawPath(path, color = strokeColor, style = Stroke(width = 3.dp.toPx()))

        // Corner accents (the "grippy" L-brackets) emphasise the snap.
        listOf(pTl, pTr, pBr, pBl).forEach { corner ->
            drawCircle(
                color = strokeColor,
                radius = (if (locked) 7 else 5).dp.toPx(),
                center = corner
            )
            drawCircle(
                color = Color.White,
                radius = (if (locked) 3 else 2).dp.toPx(),
                center = corner
            )
        }
    }
}
