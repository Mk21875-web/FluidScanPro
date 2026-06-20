package com.fluidscan.pro.ui.screens.scanner

import androidx.compose.runtime.Immutable
import com.fluidscan.pro.domain.model.Quadrilateral
import com.fluidscan.pro.domain.model.ScanFilter
import com.fluidscan.pro.domain.model.ScanPage
import com.fluidscan.pro.service.scan.EdgeResult

/** MVI contract for the Core Scanning Engine screen. */

@Immutable
data class ScannerState(
    val capturedPages: List<ScanPage> = emptyList(),
    val liveEdge: EdgeResult = EdgeResult.NONE,
    val selectedFilter: ScanFilter = ScanFilter.Default,
    val flashEnabled: Boolean = false,
    val autoCapture: Boolean = false,
    val isCapturing: Boolean = false,
    val isProcessing: Boolean = false,
    /** Page currently open in the manual crop editor, if any. */
    val editing: ScanPage? = null,
    val editingQuad: Quadrilateral = Quadrilateral.FULL
) {
    val pageCount: Int get() = capturedPages.size
    val hasPages: Boolean get() = capturedPages.isNotEmpty()
}

sealed interface ScannerIntent {
    data class EdgeDetected(val result: EdgeResult) : ScannerIntent
    data object CaptureRequested : ScannerIntent
    data class CapturedRawFile(val absolutePath: String, val rotationDegrees: Int) : ScannerIntent
    data class CaptureFailed(val message: String) : ScannerIntent
    data object ToggleFlash : ScannerIntent
    data object ToggleAutoCapture : ScannerIntent
    data class SelectFilter(val filter: ScanFilter) : ScannerIntent
    data class RemovePage(val id: String) : ScannerIntent
    data class ReorderPages(val fromIndex: Int, val toIndex: Int) : ScannerIntent
    data class OpenCrop(val id: String) : ScannerIntent
    data class CropCornerMoved(val cornerIndex: Int, val normalized: androidx.compose.ui.geometry.Offset) : ScannerIntent
    data object CropConfirmed : ScannerIntent
    data object CropDismissed : ScannerIntent
    data object FinishSession : ScannerIntent
}

sealed interface ScannerEffect {
    /** Tells the camera layer to take a full-res picture into [targetPath]. */
    data class TakePicture(val targetPath: String) : ScannerEffect
    data object EdgeSnapHaptic : ScannerEffect
    data object CaptureHaptic : ScannerEffect
    data object MagneticLockHaptic : ScannerEffect
    data class Error(val message: String) : ScannerEffect
    data class NavigateToReview(val pageIds: List<String>) : ScannerEffect
}
