package com.fluidscan.pro.domain.model

import androidx.compose.runtime.Immutable

/** Axis-aligned rectangle in normalized (0..1) page coordinates — UI/zoom independent. */
@Immutable
data class NormRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

/** A single recognized line of text plus where it sits on the page. */
@Immutable
data class OcrLine(
    val text: String,
    val box: NormRect
)

/** Script families ML Kit can recognize offline (covers 100+ languages). */
enum class OcrScript { LATIN, CHINESE, DEVANAGARI, JAPANESE, KOREAN }

@Immutable
data class OcrResult(
    val fullText: String,
    val lines: List<OcrLine>,
    val script: OcrScript
) {
    companion object {
        val EMPTY = OcrResult("", emptyList(), OcrScript.LATIN)
    }
}
