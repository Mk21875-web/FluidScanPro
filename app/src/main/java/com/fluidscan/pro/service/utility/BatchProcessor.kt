package com.fluidscan.pro.service.utility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import com.fluidscan.pro.core.common.DispatcherProvider
import com.fluidscan.pro.domain.model.BatchItem
import com.fluidscan.pro.domain.model.BatchStage
import com.fluidscan.pro.service.ocr.AiCleanup
import com.fluidscan.pro.service.ocr.OcrService
import com.fluidscan.pro.service.pdf.PdfBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** A single progress snapshot of the batch assembly line. */
data class BatchProgress(
    val items: List<BatchItem>,
    val outputPdfUri: Uri? = null
)

/**
 * Runs the batch "assembly line": each item is cleaned → recognized → packaged, advancing one
 * stage at a time and emitting a fresh snapshot per transition so the UI can animate items
 * moving down the line. The final emission carries the assembled PDF. All work is on IO.
 */
@Singleton
class BatchProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val cleanup: AiCleanup,
    private val ocr: OcrService,
    private val pdfBuilder: PdfBuilder
) {
    fun run(initial: List<BatchItem>): Flow<BatchProgress> = flow {
        var items = initial.map { it.copy(stage = BatchStage.QUEUED) }
        emit(BatchProgress(items))

        val cleanedBitmaps = mutableListOf<Bitmap>()
        items.forEachIndexed { index, item ->
            items = items.update(index, BatchStage.CLEANING)
            emit(BatchProgress(items))
            val cleanedUri = runCatching { cleanup.cleanup(item.sourceUri) }.getOrDefault(item.sourceUri)

            items = items.update(index, BatchStage.RECOGNIZING)
            emit(BatchProgress(items))
            runCatching { ocr.recognize(cleanedUri) }

            items = items.update(index, BatchStage.PACKAGING)
            emit(BatchProgress(items))
            decode(cleanedUri)?.let { cleanedBitmaps.add(it) }

            items = items.update(index, BatchStage.DONE)
            emit(BatchProgress(items))
        }

        val output = File(context.cacheDir, "batch_${System.currentTimeMillis()}.pdf")
        val pdf = runCatching { pdfBuilder.build(cleanedBitmaps, output) }.getOrNull()
        cleanedBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        emit(BatchProgress(items, outputPdfUri = pdf?.toUri()))
    }.flowOn(dispatchers.io)

    private fun List<BatchItem>.update(index: Int, stage: BatchStage): List<BatchItem> =
        mapIndexed { i, item -> if (i == index) item.copy(stage = stage) else item }

    private fun decode(uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()
}
