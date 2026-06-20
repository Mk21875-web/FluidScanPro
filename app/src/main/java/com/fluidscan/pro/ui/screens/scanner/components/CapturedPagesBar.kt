package com.fluidscan.pro.ui.screens.scanner.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.fluidscan.pro.domain.model.ScanPage
import com.fluidscan.pro.ui.theme.FluidScanMotion
import kotlin.math.roundToInt

/**
 * Horizontal strip of captured pages with **long-press drag-to-reorder** and the
 * "lift-and-shadow" effect: the grabbed thumbnail springs up in scale, gains elevation,
 * and floats above its neighbours (`zIndex`) while the list reorders live underneath it.
 *
 * Tap a thumbnail to open the crop editor; tap the ✕ to remove it.
 */
@Composable
fun CapturedPagesBar(
    pages: List<ScanPage>,
    onReorder: (from: Int, to: Int) -> Unit,
    onOpenCrop: (id: String) -> Unit,
    onRemove: (id: String) -> Unit,
    onLiftHaptic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val slotWidthDp = 84.dp // thumbnail (72) + spacing (12)
    val slotWidthPx = with(density) { slotWidthDp.toPx() }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pages.forEachIndexed { index, page ->
            val isDragging = index == draggingIndex
            PageThumbnail(
                page = page,
                index = index,
                isDragging = isDragging,
                dragOffsetX = if (isDragging) dragOffsetX else 0f,
                onOpenCrop = onOpenCrop,
                onRemove = onRemove,
                onDragStart = {
                    draggingIndex = index
                    dragOffsetX = 0f
                    onLiftHaptic()
                },
                onDrag = { delta ->
                    dragOffsetX += delta
                    val shift = (dragOffsetX / slotWidthPx).roundToInt()
                    val target = (draggingIndex + shift).coerceIn(0, pages.lastIndex)
                    if (target != draggingIndex) {
                        onReorder(draggingIndex, target)
                        // Re-baseline so the floating thumb stays under the finger.
                        dragOffsetX -= (target - draggingIndex) * slotWidthPx
                        draggingIndex = target
                    }
                },
                onDragEnd = {
                    draggingIndex = -1
                    dragOffsetX = 0f
                }
            )
        }
    }
}

@Composable
private fun RowScope.PageThumbnail(
    page: ScanPage,
    index: Int,
    isDragging: Boolean,
    dragOffsetX: Float,
    onOpenCrop: (id: String) -> Unit,
    onRemove: (id: String) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val lift by animateFloatAsState(
        targetValue = if (isDragging) 1.12f else 1f,
        animationSpec = FluidScanMotion.Springs.lift(),
        label = "lift"
    )
    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 16f else 2f,
        animationSpec = FluidScanMotion.Springs.lift(),
        label = "elev"
    )

    Box(
        modifier = Modifier
            .width(72.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationX = dragOffsetX
                scaleX = lift
                scaleY = lift
            }
            .pointerInputReorder(index, onDragStart, onDrag, onDragEnd)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.72f)
                .shadow(elevation.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onOpenCrop(page.id) }
        ) {
            AsyncImage(
                model = page.displayUri,
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp)
            )
            // Page number badge.
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable { onRemove(page.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove page",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            Icon(
                Icons.Filled.Crop,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(14.dp)
            )
        }
    }
}

private fun Modifier.pointerInputReorder(
    index: Int,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
): Modifier = this.then(
    Modifier.androidxPointerInput(index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStart() },
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() },
            onDrag = { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.x)
            }
        )
    }
)

// Alias to keep imports tidy at the call site.
private fun Modifier.androidxPointerInput(
    key: Any?,
    block: suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit
): Modifier = this.pointerInput(key, block)
