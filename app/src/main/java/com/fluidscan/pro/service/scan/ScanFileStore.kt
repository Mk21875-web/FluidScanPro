package com.fluidscan.pro.service.scan

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the on-disk scratch space for an in-progress scan session. */
@Singleton
class ScanFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val root: File by lazy {
        File(context.cacheDir, "scans").apply { mkdirs() }
    }

    fun newRawFile(): File = File(root, "raw_${System.currentTimeMillis()}.jpg")

    fun newProcessedFile(): File = File(root, "proc_${System.currentTimeMillis()}.jpg")

    fun delete(vararg files: File?) {
        files.forEach { f -> runCatching { f?.takeIf { it.exists() }?.delete() } }
    }
}
