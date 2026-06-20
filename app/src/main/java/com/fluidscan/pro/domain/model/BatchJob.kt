package com.fluidscan.pro.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable

/** Stages every item flows through on the batch "assembly line". */
enum class BatchStage { QUEUED, CLEANING, RECOGNIZING, PACKAGING, DONE }

@Immutable
data class BatchItem(
    val id: String,
    val sourceUri: Uri,
    val label: String,
    val stage: BatchStage = BatchStage.QUEUED
)

@Immutable
data class BatchJob(
    val items: List<BatchItem> = emptyList(),
    val isRunning: Boolean = false,
    val outputPdfUri: Uri? = null
) {
    val progress: Float
        get() = if (items.isEmpty()) 0f
        else items.count { it.stage == BatchStage.DONE }.toFloat() / items.size
}
