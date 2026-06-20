package com.fluidscan.pro.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.fluidscan.pro.BuildConfig
import java.io.File
import java.io.FileOutputStream

/** Centralized share/print actions for produced PDFs. */
object ShareUtils {

    fun sharePdf(context: Context, uri: Uri, chooserTitle: String = "Share PDF") {
        val shareUri = toContentUri(context, uri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun printPdf(context: Context, uri: Uri, jobName: String = "FluidScan Document") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(jobName, PdfPrintAdapter(context, uri, jobName), null)
    }

    private fun toContentUri(context: Context, uri: Uri): Uri =
        if (uri.scheme == "content") uri
        else FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            File(requireNotNull(uri.path))
        )
}

/** Streams an existing PDF file straight to the print spooler. */
private class PdfPrintAdapter(
    private val context: Context,
    private val uri: Uri,
    private val jobName: String
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("$jobName.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    requireNotNull(input).copyTo(output)
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (t: Throwable) {
            callback.onWriteFailed(t.message)
        }
    }
}
