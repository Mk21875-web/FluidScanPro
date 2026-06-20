package com.fluidscan.pro.testutil

import com.fluidscan.pro.domain.model.Document
import com.fluidscan.pro.domain.model.SyncState
import com.fluidscan.pro.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [DocumentRepository] that records mutations for assertions. */
class FakeDocumentRepository(initial: List<Document> = emptyList()) : DocumentRepository {

    private val docs = MutableStateFlow(initial)
    val deleted = mutableListOf<String>()
    val syncCalls = mutableListOf<Pair<String, SyncState>>()

    fun setDocuments(documents: List<Document>) { docs.value = documents }

    override fun observe(query: String): Flow<List<Document>> =
        docs.map { list ->
            if (query.isBlank()) list else list.filter { it.title.contains(query, ignoreCase = true) }
        }

    override suspend fun get(id: String): Document? = docs.value.firstOrNull { it.id == id }

    override suspend fun upsert(document: Document) {
        docs.value = docs.value.filterNot { it.id == document.id } + document
    }

    override suspend fun delete(id: String) {
        deleted += id
        docs.value = docs.value.filterNot { it.id == id }
    }

    override suspend fun setSyncState(id: String, state: SyncState) {
        syncCalls += id to state
    }
}
