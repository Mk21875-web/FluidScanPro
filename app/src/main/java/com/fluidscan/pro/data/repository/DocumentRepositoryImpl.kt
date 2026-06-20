package com.fluidscan.pro.data.repository

import com.fluidscan.pro.core.common.DispatcherProvider
import com.fluidscan.pro.data.local.dao.DocumentDao
import com.fluidscan.pro.data.mapper.toDomain
import com.fluidscan.pro.data.mapper.toEntity
import com.fluidscan.pro.domain.model.Document
import com.fluidscan.pro.domain.model.SyncState
import com.fluidscan.pro.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val dao: DocumentDao,
    private val dispatchers: DispatcherProvider
) : DocumentRepository {

    override fun observe(query: String): Flow<List<Document>> =
        dao.observe(query)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)

    override suspend fun get(id: String): Document? = withContext(dispatchers.io) {
        dao.get(id)?.toDomain()
    }

    override suspend fun upsert(document: Document) = withContext(dispatchers.io) {
        dao.upsert(document.toEntity())
    }

    override suspend fun delete(id: String) = withContext(dispatchers.io) {
        dao.delete(id)
    }

    override suspend fun setSyncState(id: String, state: SyncState) = withContext(dispatchers.io) {
        dao.setSyncState(id, state.name)
    }
}
