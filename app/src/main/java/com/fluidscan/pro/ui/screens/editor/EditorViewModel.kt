package com.fluidscan.pro.ui.screens.editor

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluidscan.pro.core.common.DispatcherProvider
import com.fluidscan.pro.core.common.ScanHandoff
import com.fluidscan.pro.domain.model.Annotation
import com.fluidscan.pro.domain.model.Document
import com.fluidscan.pro.domain.model.EditableDocument
import com.fluidscan.pro.domain.model.EditablePage
import com.fluidscan.pro.domain.repository.DocumentRepository
import com.fluidscan.pro.service.pdf.PageFlattener
import com.fluidscan.pro.service.pdf.PdfBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val handoff: ScanHandoff,
    private val documents: DocumentRepository,
    private val flattener: PageFlattener,
    private val pdfBuilder: PdfBuilder
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    private val _effects = Channel<EditorEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Kept out of [EditorState] so it never leaks into snapshots/recomposition. */
    private var password: String? = null

    fun onIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.LoadPages -> loadPages(intent.imageUris, intent.title)
            EditorIntent.LoadFromScan -> loadPages(handoff.consume(), handoff.title, handoff.documentId)
            is EditorIntent.SelectTool -> _state.update { it.copy(tool = intent.tool, selectedAnnotationId = null) }
            is EditorIntent.SelectColor -> _state.update { it.copy(colorArgb = intent.argb) }
            is EditorIntent.SetStrokeWidth -> _state.update { it.copy(strokeWidth = intent.width) }
            is EditorIntent.SelectShapeKind -> _state.update { it.copy(shapeKind = intent.kind) }
            is EditorIntent.SelectPage -> _state.update { it.copy(currentPage = intent.index, selectedAnnotationId = null) }

            is EditorIntent.InkStart -> _state.update { it.copy(liveInk = listOf(intent.p)) }
            is EditorIntent.InkMove -> _state.update { it.copy(liveInk = it.liveInk + intent.p) }
            EditorIntent.InkEnd -> commitInk()

            is EditorIntent.ShapeStart -> startShape(intent.p)
            is EditorIntent.ShapeMove -> _state.update { s -> s.copy(liveShape = s.liveShape?.copy(end = intent.p)) }
            EditorIntent.ShapeEnd -> commitShape()

            is EditorIntent.AddText -> addText(intent.p)
            is EditorIntent.EditText -> editText(intent.id, intent.value)

            is EditorIntent.AddStamp -> addStamp(intent.source, intent.center)
            is EditorIntent.MoveStamp -> transformStamp(intent.id) { it.copy(center = intent.center) }
            is EditorIntent.TransformStamp -> transformStamp(intent.id) {
                it.copy(scale = intent.scale, rotationDegrees = intent.rotation)
            }
            is EditorIntent.DropStamp -> emit(EditorEffect.SealPressHaptic)

            is EditorIntent.SelectAnnotation -> _state.update { it.copy(selectedAnnotationId = intent.id) }
            is EditorIntent.DeleteAnnotation -> mutatePage { it.remove(intent.id) }

            is EditorIntent.AddPage -> addPage(intent.imageUri)
            is EditorIntent.RemovePage -> removePage(intent.id)
            is EditorIntent.ReorderPages -> _state.update { it.copy(document = it.document.reordered(intent.from, intent.to)) }

            EditorIntent.RequestPasswordDialog -> _state.update { it.copy(showPasswordDialog = true) }
            EditorIntent.DismissPasswordDialog -> _state.update { it.copy(showPasswordDialog = false) }
            is EditorIntent.SetPassword -> setPassword(intent.password)
            EditorIntent.RemovePassword -> removePassword()
            EditorIntent.Export -> export()
        }
    }

    private fun loadPages(uris: List<Uri>, title: String, documentId: String? = null) {
        val pages = uris.map { EditablePage(id = UUID.randomUUID().toString(), imageUri = it) }
        _state.update {
            it.copy(
                document = EditableDocument(
                    id = documentId ?: UUID.randomUUID().toString(),
                    title = title,
                    pages = pages
                ),
                currentPage = 0
            )
        }
    }

    private fun commitInk() {
        val s = _state.value
        val pts = s.liveInk
        if (pts.size < 2) { _state.update { it.copy(liveInk = emptyList()) }; return }
        val page = s.page ?: return
        val ink = Annotation.Ink(
            id = UUID.randomUUID().toString(),
            z = page.topZ,
            points = pts,
            colorArgb = s.colorArgb,
            strokeWidth = s.strokeWidth,
            isHighlighter = s.tool == EditorTool.HIGHLIGHTER
        )
        _state.update { it.copy(liveInk = emptyList(), document = it.document.replacePage(page.upsert(ink))) }
    }

    private fun startShape(p: androidx.compose.ui.geometry.Offset) {
        val s = _state.value
        val page = s.page ?: return
        _state.update {
            it.copy(
                liveShape = Annotation.Shape(
                    id = UUID.randomUUID().toString(),
                    z = page.topZ,
                    kind = s.shapeKind,
                    start = p,
                    end = p,
                    colorArgb = s.colorArgb,
                    strokeWidth = s.strokeWidth
                )
            )
        }
    }

    private fun commitShape() {
        val s = _state.value
        val shape = s.liveShape ?: return
        val page = s.page ?: return
        _state.update { it.copy(liveShape = null, document = it.document.replacePage(page.upsert(shape))) }
    }

    private fun addText(p: androidx.compose.ui.geometry.Offset) {
        val s = _state.value
        val page = s.page ?: return
        val text = Annotation.Text(
            id = UUID.randomUUID().toString(),
            z = page.topZ,
            text = "",
            position = p,
            widthFraction = 0.5f,
            fontSizeSp = 4.5f,
            colorArgb = s.colorArgb
        )
        _state.update {
            it.copy(document = it.document.replacePage(page.upsert(text)), selectedAnnotationId = text.id)
        }
    }

    private fun editText(id: String, value: String) {
        mutatePage { page ->
            val existing = page.annotations.firstOrNull { it.id == id } as? Annotation.Text ?: return@mutatePage page
            page.upsert(existing.copy(text = value))
        }
    }

    private fun addStamp(source: com.fluidscan.pro.domain.model.StampSource, center: androidx.compose.ui.geometry.Offset) {
        val s = _state.value
        val page = s.page ?: return
        val stamp = Annotation.Stamp(
            id = UUID.randomUUID().toString(),
            z = page.topZ,
            source = source,
            center = center,
            scale = 1f,
            rotationDegrees = 0f
        )
        _state.update {
            it.copy(document = it.document.replacePage(page.upsert(stamp)), selectedAnnotationId = stamp.id)
        }
    }

    private fun transformStamp(id: String, transform: (Annotation.Stamp) -> Annotation.Stamp) {
        mutatePage { page ->
            val stamp = page.annotations.firstOrNull { it.id == id } as? Annotation.Stamp ?: return@mutatePage page
            page.upsert(transform(stamp))
        }
    }

    private fun addPage(uri: Uri) {
        val page = EditablePage(id = UUID.randomUUID().toString(), imageUri = uri)
        _state.update { it.copy(document = it.document.copy(pages = it.document.pages + page)) }
    }

    private fun removePage(id: String) {
        _state.update {
            val doc = it.document.removePage(id)
            it.copy(document = doc, currentPage = it.currentPage.coerceIn(0, (doc.pages.size - 1).coerceAtLeast(0)))
        }
    }

    private fun setPassword(pw: String) {
        password = pw.ifEmpty { null }
        _state.update {
            it.copy(
                document = it.document.copy(isPasswordProtected = password != null),
                showPasswordDialog = false
            )
        }
        emit(EditorEffect.LockHaptic)
    }

    private fun removePassword() {
        password = null
        _state.update { it.copy(document = it.document.copy(isPasswordProtected = false), showPasswordDialog = false) }
    }

    private fun export() {
        val doc = _state.value.document
        if (doc.pages.isEmpty()) { emit(EditorEffect.Error("Nothing to export")); return }
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val result = runCatching { buildPdf(doc) }
            _state.update { it.copy(isExporting = false) }
            result.onSuccess { uri ->
                persist(doc, uri.toString())
                emit(EditorEffect.Exported(uri))
            }.onFailure { emit(EditorEffect.Error("Export failed: ${it.message}")) }
        }
    }

    /** Save/update the document record so it appears in the dashboard (Phase 3). */
    private suspend fun persist(doc: EditableDocument, pdfUri: String) {
        documents.upsert(
            Document(
                id = doc.id,
                title = doc.title,
                pageUris = doc.pages.map { it.imageUri.toString() },
                pdfUri = pdfUri,
                thumbnailUri = doc.pages.firstOrNull()?.imageUri?.toString(),
                isPasswordProtected = doc.isPasswordProtected,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    private suspend fun buildPdf(doc: EditableDocument): Uri = withContext(dispatchers.io) {
        val bitmaps = doc.pages.map { flattener.flatten(it) }
        try {
            val outDir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val out = File(outDir, "${doc.title.ifBlank { "document" }}_${System.currentTimeMillis()}.pdf")
            pdfBuilder.build(bitmaps, out, password)
            out.toUri()
        } finally {
            bitmaps.forEach { it.recycle() }
        }
    }

    private inline fun mutatePage(transform: (EditablePage) -> EditablePage) {
        _state.update { s ->
            val page = s.page ?: return@update s
            s.copy(document = s.document.replacePage(transform(page)))
        }
    }

    private fun emit(effect: EditorEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
