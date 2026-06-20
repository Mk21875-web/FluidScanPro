package com.fluidscan.pro.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fluidscan.pro.data.local.Converters
import com.fluidscan.pro.data.local.dao.DocumentDao
import com.fluidscan.pro.data.local.entity.DocumentEntity

@Database(
    entities = [DocumentEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FluidScanDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        const val NAME = "fluidscan.db"
    }
}
