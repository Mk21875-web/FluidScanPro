package com.fluidscan.pro.ui.screens.utilities.batch

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.fluidscan.pro.domain.model.BatchJob

@Immutable
data class BatchState(
    val job: BatchJob = BatchJob()
)

sealed interface BatchIntent {
    data class AddImages(val uris: List<Uri>) : BatchIntent
    data object Start : BatchIntent
    data object Clear : BatchIntent
    data object Share : BatchIntent
}

sealed interface BatchEffect {
    data class SharePdf(val uri: Uri) : BatchEffect
    data class Message(val text: String) : BatchEffect
}
