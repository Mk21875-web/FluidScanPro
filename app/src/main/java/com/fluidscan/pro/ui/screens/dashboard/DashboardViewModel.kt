package com.fluidscan.pro.ui.screens.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluidscan.pro.core.common.ScanHandoff
import com.fluidscan.pro.domain.model.SyncState
import com.fluidscan.pro.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val handoff: ScanHandoff
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val query = MutableStateFlow("")

    init {
        query
            .debounce(180)
            .distinctUntilChanged()
            .flatMapLatest { repository.observe(it) }
            .onEach { docs -> _state.update { it.copy(documents = docs, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.SetQuery -> {
                query.value = intent.query
                _state.update { it.copy(query = intent.query) }
            }
            is DashboardIntent.SetSearchExpanded -> {
                if (!intent.expanded) query.value = ""
                _state.update {
                    it.copy(isSearchExpanded = intent.expanded, query = if (intent.expanded) it.query else "")
                }
            }
            DashboardIntent.ToggleViewMode -> _state.update {
                it.copy(viewMode = if (it.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
            }
            is DashboardIntent.ExpandDocument -> _state.update { it.copy(expandedDocId = intent.id) }
            is DashboardIntent.OpenInEditor -> openInEditor(intent.id)
            is DashboardIntent.OpenOcr -> openOcr(intent.id)
            is DashboardIntent.DeleteDocument -> delete(intent.id)
            is DashboardIntent.SyncDocument -> sync(intent.id)
            DashboardIntent.NewScan -> emit(DashboardEffect.NavigateToScanner)
            is DashboardIntent.ImportImages -> importImages(intent.uris)
        }
    }

    private fun openInEditor(id: String) {
        viewModelScope.launch {
            val doc = repository.get(id) ?: return@launch emit(DashboardEffect.Message("Document not found"))
            handoff.set(doc.pageUris.map { Uri.parse(it) }, doc.title, documentId = doc.id)
            _state.update { it.copy(expandedDocId = null) }
            emit(DashboardEffect.NavigateToEditor(id))
        }
    }

    private fun openOcr(id: String) {
        viewModelScope.launch {
            val doc = repository.get(id) ?: return@launch emit(DashboardEffect.Message("Document not found"))
            handoff.set(doc.pageUris.map { Uri.parse(it) }, doc.title, documentId = doc.id)
            _state.update { it.copy(expandedDocId = null) }
            emit(DashboardEffect.NavigateToOcr(id))
        }
    }

    private fun importImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        handoff.set(uris, "Imported ${uris.size}p")
        emit(DashboardEffect.NavigateToEditor("import"))
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            _state.update { it.copy(expandedDocId = null) }
        }
    }

    /** Simulated cloud sync: LOCAL → SYNCING → SYNCED. The wave indicator tracks SYNCING. */
    private fun sync(id: String) {
        viewModelScope.launch {
            repository.setSyncState(id, SyncState.SYNCING)
            delay(2200)
            repository.setSyncState(id, SyncState.SYNCED)
        }
    }

    private fun emit(effect: DashboardEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
