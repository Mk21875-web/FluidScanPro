package com.fluidscan.pro.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable

/** A page inside the PDF editor: a base image plus its annotation layer. */
@Immutable
data class EditablePage(
    val id: String,
    val imageUri: Uri,
    val annotations: List<Annotation> = emptyList(),
    val rotationDegrees: Int = 0
) {
    fun upsert(annotation: Annotation): EditablePage {
        val idx = annotations.indexOfFirst { it.id == annotation.id }
        return if (idx >= 0) copy(annotations = annotations.toMutableList().also { it[idx] = annotation })
        else copy(annotations = annotations + annotation)
    }

    fun remove(annotationId: String): EditablePage =
        copy(annotations = annotations.filterNot { it.id == annotationId })

    val topZ: Int get() = (annotations.maxOfOrNull { it.z } ?: 0) + 1
}

/** The whole document being edited; [pages] order is page order. */
@Immutable
data class EditableDocument(
    val id: String,
    val title: String,
    val pages: List<EditablePage> = emptyList(),
    val isPasswordProtected: Boolean = false
) {
    fun reordered(from: Int, to: Int): EditableDocument {
        if (from == to || from !in pages.indices || to !in pages.indices) return this
        val m = pages.toMutableList()
        m.add(to, m.removeAt(from))
        return copy(pages = m)
    }

    fun replacePage(page: EditablePage): EditableDocument =
        copy(pages = pages.map { if (it.id == page.id) page else it })

    fun removePage(id: String): EditableDocument =
        copy(pages = pages.filterNot { it.id == id })
}
