package com.fluidscan.pro.core.util

import com.fluidscan.pro.domain.model.ScanFilter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveFilterTest {

    @Test
    fun `ORIGINAL applies no color filter`() {
        assertThat(ScanFilter.ORIGINAL.toLiveColorFilter()).isNull()
    }

    @Test
    fun `every non-original filter produces a color filter`() {
        val withFilter = ScanFilter.entries.filter { it != ScanFilter.ORIGINAL }
        for (filter in withFilter) {
            assertThat(filter.toLiveColorFilter()).isNotNull()
        }
    }
}
