package com.fluidscan.pro.ui.screens.ocr.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.fluidscan.pro.domain.model.OcrLine

/**
 * Draws translucent highlight boxes over recognized lines as they are revealed one by one,
 * plus a bright "scanner" bar on the line currently being read — the line-by-line OCR sweep.
 */
@Composable
fun OcrLineHighlightOverlay(
    lines: List<OcrLine>,
    revealedCount: Int,
    highlight: Color,
    scanner: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        lines.take(revealedCount).forEach { line ->
            drawRoundRect(
                color = highlight.copy(alpha = 0.28f),
                topLeft = Offset(line.box.left * w, line.box.top * h),
                size = Size(line.box.width * w, line.box.height * h),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }

        // The line currently being scanned gets a solid leading bar.
        lines.getOrNull(revealedCount)?.let { next ->
            val y = next.box.top * h
            drawRoundRect(
                color = scanner.copy(alpha = 0.9f),
                topLeft = Offset(next.box.left * w, y),
                size = Size(next.box.width * w, (next.box.height * h).coerceAtLeast(3f) * 0.18f + 3f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
