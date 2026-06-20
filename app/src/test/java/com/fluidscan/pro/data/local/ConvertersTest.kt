package com.fluidscan.pro.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `string list survives a JSON round trip`() {
        val uris = listOf(
            "content://media/external/images/1",
            "file:///data/scan_2.jpg",
            "content://media/external/images/3"
        )
        val encoded = converters.fromStringList(uris)
        assertThat(converters.toStringList(encoded)).isEqualTo(uris)
    }

    @Test
    fun `an empty list round trips as an empty list`() {
        val encoded = converters.fromStringList(emptyList())
        assertThat(converters.toStringList(encoded)).isEmpty()
    }

    @Test
    fun `a blank stored value decodes to an empty list`() {
        assertThat(converters.toStringList("")).isEmpty()
        assertThat(converters.toStringList("   ")).isEmpty()
    }

    @Test
    fun `values containing commas and quotes are preserved`() {
        val tricky = listOf("a,b,c", "with \"quotes\"", "emoji-safe")
        val encoded = converters.fromStringList(tricky)
        assertThat(converters.toStringList(encoded)).isEqualTo(tricky)
    }
}
