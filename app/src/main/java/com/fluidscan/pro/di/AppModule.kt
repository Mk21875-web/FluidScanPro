package com.fluidscan.pro.di

import com.fluidscan.pro.core.common.DefaultDispatcherProvider
import com.fluidscan.pro.core.common.DispatcherProvider
import com.fluidscan.pro.service.scan.EdgeDetector
import com.fluidscan.pro.service.scan.LuminanceEdgeDetector
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanBindingsModule {

    /**
     * Default real-time detector is the on-device luminance/contour heuristic.
     * Swap this binding for an OpenCV- or ML-Kit-backed implementation without
     * touching any caller.
     */
    @Binds
    @Singleton
    abstract fun bindEdgeDetector(impl: LuminanceEdgeDetector): EdgeDetector
}
