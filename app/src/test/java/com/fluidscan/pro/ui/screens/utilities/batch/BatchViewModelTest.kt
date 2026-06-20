package com.fluidscan.pro.ui.screens.utilities.batch

import android.net.Uri
import com.fluidscan.pro.domain.model.BatchItem
import com.fluidscan.pro.domain.model.BatchStage
import com.fluidscan.pro.service.utility.BatchProcessor
import com.fluidscan.pro.service.utility.BatchProgress
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val processor = mockk<BatchProcessor>()
    private lateinit var vm: BatchViewModel

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        vm = BatchViewModel(processor)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    private fun uri(n: Int): Uri = Uri.parse("content://media/$n")

    @Test
    fun `adding images appends labelled items`() {
        vm.onIntent(BatchIntent.AddImages(listOf(uri(1), uri(2))))

        val items = vm.state.value.job.items
        assertThat(items).hasSize(2)
        assertThat(items.map { it.label }).containsExactly("Page 1", "Page 2").inOrder()
    }

    @Test
    fun `adding an empty list is a no-op`() {
        vm.onIntent(BatchIntent.AddImages(emptyList()))
        assertThat(vm.state.value.job.items).isEmpty()
    }

    @Test
    fun `clear resets the job`() {
        vm.onIntent(BatchIntent.AddImages(listOf(uri(1))))
        vm.onIntent(BatchIntent.Clear)
        assertThat(vm.state.value.job.items).isEmpty()
    }

    @Test
    fun `start with no items does not run the processor`() {
        vm.onIntent(BatchIntent.Start)
        assertThat(vm.state.value.job.isRunning).isFalse()
    }

    @Test
    fun `start drives the job to a finished state with the output PDF`() {
        vm.onIntent(BatchIntent.AddImages(listOf(uri(1))))
        val done = vm.state.value.job.items.map { it.copy(stage = BatchStage.DONE) }
        val out = Uri.parse("file:///batch/out.pdf")
        every { processor.run(any()) } returns flowOf(BatchProgress(done, outputPdfUri = out))

        vm.onIntent(BatchIntent.Start)

        val job = vm.state.value.job
        assertThat(job.isRunning).isFalse()
        assertThat(job.outputPdfUri).isEqualTo(out)
        assertThat(job.items.all { it.stage == BatchStage.DONE }).isTrue()
    }

    @Test
    fun `sharing before running prompts the user to run first`() = runTest(dispatcher) {
        val effects = mutableListOf<BatchEffect>()
        val collector = launch { vm.effects.toList(effects) }

        vm.onIntent(BatchIntent.Share)

        assertThat(effects.filterIsInstance<BatchEffect.Message>()).isNotEmpty()
        collector.cancel()
    }

    @Test
    fun `sharing after a finished run emits the share effect`() = runTest(dispatcher) {
        val out = Uri.parse("file:///batch/out.pdf")
        every { processor.run(any()) } returns flowOf(
            BatchProgress(listOf(BatchItem("a", uri(1), "Page 1", BatchStage.DONE)), outputPdfUri = out)
        )
        vm.onIntent(BatchIntent.AddImages(listOf(uri(1))))
        vm.onIntent(BatchIntent.Start)

        val effects = mutableListOf<BatchEffect>()
        val collector = launch { vm.effects.toList(effects) }
        vm.onIntent(BatchIntent.Share)

        assertThat(effects).contains(BatchEffect.SharePdf(out))
        collector.cancel()
    }
}
