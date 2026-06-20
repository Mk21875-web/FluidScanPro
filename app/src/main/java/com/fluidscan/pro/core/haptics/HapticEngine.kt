package com.fluidscan.pro.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralised haptic vocabulary for the "alive" feel. Each method maps a UX moment to a
 * tuned vibration so the whole app speaks one tactile language.
 *
 * Falls back gracefully on devices without amplitude control / vibrator.
 */
@Singleton
class HapticEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(VibratorManager::class.java)
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val canVibrate: Boolean get() = vibrator?.hasVibrator() == true

    /** Light tick when the detected edge snaps to a new document boundary. */
    fun edgeSnap() = predefinedOrFallback(
        effectId = VibrationEffect.EFFECT_TICK,
        fallbackMs = 12,
        amplitude = 80
    )

    /** Firmer click when a manual crop handle magnetically locks onto a guide. */
    fun magneticLock() = predefinedOrFallback(
        effectId = VibrationEffect.EFFECT_CLICK,
        fallbackMs = 20,
        amplitude = 160
    )

    /** Crisp confirmation on a successful capture. */
    fun captureConfirm() = oneShot(durationMs = 30, amplitude = 200)

    /** Heavy "seal-press" thud for stamp/signature drop (used in Phase 2). */
    fun sealPress() {
        if (!canVibrate) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 18, 40, 55)
            val amplitudes = intArrayOf(0, 120, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            legacy(60)
        }
    }

    /** Success cue when QR/barcode reticle locks (Phase 5). */
    fun detectionSuccess() = predefinedOrFallback(
        effectId = VibrationEffect.EFFECT_HEAVY_CLICK,
        fallbackMs = 35,
        amplitude = 220
    )

    private fun oneShot(durationMs: Long, amplitude: Int) {
        if (!canVibrate) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            legacy(durationMs)
        }
    }

    private fun predefinedOrFallback(effectId: Int, fallbackMs: Long, amplitude: Int) {
        if (!canVibrate) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playPredefined(effectId)
        } else {
            oneShot(fallbackMs, amplitude)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun playPredefined(effectId: Int) {
        vibrator?.vibrate(VibrationEffect.createPredefined(effectId))
    }

    @Suppress("DEPRECATION")
    private fun legacy(durationMs: Long) = vibrator?.vibrate(durationMs)
}
