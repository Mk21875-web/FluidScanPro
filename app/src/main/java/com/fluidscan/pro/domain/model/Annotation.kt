package com.fluidscan.pro.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * An overlay placed on top of a page. All geometry is in **normalized** page coordinates
 * (0..1) so annotations survive zoom, rotation, and the eventual PDF render at any DPI.
 */
@Immutable
sealed interface Annotation {
    val id: String
    /** Draw order; higher is on top. */
    val z: Int

    /** Freehand pen stroke. [points] are the raw samples; rendering smooths them (Bézier). */
    @Immutable
    data class Ink(
        override val id: String,
        override val z: Int,
        val points: List<Offset>,
        val colorArgb: Long,
        val strokeWidth: Float,
        val isHighlighter: Boolean = false
    ) : Annotation

    /** Vector shape with a live spring-preview while being drawn. */
    @Immutable
    data class Shape(
        override val id: String,
        override val z: Int,
        val kind: ShapeKind,
        val start: Offset,
        val end: Offset,
        val colorArgb: Long,
        val strokeWidth: Float,
        val filled: Boolean = false
    ) : Annotation

    /** Text box; [position] is the top-left anchor. */
    @Immutable
    data class Text(
        override val id: String,
        override val z: Int,
        val text: String,
        val position: Offset,
        val widthFraction: Float,
        val fontSizeSp: Float,
        val colorArgb: Long
    ) : Annotation

    /** A signature or stamp image, freely transformed (parallax drag, scale, rotate). */
    @Immutable
    data class Stamp(
        override val id: String,
        override val z: Int,
        val source: StampSource,
        val center: Offset,
        val scale: Float,
        val rotationDegrees: Float
    ) : Annotation
}

enum class ShapeKind { RECTANGLE, OVAL, LINE, ARROW }

/** Where a stamp's pixels come from. */
@Immutable
sealed interface StampSource {
    /** Persisted signature/stamp image on disk. */
    data class Image(val uri: String) : StampSource
    /** Built-in vector stamp (e.g. APPROVED / CONFIDENTIAL) rendered by the app. */
    data class Builtin(val label: String, val tintArgb: Long) : StampSource
}
