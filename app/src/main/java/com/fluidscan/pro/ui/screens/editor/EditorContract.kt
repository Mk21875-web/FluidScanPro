package com.fluidscan.pro.ui.screens.editor

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.fluidscan.pro.domain.model.Annotation
import com.fluidscan.pro.domain.model.EditableDocument
import com.fluidscan.pro.domain.model.ShapeKind
import com.fluidscan.pro.domain.model.StampSource

/** Active annotation tool. */
enum class EditorTool { PAN, PEN, HIGHLIGHTER, SHAPE, TEXT, STAMP, ERASER }

@Immutable
data class EditorState(
    val document: EditableDocument = EditableDocument(id = "", title = "Untitled"),
    val currentPage: Int = 0,
    val tool: EditorTool = EditorTool.PAN,
    val colorArgb: Long = 0xFF2D6CFF,
    val strokeWidth: Float = 0.006f,
    val shapeKind: ShapeKind = ShapeKind.RECTANGLE,
    /** Live, un-committed pen samples (normalized) for the current stroke. */
    val liveInk: List<Offset> = emptyList(),
    /** Live shape being dragged out (drives the spring preview). */
    val liveShape: Annotation.Shape? = null,
    val selectedAnnotationId: String? = null,
    val showPasswordDialog: Boolean = false,
    val isExporting: Boolean = false
) {
    val page get() = document.pages.getOrNull(currentPage)
    val pageCount get() = document.pages.size
}

sealed interface EditorIntent {
    data class LoadPages(val imageUris: List<Uri>, val title: String) : EditorIntent
    /** Pull the just-scanned pages from the scanner→editor handoff. */
    data object LoadFromScan : EditorIntent
    data class SelectTool(val tool: EditorTool) : EditorIntent
    data class SelectColor(val argb: Long) : EditorIntent
    data class SetStrokeWidth(val width: Float) : EditorIntent
    data class SelectShapeKind(val kind: ShapeKind) : EditorIntent
    data class SelectPage(val index: Int) : EditorIntent

    // Pen
    data class InkStart(val p: Offset) : EditorIntent
    data class InkMove(val p: Offset) : EditorIntent
    data object InkEnd : EditorIntent

    // Shapes
    data class ShapeStart(val p: Offset) : EditorIntent
    data class ShapeMove(val p: Offset) : EditorIntent
    data object ShapeEnd : EditorIntent

    // Text
    data class AddText(val p: Offset) : EditorIntent
    data class EditText(val id: String, val value: String) : EditorIntent

    // Stamp / signature
    data class AddStamp(val source: StampSource, val center: Offset) : EditorIntent
    data class MoveStamp(val id: String, val center: Offset) : EditorIntent
    data class TransformStamp(val id: String, val scale: Float, val rotation: Float) : EditorIntent
    /** Finger lifted after placing a stamp → triggers the seal-press. */
    data class DropStamp(val id: String) : EditorIntent

    data class SelectAnnotation(val id: String?) : EditorIntent
    data class DeleteAnnotation(val id: String) : EditorIntent

    // Page management
    data class AddPage(val imageUri: Uri) : EditorIntent
    data class RemovePage(val id: String) : EditorIntent
    data class ReorderPages(val from: Int, val to: Int) : EditorIntent

    // Security & export
    data object RequestPasswordDialog : EditorIntent
    data object DismissPasswordDialog : EditorIntent
    data class SetPassword(val password: String) : EditorIntent
    data object RemovePassword : EditorIntent
    data object Export : EditorIntent
}

sealed interface EditorEffect {
    data object SealPressHaptic : EditorEffect
    data object LockHaptic : EditorEffect
    data class Exported(val uri: Uri) : EditorEffect
    data class Error(val message: String) : EditorEffect
}
