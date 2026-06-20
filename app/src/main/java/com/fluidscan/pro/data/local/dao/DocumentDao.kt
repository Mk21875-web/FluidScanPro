package com.fluidscan.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fluidscan.pro.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query(
        "SELECT * FROM documents WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC"
    )
    fun observe(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun get(id: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE documents SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: String)
}
