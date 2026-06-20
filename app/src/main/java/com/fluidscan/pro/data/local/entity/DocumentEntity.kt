package com.fluidscan.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val pageUris: List<String>,
    val pdfUri: String?,
    val thumbnailUri: String?,
    val isPasswordProtected: Boolean,
    val syncState: String,
    val createdAt: Long,
    val updatedAt: Long
)
