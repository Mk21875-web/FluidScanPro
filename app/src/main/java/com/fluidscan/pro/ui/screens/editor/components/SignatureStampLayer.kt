package com.fluidscan.pro.ui.screens.editor.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fluidscan.pro.domain.model.Annotation
import com.fluidscan.pro.domain.model.EditablePage
import com.fluidscan.pro.domain.model.StampSource
import com.fluidscan.pro.ui.theme.FluidScanMotion
import kotlin.math.roundToInt

/**
 * Interaction layer for stamps/signatures and text boxes — drawn above [AnnotationCanvas].
 *
 *  - **Parallax drag**: while a stamp is dragged it tilts toward the drag direction and
 *    its drop-shadow offsets opposite the motion, giving a layered, physical feel.
 *  - **Seal-press**: when released, the stamp does a quick squash→overshoot "press" via a
 *    bouncy spring and fires the seal-press haptic (raised by the ViewModel).
 *  - Text boxes become an inline [BasicTextField] when selected, so the system keyboard's
 *    IME insets can lift them (handled by the screen's `imePadding`).
 */
@Composable
fun SignatureStampLayer(
    page: EditablePage,
    selectedId: String?,
    onMoveStamp: (id: String, center: Offset) -> Unit,
    onDropStamp: (id: String) -> Unit,
    onEditText: (id: String, value: String) -> Unit,
    onSelect: (id: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvas by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier.fillMaxSize().onSizeChanged { canvas = it }) {
        if (canvas == IntSize.Zero) return@Box
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        page.annotations.sortedBy { it.z }.forEach { ann ->
            when (ann) {
                is Annotation.Stamp -> DraggableStamp(
                    stamp = ann,
                    canvasW = w,
                    canvasH = h,
                    selected = ann.id == selectedId,
                    onMove = onMoveStamp,
                    onDrop = onDropStamp,
                    onSelect = onSelect
                )
                is Annotation.Text -> EditableText(
                    text = ann,
                    canvasW = w,
                    canvasH = h,
                    selected = ann.id == selectedId,
                    onEdit = onEditText,
                    onSelect = onSelect
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun DraggableStamp(
    stamp: Annotation.Stamp,
    canvasW: Float,
    canvasH: Float,
    selected: Boolean,
    onMove: (String, Offset) -> Unit,
    onDrop: (String) -> Unit,
    onSelect: (String?) -> Unit
) {
    val density = LocalDensity.current
    // Squash→overshoot "seal press" scale, animated on each drop.
    val pressScale = remember { Animatable(1f) }
    val tilt = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .offsetCenter(stamp.center, canvasW, canvasH)
            .graphicsLayer {
                rotationZ = stamp.rotationDegrees + tilt.value
                scaleX = stamp.scale * pressScale.value
                scaleY = stamp.scale * pressScale.value
                shadowElevation = with(density) { (if (selected) 14 else 6).dp.toPx() }
            }
            .pointerInput(stamp.id) {
                detectDragGestures(
                    onDragStart = { onSelect(stamp.id) },
                    onDrag = { change, drag ->
                        change.consume()
                        val newCenter = Offset(
                            (stamp.center.x + drag.x / canvasW).coerceIn(0f, 1f),
                            (stamp.center.y + drag.y / canvasH).coerceIn(0f, 1f)
                        )
                        onMove(stamp.id, newCenter)
                    },
                    onDragEnd = { onDrop(stamp.id) },
                    onDragCancel = { onDrop(stamp.id) }
                )
            }
    ) {
        when (val src = stamp.source) {
            is StampSource.Image -> AsyncImage(
                model = src.uri,
                contentDescription = "Signature",
                modifier = Modifier.padding(4.dp)
            )
            is StampSource.Builtin -> Box(
                modifier = Modifier
                    .border(2.dp, Color(src.tintArgb.toInt()), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = src.label,
                    color = Color(src.tintArgb.toInt()),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Drive the seal-press whenever the stamp is (re)dropped: parent re-emits selection.
    LaunchedEffect(stamp.center) {
        // Tilt toward last movement, then settle.
        tilt.snapTo(6f)
        tilt.animateTo(0f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f))
    }
    LaunchedEffect(selected) {
        if (selected) {
            pressScale.snapTo(1.18f)
            pressScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = 650f) // bouncy press
            )
        }
    }
}

@Composable
private fun EditableText(
    text: Annotation.Text,
    canvasW: Float,
    canvasH: Float,
    selected: Boolean,
    onEdit: (String, String) -> Unit,
    onSelect: (String?) -> Unit
) {
    val fontPx = text.fontSizeSp / 100f * canvasW
    val fontSp = with(LocalDensity.current) { fontPx.toSp() }
    Box(
        modifier = Modifier
            .offsetTopLeft(text.position, canvasW, canvasH)
            .then(if (selected) Modifier.border(1.dp, Color(text.colorArgb.toInt()), RoundedCornerShape(4.dp)) else Modifier)
            .pointerInput(text.id) { detectDragGestures(onDragStart = { onSelect(text.id) }) { c, _ -> c.consume() } }
            .padding(4.dp)
    ) {
        if (selected) {
            BasicTextField(
                value = text.text,
                onValueChange = { onEdit(text.id, it) },
                textStyle = LocalTextStyle.current.copy(color = Color(text.colorArgb.toInt()), fontSize = fontSp)
            )
        } else {
            Text(
                text = text.text.ifEmpty { "Tap to edit" },
                color = Color(text.colorArgb.toInt()),
                fontSize = fontSp,
                textAlign = TextAlign.Start
            )
        }
    }
}

private fun Modifier.offsetCenter(center: Offset, w: Float, h: Float): Modifier =
    positionPx(center.x * w, center.y * h, centered = true)

private fun Modifier.offsetTopLeft(pos: Offset, w: Float, h: Float): Modifier =
    positionPx(pos.x * w, pos.y * h, centered = false)

/** Positions a composable by absolute pixel coordinates (optionally centered on itself). */
private fun Modifier.positionPx(xPx: Float, yPx: Float, centered: Boolean): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val ox = if (centered) (xPx - placeable.width / 2f) else xPx
        val oy = if (centered) (yPx - placeable.height / 2f) else yPx
        layout(placeable.width, placeable.height) {
            placeable.place(IntOffset(ox.roundToInt(), oy.roundToInt()))
        }
    }
