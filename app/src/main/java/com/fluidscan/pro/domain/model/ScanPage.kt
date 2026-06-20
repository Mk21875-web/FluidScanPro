package com.fluidscan.pro.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable

/**
 * A single captured page within an in-progress scan session.
 *
 * @param id stable identity (drives Compose keys for the reorder/card-stack animations).
 * @param rawUri the unprocessed full-res capture on disk.
 * @param processedUri the perspective-corrected + filtered result (null until processed).
 * @param cropQuad the document boundary in normalized coords (editable in the crop screen).
 * @param filter the filter applied to this page.
 * @param rotationDegrees user rotation applied on top of auto-orientation.
 */
@Immutable
data class ScanPage(
    val id: String,
    val rawUri: Uri,
    val processedUri: Uri? = null,
    val cropQuad: Quadrilateral = Quadrilateral.FULL,
    val filter: ScanFilter = ScanFilter.Default,
    val rotationDegrees: Int = 0
) {
    val displayUri: Uri get() = processedUri ?: rawUri
    val isProcessed: Boolean get() = processedUri != null
}
