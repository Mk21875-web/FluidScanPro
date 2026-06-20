package com.fluidscan.pro.core.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [InkSmoothing.buildPath] produces an `androidx.compose.ui.graphics.Path` backed by
 * `android.graphics.Path`, so these run under Robolectric. [InkSmoothing.denoise] is pure
 * math over [Offset] and is asserted exactly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InkSmoothingTest {

    private val canvas = Size(100f, 100f)

    @Test
    fun `empty input yields an empty path`() {
        assertThat(InkSmoothing.buildPath(emptyList(), canvas).isEmpty).isTrue()
    }

    @Test
    fun `single point yields a non-empty path`() {
        val path = InkSmoothing.buildPath(listOf(Offset(0.5f, 0.5f)), canvas)
        assertThat(path.isEmpty).isFalse()
    }

    @Test
    fun `a multi-point stroke builds without error and is non-empty`() {
        val points = listOf(
            Offset(0.1f, 0.1f),
            Offset(0.3f, 0.4f),
            Offset(0.6f, 0.2f),
            Offset(0.9f, 0.7f)
        )
        val path = InkSmoothing.buildPath(points, canvas)
        assertThat(path.isEmpty).isFalse()
    }

    @Test
    fun `denoise returns input unchanged when fewer than three points`() {
        val pts = listOf(Offset(0f, 0f), Offset(1f, 1f))
        assertThat(InkSmoothing.denoise(pts)).isEqualTo(pts)
    }

    @Test
    fun `denoise keeps the first point and pulls later points toward the running average`() {
        val pts = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 0f))
        val out = InkSmoothing.denoise(pts, factor = 0.5f)

        // First sample is preserved exactly.
        assertThat(out[0]).isEqualTo(Offset(0f, 0f))
        // EMA with factor 0.5: 0 -> 5 -> 7.5
        assertThat(out[1].x).isWithin(1e-4f).of(5f)
        assertThat(out[2].x).isWithin(1e-4f).of(7.5f)
    }

    @Test
    fun `denoise with factor of one tracks the raw points exactly`() {
        val pts = listOf(Offset(0f, 0f), Offset(3f, 4f), Offset(7f, 1f))
        val out = InkSmoothing.denoise(pts, factor = 1f)
        assertThat(out).isEqualTo(pts)
    }
}
