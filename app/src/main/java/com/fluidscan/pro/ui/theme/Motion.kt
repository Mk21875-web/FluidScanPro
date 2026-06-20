package com.fluidscan.pro.ui.theme

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * FluidScanMotion — the single source of truth for the app's "Ultra-Fluid" feel.
 *
 * Every animated interaction in FluidScan Pro pulls its physics from here so that
 * the motion language stays consistent (and tunable) across the entire app.
 *
 * Design contract:
 *  - Use [Springs] for anything physical/interruptible (drag, snap, overshoot, gestures).
 *  - Use [Easings] + [Durations] (tween) for choreographed, deterministic transitions
 *    (cross-fades, morphs, screen enters).
 *  - Target frame budget: 90fps capable (≈11ms/frame). Keep work off the UI thread.
 */
object FluidScanMotion {

    /** Hand-tuned spring specs. DampingRatio 0.6f = lively overshoot without ringing. */
    object Springs {
        const val DampingRatioOvershoot = 0.6f          // the signature "snap" bounce
        const val DampingRatioGentle = 0.85f            // settle with minimal overshoot
        const val StiffnessSnappy = 1400f               // edge-detect border snap
        const val StiffnessStandard = Spring.StiffnessMedium // 1500f
        const val StiffnessSoft = Spring.StiffnessLow        // 200f

        /** Edge-detection border / magnetic crop snap — bouncy, immediate. */
        fun <T> snap(): SpringSpec<T> = spring(
            dampingRatio = DampingRatioOvershoot,
            stiffness = StiffnessSnappy
        )

        /** Card lift / drag-and-drop reorder — gentle settle under the finger. */
        fun <T> lift(): SpringSpec<T> = spring(
            dampingRatio = DampingRatioGentle,
            stiffness = StiffnessStandard
        )

        /** Sheet / container expansion — soft, premium glide. */
        fun <T> expand(): SpringSpec<T> = spring(
            dampingRatio = DampingRatioGentle,
            stiffness = StiffnessSoft
        )

        // Visibility-threshold-aware specs for pixel/size based animations (avoids sub-pixel jitter).
        val OffsetSpring: SpringSpec<IntOffset> = spring(
            dampingRatio = DampingRatioOvershoot,
            stiffness = StiffnessStandard,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
        val SizeSpring: SpringSpec<IntSize> = spring(
            dampingRatio = DampingRatioGentle,
            stiffness = StiffnessStandard,
            visibilityThreshold = IntSize.VisibilityThreshold
        )
        val DpSpring: SpringSpec<Dp> = spring(
            dampingRatio = DampingRatioGentle,
            stiffness = StiffnessStandard,
            visibilityThreshold = Dp.VisibilityThreshold
        )
    }

    /** Strict easing curves (M3 emphasized set + classic FastOutSlowIn). */
    object Easings {
        val FastOutSlowIn: Easing = FastOutSlowInEasing               // standard
        val LinearOutSlowIn: Easing = LinearOutSlowInEasing           // enter
        val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
        val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
        val Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        val Anticipate: Easing = CubicBezierEasing(0.36f, 0.0f, 0.66f, -0.56f) // wind-up
    }

    /** Canonical durations (ms). Pair with [Easings] in `tween`. */
    object Durations {
        const val Quick = 150
        const val Standard = 300
        const val Emphasized = 450
        const val Elaborate = 600        // morphs, shared-element transitions
        const val Infinite = AnimationConstants.DefaultDurationMillis
    }

    // Convenience tweens used widely (cross-fade wipes, morphs).
    fun <T> standardTween(durationMillis: Int = Durations.Standard) =
        tween<T>(durationMillis = durationMillis, easing = Easings.FastOutSlowIn)

    fun <T> emphasizedEnter(durationMillis: Int = Durations.Emphasized) =
        tween<T>(durationMillis = durationMillis, easing = Easings.EmphasizedDecelerate)

    fun <T> emphasizedExit(durationMillis: Int = Durations.Quick) =
        tween<T>(durationMillis = durationMillis, easing = Easings.EmphasizedAccelerate)
}
