package com.fluidscan.pro.service.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.core.net.toUri
import com.fluidscan.pro.core.common.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "AI Cleanup": desaturates and boosts contrast/brightness to flatten shadows and whiten
 * paper — the document-scanner "magic" look. Runs on IO and recycles intermediates eagerly.
 */
@Singleton
class AiCleanup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) {
    suspend fun cleanup(source: Uri, contrast: Float = 1.35f, brightness: Float = 18f): Uri =
        withContext(dispatchers.io) {
            val src = decode(source) ?: error("Could not read image")
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val matrix = ColorMatrix().apply {
                setSaturation(0f)
                postConcat(ColorMatrix(contrastBrightnessArray(contrast, brightness)))
            }
            Canvas(out).drawBitmap(
                src,
                0f,
                0f,
                Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
            )
            src.recycle()

            val file = File(context.cacheDir, "cleanup_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            out.recycle()
            file.toUri()
        }

    private fun decode(uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it)
        }

    private fun contrastBrightnessArray(c: Float, b: Float): FloatArray {
        val t = (-0.5f * c + 0.5f) * 255f + b
        return floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
