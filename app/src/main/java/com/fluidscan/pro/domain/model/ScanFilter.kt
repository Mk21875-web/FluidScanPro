package com.fluidscan.pro.domain.model

/**
 * Live document filters. Each maps to a deterministic pixel transform in
 * `service.scan.ImageFilters`. The order here is the order shown in the
 * cross-fade filter carousel.
 */
enum class ScanFilter(val displayName: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    GRAYSCALE("Grayscale"),
    BLACK_WHITE("B&W"),
    LIGHTEN("Lighten");

    companion object {
        val Default = ORIGINAL
    }
}
