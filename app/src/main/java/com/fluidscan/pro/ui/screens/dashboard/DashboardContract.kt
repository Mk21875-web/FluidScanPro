package com.fluidscan.pro.ui.screens.dashboard

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.fluidscan.pro.domain.model.Document

enum class ViewMode { GRID, LIST }

@Immutable
data class DashboardState(
    val documents: List<Document> = emptyList(),
    val query: String = "",
    val isSearchExpanded: Boolean = false,
    val viewMode: ViewMode = ViewMode.GRID,
    val expandedDocId: String? = null,
    val isLoading: Boolean = true
) {
    val expandedDocument: Document? get() = documents.firstOrNull { it.id == expandedDocId }
    val isEmpty: Boolean get() = !isLoading && documents.isEmpty()
}

sealed interface DashboardIntent {
    data class SetQuery(val query: String) : DashboardIntent
    data class SetSearchExpanded(val expanded: Boolean) : DashboardIntent
    data object ToggleViewMode : DashboardIntent
    data class ExpandDocument(val id: String?) : DashboardIntent
    data class OpenInEditor(val id: String) : DashboardIntent
    data class DeleteDocument(val id: String) : DashboardIntent
    data class SyncDocument(val id: String) : DashboardIntent
    data object NewScan : DashboardIntent
    data class ImportImages(val uris: List<Uri>) : DashboardIntent
}

sealed interface DashboardEffect {
    data object NavigateToScanner : DashboardEffect
    data class NavigateToEditor(val id: String) : DashboardEffect
    data class Message(val text: String) : DashboardEffect
}
