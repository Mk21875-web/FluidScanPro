package com.fluidscan.pro.data.mapper

import com.fluidscan.pro.data.local.entity.DocumentEntity
import com.fluidscan.pro.domain.model.Document
import com.fluidscan.pro.domain.model.SyncState

fun DocumentEntity.toDomain(): Document = Document(
    id = id,
    title = title,
    pageUris = pageUris,
    pdfUri = pdfUri,
    thumbnailUri = thumbnailUri,
    isPasswordProtected = isPasswordProtected,
    syncState = runCatching { SyncState.valueOf(syncState) }.getOrDefault(SyncState.LOCAL),
    createdAtEpochMs = createdAt,
    updatedAtEpochMs = updatedAt
)

fun Document.toEntity(): DocumentEntity = DocumentEntity(
    id = id,
    title = title,
    pageUris = pageUris,
    pdfUri = pdfUri,
    thumbnailUri = thumbnailUri,
    isPasswordProtected = isPasswordProtected,
    syncState = syncState.name,
    createdAt = createdAtEpochMs,
    updatedAt = updatedAtEpochMs
)
