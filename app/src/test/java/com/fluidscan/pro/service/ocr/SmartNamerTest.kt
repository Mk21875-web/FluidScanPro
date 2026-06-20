package com.fluidscan.pro.service.ocr

import com.fluidscan.pro.domain.model.NormRect
import com.fluidscan.pro.domain.model.OcrLine
import com.fluidscan.pro.domain.model.OcrResult
import com.fluidscan.pro.domain.model.OcrScript
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmartNamerTest {

    private val namer = SmartNamer()

    private fun result(vararg lines: String): OcrResult {
        val box = NormRect(0f, 0f, 1f, 0.1f)
        return OcrResult(
            fullText = lines.joinToString("\n"),
            lines = lines.map { OcrLine(it, box) },
            script = OcrScript.LATIN
        )
    }

    @Test
    fun `title-cases the first substantial line`() {
        assertThat(namer.suggest(result("invoice number 4471"))).isEqualTo("Invoice Number 4471")
    }

    @Test
    fun `skips lines without enough alphanumeric content`() {
        assertThat(namer.suggest(result("--", "  ", "tax report"))).isEqualTo("Tax Report")
    }

    @Test
    fun `strips punctuation and collapses whitespace`() {
        assertThat(namer.suggest(result("  Hello,   World!!!  "))).isEqualTo("Hello World")
    }

    @Test
    fun `limits to maxWords`() {
        val long = "one two three four five six seven eight"
        assertThat(namer.suggest(result(long), maxWords = 3)).isEqualTo("One Two Three")
    }

    @Test
    fun `falls back to Scan when no usable line exists`() {
        assertThat(namer.suggest(result("--", "!!", ""))).isEqualTo("Scan")
    }

    @Test
    fun `falls back to Scan for an empty result`() {
        assertThat(namer.suggest(OcrResult.EMPTY)).isEqualTo("Scan")
    }
}
