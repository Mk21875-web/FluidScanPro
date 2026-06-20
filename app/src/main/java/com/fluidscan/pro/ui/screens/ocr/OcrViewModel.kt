package com.fluidscan.pro.ui.screens.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluidscan.pro.core.common.ScanHandoff
import com.fluidscan.pro.domain.model.OcrResult
import com.fluidscan.pro.service.ocr.AiCleanup
import com.fluidscan.pro.service.ocr.OcrService
import com.fluidscan.pro.service.ocr.SmartNamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val handoff: ScanHandoff,
    private val ocr: OcrService,
    private val cleanup: AiCleanup,
    private val namer: SmartNamer
) : ViewModel() {

    private val _state = MutableStateFlow(OcrState())
    val state = _state.asStateFlow()

    private val _effects = Channel<OcrEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: OcrIntent) {
        when (intent) {
            is OcrIntent.LoadPages -> _state.update {
                it.copy(pages = intent.uris, title = intent.title, currentPage = 0, result = OcrResult.EMPTY)
            }
            OcrIntent.LoadFromScan -> _state.update {
                it.copy(pages = handoff.pageImageUris, title = handoff.title, currentPage = 0)
            }
            is OcrIntent.SelectPage -> _state.update {
                it.copy(currentPage = intent.index, result = OcrResult.EMPTY)
            }
            is OcrIntent.SetScript -> _state.update { it.copy(script = intent.script) }
            OcrIntent.RunOcr -> runOcr()
            OcrIntent.RunCleanup -> runCleanup()
            OcrIntent.GenerateName -> generateName()
        }
    }

    private fun runOcr() {
        val uri = _state.value.currentUri ?: return emit(OcrEffect.Error("No page to scan"))
        if (_state.value.isRecognizing) return
        _state.update { it.copy(isRecognizing = true, result = OcrResult.EMPTY) }
        viewModelScope.launch {
            val result = runCatching { ocr.recognize(uri, _state.value.script) }
            _state.update { it.copy(isRecognizing = false) }
            result.onSuccess { res ->
                _state.update { it.copy(result = res) }
                emit(OcrEffect.TextRecognized(res.lines.size))
            }.onFailure { emit(OcrEffect.Error("OCR failed: ${it.message}")) }
        }
    }

    private fun runCleanup() {
        val page = _state.value.currentPage
        val uri = _state.value.pages.getOrNull(page) ?: return
        if (_state.value.isCleaning) return
        _state.update { it.copy(isCleaning = true) }
        viewModelScope.launch {
            val result = runCatching { cleanup.cleanup(uri) }
            _state.update { it.copy(isCleaning = false) }
            result.onSuccess { cleaned ->
                _state.update { it.copy(cleanedPages = it.cleanedPages + (page to cleaned)) }
                emit(OcrEffect.CleanupDone)
            }.onFailure { emit(OcrEffect.Error("Cleanup failed: ${it.message}")) }
        }
    }

    private fun generateName() {
        val res = _state.value.result
        if (res.fullText.isBlank()) return emit(OcrEffect.Error("Run OCR first"))
        val name = namer.suggest(res)
        _state.update { it.copy(suggestedName = name) }
        emit(OcrEffect.NameSuggested(name))
    }

    private fun emit(effect: OcrEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    override fun onCleared() {
        ocr.close()
        super.onCleared()
    }
}
