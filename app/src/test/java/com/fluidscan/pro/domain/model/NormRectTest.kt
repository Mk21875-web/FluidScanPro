package com.fluidscan.pro.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NormRectTest {

    @Test
    fun `width and height are the span between edges`() {
        val rect = NormRect(left = 0.2f, top = 0.1f, right = 0.8f, bottom = 0.6f)
        assertThat(rect.width).isWithin(1e-6f).of(0.6f)
        assertThat(rect.height).isWithin(1e-6f).of(0.5f)
    }

    @Test
    fun `inverted edges clamp span to zero rather than going negative`() {
        val rect = NormRect(left = 0.9f, top = 0.9f, right = 0.1f, bottom = 0.1f)
        assertThat(rect.width).isEqualTo(0f)
        assertThat(rect.height).isEqualTo(0f)
    }
}
