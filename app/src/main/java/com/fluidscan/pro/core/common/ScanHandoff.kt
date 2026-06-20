package com.fluidscan.pro.core.common

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A tiny in-memory bridge that carries the freshly-scanned page images from the scanner to
 * the PDF editor. This is deliberately ephemeral; durable storage (Room) arrives in Phase 3
 * and will replace this handoff with real document persistence.
 */
@Singleton
class ScanHandoff @Inject constructor() {
    @Volatile var pageImageUris: List<Uri> = emptyList()
        private set

    @Volatile var title: String = "Scan"
        private set

    /** Set when re-opening an existing persisted document, so the editor keeps its identity. */
    @Volatile var documentId: String? = null
        private set

    fun set(uris: List<Uri>, title: String, documentId: String? = null) {
        this.pageImageUris = uris
        this.title = title
        this.documentId = documentId
    }

    fun consume(): List<Uri> = pageImageUris.also { pageImageUris = emptyList() }
}
