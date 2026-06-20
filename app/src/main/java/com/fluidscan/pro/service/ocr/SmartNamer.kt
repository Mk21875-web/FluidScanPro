package com.fluidscan.pro.service.ocr

import com.fluidscan.pro.domain.model.OcrResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives a human-friendly document name from OCR output: the first substantial line,
 * sanitized and title-cased. Falls back to a timestamped default when nothing usable is found.
 */
@Singleton
class SmartNamer @Inject constructor() {

    fun suggest(result: OcrResult, maxWords: Int = 6): String {
        val candidate = result.lines
            .map { it.text.trim() }
            .firstOrNull { line -> line.count { it.isLetterOrDigit() } >= 3 }
            ?: return "Scan"

        val cleaned = candidate
            .filter { it.isLetterOrDigit() || it.isWhitespace() }
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .take(maxWords)
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        return cleaned.ifBlank { "Scan" }
    }
}
