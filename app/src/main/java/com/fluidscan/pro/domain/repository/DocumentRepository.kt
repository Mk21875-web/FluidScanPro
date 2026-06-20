package com.fluidscan.pro.domain.repository

import com.fluidscan.pro.domain.model.Document
import com.fluidscan.pro.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/** Domain-facing contract for document persistence. Implemented in the data layer (Room). */
interface DocumentRepository {
    /** Observes all documents, newest first. [query] filters by title (blank = all). */
    fun observe(query: String = ""): Flow<List<Document>>

    suspend fun get(id: String): Document?

    suspend fun upsert(document: Document)

    suspend fun delete(id: String)

    suspend fun setSyncState(id: String, state: SyncState)
}
