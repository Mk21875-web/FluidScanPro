package com.fluidscan.pro.service.ocr

import android.content.Context
import android.net.Uri
import com.fluidscan.pro.core.common.DispatcherProvider
import com.fluidscan.pro.domain.model.NormRect
import com.fluidscan.pro.domain.model.OcrLine
import com.fluidscan.pro.domain.model.OcrResult
import com.fluidscan.pro.domain.model.OcrScript
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline OCR via ML Kit Text Recognition v2. One recognizer per script family (Latin/Chinese/
 * Devanagari/Japanese/Korean) covers 100+ languages. Recognizers are cached and reused;
 * all work runs on [DispatcherProvider.io].
 */
@Singleton
class OcrService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    private val recognizers = ConcurrentHashMap<OcrScript, TextRecognizer>()

    private fun recognizer(script: OcrScript): TextRecognizer = recognizers.getOrPut(script) {
        val options = when (script) {
            OcrScript.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
            OcrScript.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            OcrScript.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            OcrScript.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            OcrScript.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
        }
        TextRecognition.getClient(options)
    }

    suspend fun recognize(uri: Uri, script: OcrScript = OcrScript.LATIN): OcrResult =
        withContext(dispatchers.io) {
            val image = InputImage.fromFilePath(context, uri)
            val w = image.width.toFloat().coerceAtLeast(1f)
            val h = image.height.toFloat().coerceAtLeast(1f)
            val visionText = recognizer(script).process(image).await()

            val lines = visionText.textBlocks
                .asSequence()
                .flatMap { it.lines.asSequence() }
                .mapNotNull { line ->
                    val b = line.boundingBox ?: return@mapNotNull null
                    OcrLine(
                        text = line.text,
                        box = NormRect(
                            left = (b.left / w).coerceIn(0f, 1f),
                            top = (b.top / h).coerceIn(0f, 1f),
                            right = (b.right / w).coerceIn(0f, 1f),
                            bottom = (b.bottom / h).coerceIn(0f, 1f)
                        )
                    )
                }
                .toList()

            OcrResult(fullText = visionText.text, lines = lines, script = script)
        }

    /** Release native recognizers. Call when the OCR feature is torn down. */
    fun close() {
        recognizers.values.forEach { it.close() }
        recognizers.clear()
    }
}
