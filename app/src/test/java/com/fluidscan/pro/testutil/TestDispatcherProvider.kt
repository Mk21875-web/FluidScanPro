package com.fluidscan.pro.testutil

import com.fluidscan.pro.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

/** Routes every dispatcher to a single test dispatcher so coroutines run deterministically. */
class TestDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
