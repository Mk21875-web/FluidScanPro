package com.fluidscan.pro.ui.screens.ocr

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.fluidscan.pro.domain.model.OcrResult
import com.fluidscan.pro.domain.model.OcrScript

@Immutable
data class OcrState(
    val pages: List<Uri> = emptyList(),
    val title: String = "Scan",
    val currentPage: Int = 0,
    val script: OcrScript = OcrScript.LATIN,
    val isRecognizing: Boolean = false,
    val result: OcrResult = OcrResult.EMPTY,
    val isCleaning: Boolean = false,
    val cleanedPages: Map<Int, Uri> = emptyMap(),
    val suggestedName: String? = null
) {
    val currentUri: Uri? get() = cleanedPages[currentPage] ?: pages.getOrNull(currentPage)
    val hasText: Boolean get() = result.fullText.isNotBlank()
}

sealed interface OcrIntent {
    data class LoadPages(val uris: List<Uri>, val title: String) : OcrIntent
    data object LoadFromScan : OcrIntent
    data class SelectPage(val index: Int) : OcrIntent
    data class SetScript(val script: OcrScript) : OcrIntent
    data object RunOcr : OcrIntent
    data object RunCleanup : OcrIntent
    data object GenerateName : OcrIntent
}

sealed interface OcrEffect {
    /** Emitted when recognition finishes so the UI can run the line-by-line highlight sweep. */
    data class TextRecognized(val lineCount: Int) : OcrEffect
    data class NameSuggested(val name: String) : OcrEffect
    data object CleanupDone : OcrEffect
    data class CopyText(val text: String) : OcrEffect
    data class Error(val message: String) : OcrEffect
}
