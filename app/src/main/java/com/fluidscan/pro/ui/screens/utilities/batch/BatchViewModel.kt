package com.fluidscan.pro.ui.screens.utilities.batch

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluidscan.pro.domain.model.BatchItem
import com.fluidscan.pro.domain.model.BatchJob
import com.fluidscan.pro.service.utility.BatchProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val processor: BatchProcessor
) : ViewModel() {

    private val _state = MutableStateFlow(BatchState())
    val state = _state.asStateFlow()

    private val _effects = Channel<BatchEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: BatchIntent) {
        when (intent) {
            is BatchIntent.AddImages -> addImages(intent.uris)
            BatchIntent.Start -> start()
            BatchIntent.Clear -> _state.update { it.copy(job = BatchJob()) }
            BatchIntent.Share -> share()
        }
    }

    private fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val items = uris.mapIndexed { i, uri ->
            BatchItem(id = UUID.randomUUID().toString(), sourceUri = uri, label = "Page ${i + 1}")
        }
        _state.update { it.copy(job = it.job.copy(items = it.job.items + items, outputPdfUri = null)) }
    }

    private fun start() {
        val job = _state.value.job
        if (job.isRunning || job.items.isEmpty()) return
        _state.update { it.copy(job = it.job.copy(isRunning = true, outputPdfUri = null)) }
        processor.run(job.items)
            .onEach { progress ->
                _state.update {
                    it.copy(
                        job = it.job.copy(
                            items = progress.items,
                            outputPdfUri = progress.outputPdfUri ?: it.job.outputPdfUri,
                            isRunning = progress.outputPdfUri == null
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun share() {
        val uri = _state.value.job.outputPdfUri
            ?: return emit(BatchEffect.Message("Run the batch first"))
        emit(BatchEffect.SharePdf(uri))
    }

    private fun emit(effect: BatchEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
