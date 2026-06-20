package com.fluidscan.pro.service.scan

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import com.fluidscan.pro.domain.model.Quadrilateral
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real `android.graphics.Bitmap`/`Matrix` path under Robolectric. The output
 * bitmap is sized from the homography's edge lengths, so we assert those dimensions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PerspectiveTransformerTest {

    private val transformer = PerspectiveTransformer()

    private fun quad(
        tl: Offset, tr: Offset, br: Offset, bl: Offset
    ) = Quadrilateral(topLeft = tl, topRight = tr, bottomRight = br, bottomLeft = bl)

    @Test
    fun `a full-frame quad yields an output matching the source dimensions`() {
        val source = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val full = quad(Offset(0f, 0f), Offset(1f, 0f), Offset(1f, 1f), Offset(0f, 1f))

        val out = transformer.correct(source, full)

        assertThat(out.width).isEqualTo(200)
        assertThat(out.height).isEqualTo(300)
    }

    @Test
    fun `output size comes from the longest opposing edges of an inset quad`() {
        // Document occupies the left half horizontally, full height.
        val source = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        val half = quad(Offset(0f, 0f), Offset(0.5f, 0f), Offset(0.5f, 1f), Offset(0f, 1f))

        val out = transformer.correct(source, half)

        // width = 0.5 * 400 = 200, height = 1.0 * 200 = 200
        assertThat(out.width).isEqualTo(200)
        assertThat(out.height).isEqualTo(200)
    }

    @Test
    fun `a degenerate zero-area quad still yields a valid one-by-one bitmap`() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val point = quad(Offset(0f, 0f), Offset(0f, 0f), Offset(0f, 0f), Offset(0f, 0f))

        val out = transformer.correct(source, point)

        assertThat(out.width).isAtLeast(1)
        assertThat(out.height).isAtLeast(1)
    }
}
