package com.fluidscan.pro.domain.model

import androidx.compose.runtime.Immutable

/** A detected QR/barcode. [format] is a human label (e.g. "QR_CODE"); [kind] groups by payload. */
@Immutable
data class BarcodeResult(
    val rawValue: String,
    val format: String,
    val kind: BarcodeKind
)

enum class BarcodeKind { URL, TEXT, WIFI, CONTACT, PHONE, EMAIL, GEO, PRODUCT, OTHER }
