package com.fluidscan.pro.di

import android.content.Context
import androidx.room.Room
import com.fluidscan.pro.data.local.dao.DocumentDao
import com.fluidscan.pro.data.local.db.FluidScanDatabase
import com.fluidscan.pro.data.repository.DocumentRepositoryImpl
import com.fluidscan.pro.domain.repository.DocumentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FluidScanDatabase =
        Room.databaseBuilder(context, FluidScanDatabase::class.java, FluidScanDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDocumentDao(db: FluidScanDatabase): DocumentDao = db.documentDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository
}
