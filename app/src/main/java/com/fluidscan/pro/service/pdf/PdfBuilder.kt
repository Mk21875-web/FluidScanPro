package com.fluidscan.pro.service.pdf

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles flattened page bitmaps into a PDF using PDFBox-Android and (optionally) applies
 * AES password protection. Heavy + allocation-heavy → always invoke from `Dispatchers.IO`.
 *
 * Each bitmap becomes one page sized to the image's aspect ratio at [pointsPerInch]-ish
 * scale; the image is drawn to fill the page. Bitmaps passed in are NOT recycled here.
 */
@Singleton
class PdfBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        // Loads embedded fonts/resources from assets — required before any PDF op.
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    /**
     * @param pages flattened, render-ready page bitmaps in page order.
     * @param output destination file.
     * @param password if non-null, encrypts the PDF (AES) with this user password.
     * @param jpegQuality 0..1 quality for the embedded page images.
     */
    fun build(
        pages: List<Bitmap>,
        output: File,
        password: String? = null,
        jpegQuality: Float = 0.85f
    ): File {
        require(pages.isNotEmpty()) { "Cannot build a PDF with no pages" }

        PDDocument().use { doc ->
            for (bitmap in pages) {
                val pageRect = PDRectangle(bitmap.width.toFloat(), bitmap.height.toFloat())
                val page = PDPage(pageRect)
                doc.addPage(page)

                val image = JPEGFactory.createFromImage(doc, bitmap, jpegQuality)
                PDPageContentStream(doc, page).use { stream ->
                    stream.drawImage(image, 0f, 0f, pageRect.width, pageRect.height)
                }
            }

            if (!password.isNullOrEmpty()) {
                applyEncryption(doc, password)
            }

            doc.save(output)
        }
        return output
    }

    private fun applyEncryption(doc: PDDocument, password: String) {
        val permissions = AccessPermission().apply {
            // Allow viewing/printing; lock down content extraction & assembly.
            setCanExtractContent(false)
            setCanModify(false)
            setCanAssembleDocument(false)
        }
        // Owner == user for a single-password flow.
        val policy = StandardProtectionPolicy(password, password, permissions).apply {
            setEncryptionKeyLength(256)
            setPreferAES(true)
        }
        doc.protect(policy)
    }
}
