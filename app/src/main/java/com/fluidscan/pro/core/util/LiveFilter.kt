package com.fluidscan.pro.core.util

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.fluidscan.pro.domain.model.ScanFilter

/**
 * Compose-side approximations of the engine filters, used for the **live** cross-fade
 * preview carousel. They run as a GPU `ColorFilter` on the already-decoded preview
 * bitmap, so swiping between filters costs nothing (no re-decode, no extra bitmap).
 *
 * The committed result still goes through `service.scan.ImageFilters` for the exact
 * (e.g. true thresholded B&W) output.
 */
fun ScanFilter.toLiveColorFilter(): ColorFilter? = when (this) {
    ScanFilter.ORIGINAL -> null
    ScanFilter.GRAYSCALE -> ColorFilter.colorMatrix(grayscale())
    ScanFilter.BLACK_WHITE -> ColorFilter.colorMatrix(highContrastMono())
    ScanFilter.MAGIC_COLOR -> ColorFilter.colorMatrix(magicColor())
    ScanFilter.LIGHTEN -> ColorFilter.colorMatrix(lighten())
}

private fun grayscale() = ColorMatrix().apply { setToSaturation(0f) }

private fun highContrastMono(): ColorMatrix {
    val m = ColorMatrix().apply { setToSaturation(0f) }
    val c = 2.4f
    val t = (-0.5f * c + 0.5f) * 255f
    m.timesAssign(
        ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return m
}

private fun magicColor(): ColorMatrix {
    val m = ColorMatrix().apply { setToSaturation(1.35f) }
    val c = 1.25f
    val t = (-0.5f * c + 0.5f) * 255f
    m.timesAssign(
        ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    return m
}

private fun lighten() = ColorMatrix(
    floatArrayOf(
        1.18f, 0f, 0f, 0f, 18f,
        0f, 1.18f, 0f, 0f, 18f,
        0f, 0f, 1.18f, 0f, 18f,
        0f, 0f, 0f, 1f, 0f
    )
)
