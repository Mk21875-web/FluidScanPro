package com.fluidscan.pro.domain.model

import androidx.compose.runtime.Immutable

/** Cloud-sync lifecycle for a saved document. */
enum class SyncState { LOCAL, SYNCING, SYNCED, FAILED }

/**
 * A persisted document as shown in the dashboard/file-manager. [pageUris] are the flattened
 * page images; [pdfUri] is the most recently exported PDF (if any).
 */
@Immutable
data class Document(
    val id: String,
    val title: String,
    val pageUris: List<String>,
    val pdfUri: String? = null,
    val thumbnailUri: String? = pageUris.firstOrNull(),
    val isPasswordProtected: Boolean = false,
    val syncState: SyncState = SyncState.LOCAL,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val pageCount: Int get() = pageUris.size
}
