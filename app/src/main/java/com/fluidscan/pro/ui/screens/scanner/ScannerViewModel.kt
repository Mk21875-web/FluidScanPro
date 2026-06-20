package com.fluidscan.pro.ui.screens.scanner

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluidscan.pro.core.common.DispatcherProvider
import com.fluidscan.pro.domain.model.Quadrilateral
import com.fluidscan.pro.domain.model.ScanFilter
import com.fluidscan.pro.domain.model.ScanPage
import com.fluidscan.pro.core.util.ImageProcessing
import com.fluidscan.pro.service.scan.EdgeDetector
import com.fluidscan.pro.service.scan.EdgeResult
import com.fluidscan.pro.service.scan.ImageFilters
import com.fluidscan.pro.service.scan.PerspectiveTransformer
import com.fluidscan.pro.service.scan.ScanFileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val fileStore: ScanFileStore,
    private val perspective: PerspectiveTransformer,
    private val filters: ImageFilters,
    val edgeDetector: EdgeDetector
) : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    private val _effects = Channel<ScannerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Tracks whether the previous frame had a confident document, to fire snap haptic on the rising edge only. */
    private var lastHadDocument = false

    fun onIntent(intent: ScannerIntent) {
        when (intent) {
            is ScannerIntent.EdgeDetected -> onEdge(intent.result)
            ScannerIntent.CaptureRequested -> onCaptureRequested()
            is ScannerIntent.CapturedRawFile -> onRawCaptured(intent.absolutePath, intent.rotationDegrees)
            is ScannerIntent.CaptureFailed -> emit(ScannerEffect.Error(intent.message)).also {
                _state.update { it.copy(isCapturing = false) }
            }
            ScannerIntent.ToggleFlash -> _state.update { it.copy(flashEnabled = !it.flashEnabled) }
            ScannerIntent.ToggleAutoCapture -> _state.update { it.copy(autoCapture = !it.autoCapture) }
            is ScannerIntent.SelectFilter -> _state.update { it.copy(selectedFilter = intent.filter) }
            is ScannerIntent.RemovePage -> removePage(intent.id)
            is ScannerIntent.ReorderPages -> reorder(intent.fromIndex, intent.toIndex)
            is ScannerIntent.OpenCrop -> openCrop(intent.id)
            is ScannerIntent.CropCornerMoved -> moveCropCorner(intent.cornerIndex, intent.normalized)
            ScannerIntent.CropConfirmed -> confirmCrop()
            ScannerIntent.CropDismissed -> _state.update { it.copy(editing = null) }
            ScannerIntent.FinishSession -> finish()
        }
    }

    private fun onEdge(result: EdgeResult) {
        _state.update { it.copy(liveEdge = result) }
        val now = result.hasDocument
        if (now && !lastHadDocument) emit(ScannerEffect.EdgeSnapHaptic)
        lastHadDocument = now
    }

    private fun onCaptureRequested() {
        if (_state.value.isCapturing) return
        _state.update { it.copy(isCapturing = true) }
        val target = fileStore.newRawFile()
        emit(ScannerEffect.CaptureHaptic)
        emit(ScannerEffect.TakePicture(target.absolutePath))
    }

    private fun onRawCaptured(rawPath: String, rotationDegrees: Int) {
        val quad = _state.value.liveEdge.quad ?: Quadrilateral.FULL
        val filter = _state.value.selectedFilter
        _state.update { it.copy(isCapturing = false, isProcessing = true) }

        viewModelScope.launch {
            val page = runCatching {
                processCapture(File(rawPath), rotationDegrees, quad, filter)
            }.getOrElse {
                emit(ScannerEffect.Error("Couldn't process page: ${it.message}"))
                null
            }
            _state.update { s ->
                if (page == null) s.copy(isProcessing = false)
                else s.copy(isProcessing = false, capturedPages = s.capturedPages + page)
            }
        }
    }

    /** All heavy bitmap work runs on [DispatcherProvider.io]; bitmaps are recycled eagerly. */
    private suspend fun processCapture(
        rawFile: File,
        rotationDegrees: Int,
        quad: Quadrilateral,
        filter: ScanFilter
    ): ScanPage = withContext(dispatchers.io) {
        val decoded = ImageProcessing.decodeSampled(rawFile)
        val rotated = ImageProcessing.rotate(decoded, rotationDegrees)
        val warped = perspective.correct(rotated, quad)
        rotated.recycle()
        val filtered = filters.apply(warped, filter)
        warped.recycle()
        val processedFile = fileStore.newProcessedFile()
        ImageProcessing.saveJpeg(filtered, processedFile)
        filtered.recycle()
        ScanPage(
            id = UUID.randomUUID().toString(),
            rawUri = rawFile.toUri(),
            processedUri = processedFile.toUri(),
            cropQuad = quad,
            filter = filter
        )
    }

    private fun removePage(id: String) {
        val page = _state.value.capturedPages.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(capturedPages = it.capturedPages.filterNot { p -> p.id == id }) }
        viewModelScope.launch(dispatchers.io) {
            fileStore.delete(page.rawUri.toFile(), page.processedUri?.toFile())
        }
    }

    private fun reorder(from: Int, to: Int) {
        _state.update { s ->
            val pages = s.capturedPages
            if (from !in pages.indices || to !in pages.indices) return@update s
            val mutable = pages.toMutableList()
            mutable.add(to, mutable.removeAt(from))
            s.copy(capturedPages = mutable)
        }
    }

    private fun openCrop(id: String) {
        val page = _state.value.capturedPages.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(editing = page, editingQuad = page.cropQuad) }
    }

    /**
     * Magnetic snap: while dragging a corner, if it lands within [SNAP_THRESHOLD] of a
     * guide (frame edge, or the x/y of an adjacent corner) it locks onto it and fires a
     * crisp haptic — the satisfying "click into place" feel.
     */
    private fun moveCropCorner(index: Int, raw: androidx.compose.ui.geometry.Offset) {
        val current = _state.value.editingQuad
        var x = raw.x.coerceIn(0f, 1f)
        var y = raw.y.coerceIn(0f, 1f)
        var locked = false

        val guidesX = listOf(0f, 1f) + current.corners.filterIndexed { i, _ -> i != index }.map { it.x }
        val guidesY = listOf(0f, 1f) + current.corners.filterIndexed { i, _ -> i != index }.map { it.y }

        guidesX.firstOrNull { abs(it - x) < SNAP_THRESHOLD }?.let { x = it; locked = true }
        guidesY.firstOrNull { abs(it - y) < SNAP_THRESHOLD }?.let { y = it; locked = true }

        val snapped = current.withCorner(index, androidx.compose.ui.geometry.Offset(x, y))
        val wasLocked = cornerWasLocked
        _state.update { it.copy(editingQuad = snapped) }
        if (locked && !wasLocked) emit(ScannerEffect.MagneticLockHaptic)
        cornerWasLocked = locked
    }

    private var cornerWasLocked = false

    private fun confirmCrop() {
        val editing = _state.value.editing ?: return
        val quad = _state.value.editingQuad
        val filter = editing.filter
        _state.update { it.copy(editing = null, isProcessing = true) }
        viewModelScope.launch {
            val updated = runCatching {
                reprocess(editing, quad, filter)
            }.getOrElse {
                emit(ScannerEffect.Error("Couldn't re-crop: ${it.message}"))
                null
            }
            _state.update { s ->
                if (updated == null) s.copy(isProcessing = false)
                else s.copy(
                    isProcessing = false,
                    capturedPages = s.capturedPages.map { if (it.id == updated.id) updated else it }
                )
            }
        }
    }

    private suspend fun reprocess(page: ScanPage, quad: Quadrilateral, filter: ScanFilter): ScanPage =
        withContext(dispatchers.io) {
            val rawFile = page.rawUri.toFile()
            val decoded = ImageProcessing.decodeSampled(rawFile)
            val warped = perspective.correct(decoded, quad)
            decoded.recycle()
            val filtered = filters.apply(warped, filter)
            warped.recycle()
            page.processedUri?.toFile()?.let { fileStore.delete(it) }
            val out = fileStore.newProcessedFile()
            ImageProcessing.saveJpeg(filtered, out)
            filtered.recycle()
            page.copy(processedUri = out.toUri(), cropQuad = quad, filter = filter)
        }

    private fun finish() {
        val ids = _state.value.capturedPages.map { it.id }
        if (ids.isEmpty()) {
            emit(ScannerEffect.Error("Capture at least one page first."))
        } else {
            emit(ScannerEffect.NavigateToReview(ids))
        }
    }

    private fun emit(effect: ScannerEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun Uri.toFile(): File = File(requireNotNull(path) { "Uri has no path: $this" })

    private companion object {
        const val SNAP_THRESHOLD = 0.025f
    }
}
