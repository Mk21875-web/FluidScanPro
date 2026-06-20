package com.fluidscan.pro.ui.screens.utilities.qr

import androidx.compose.runtime.Immutable
import com.fluidscan.pro.domain.model.BarcodeResult

@Immutable
data class QrState(
    val result: BarcodeResult? = null
) {
    val detected: Boolean get() = result != null
}

sealed interface QrIntent {
    data class Detected(val result: BarcodeResult) : QrIntent
    data object Reset : QrIntent
}

sealed interface QrEffect {
    data object DetectionHaptic : QrEffect
    data class Copy(val text: String) : QrEffect
    data class OpenUrl(val url: String) : QrEffect
}
