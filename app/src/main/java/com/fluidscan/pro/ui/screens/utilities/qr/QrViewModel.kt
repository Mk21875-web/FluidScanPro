package com.fluidscan.pro.ui.screens.utilities.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(QrState())
    val state = _state.asStateFlow()

    private val _effects = Channel<QrEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: QrIntent) {
        when (intent) {
            is QrIntent.Detected -> {
                // Lock onto the first detection; ignore further frames until reset.
                if (_state.value.detected) return
                _state.update { it.copy(result = intent.result) }
                emit(QrEffect.DetectionHaptic)
            }
            QrIntent.Reset -> _state.update { it.copy(result = null) }
        }
    }

    private fun emit(effect: QrEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
