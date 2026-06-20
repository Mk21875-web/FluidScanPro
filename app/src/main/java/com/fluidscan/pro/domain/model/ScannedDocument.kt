package com.fluidscan.pro.domain.model

import androidx.compose.runtime.Immutable

/**
 * An in-memory grouping of scanned pages before it is persisted as a Document/PDF
 * (persistence lands in Phase 3). Order of [pages] is the page order.
 */
@Immutable
data class ScannedDocument(
    val id: String,
    val title: String,
    val pages: List<ScanPage> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val pageCount: Int get() = pages.size

    fun reordered(fromIndex: Int, toIndex: Int): ScannedDocument {
        if (fromIndex == toIndex) return this
        val mutable = pages.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        return copy(pages = mutable)
    }

    fun withPage(page: ScanPage): ScannedDocument {
        val idx = pages.indexOfFirst { it.id == page.id }
        return if (idx >= 0) copy(pages = pages.toMutableList().also { it[idx] = page })
        else copy(pages = pages + page)
    }

    fun removePage(id: String): ScannedDocument =
        copy(pages = pages.filterNot { it.id == id })
}
