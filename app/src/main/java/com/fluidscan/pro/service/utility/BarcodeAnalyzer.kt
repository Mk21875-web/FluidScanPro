package com.fluidscan.pro.service.utility

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.fluidscan.pro.domain.model.BarcodeKind
import com.fluidscan.pro.domain.model.BarcodeResult
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX [ImageAnalysis.Analyzer] that decodes QR/barcodes per frame via ML Kit and reports
 * the first detection through [onResult]. The scanner is configured for all common formats.
 */
class BarcodeAnalyzer(
    private val onResult: (BarcodeResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.let { onResult(it.toDomain()) }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

private fun Barcode.toDomain(): BarcodeResult = BarcodeResult(
    rawValue = rawValue.orEmpty(),
    format = formatName(format),
    kind = when (valueType) {
        Barcode.TYPE_URL -> BarcodeKind.URL
        Barcode.TYPE_WIFI -> BarcodeKind.WIFI
        Barcode.TYPE_CONTACT_INFO -> BarcodeKind.CONTACT
        Barcode.TYPE_PHONE -> BarcodeKind.PHONE
        Barcode.TYPE_EMAIL -> BarcodeKind.EMAIL
        Barcode.TYPE_GEO -> BarcodeKind.GEO
        Barcode.TYPE_PRODUCT -> BarcodeKind.PRODUCT
        Barcode.TYPE_TEXT -> BarcodeKind.TEXT
        else -> BarcodeKind.OTHER
    }
)

private fun formatName(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR_CODE"
    Barcode.FORMAT_AZTEC -> "AZTEC"
    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_EAN_13 -> "EAN_13"
    Barcode.FORMAT_EAN_8 -> "EAN_8"
    Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"
    Barcode.FORMAT_CODE_128 -> "CODE_128"
    Barcode.FORMAT_CODE_39 -> "CODE_39"
    Barcode.FORMAT_CODE_93 -> "CODE_93"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_ITF -> "ITF"
    else -> "BARCODE"
}
