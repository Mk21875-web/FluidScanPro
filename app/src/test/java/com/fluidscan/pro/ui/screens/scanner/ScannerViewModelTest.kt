package com.fluidscan.pro.ui.screens.scanner

import androidx.compose.ui.geometry.Offset
import com.fluidscan.pro.core.common.ScanHandoff
import com.fluidscan.pro.domain.model.Quadrilateral
import com.fluidscan.pro.domain.model.ScanFilter
import com.fluidscan.pro.service.scan.EdgeDetector
import com.fluidscan.pro.service.scan.EdgeResult
import com.fluidscan.pro.service.scan.ImageFilters
import com.fluidscan.pro.service.scan.PerspectiveTransformer
import com.fluidscan.pro.service.scan.ScanFileStore
import com.fluidscan.pro.testutil.TestDispatcherProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var vm: ScannerViewModel

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        vm = ScannerViewModel(
            dispatchers = TestDispatcherProvider(dispatcher),
            fileStore = mockk<ScanFileStore>(relaxed = true),
            perspective = mockk<PerspectiveTransformer>(relaxed = true),
            filters = mockk<ImageFilters>(relaxed = true),
            handoff = mockk<ScanHandoff>(relaxed = true),
            edgeDetector = mockk<EdgeDetector>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `toggle flash flips the flag`() {
        assertThat(vm.state.value.flashEnabled).isFalse()
        vm.onIntent(ScannerIntent.ToggleFlash)
        assertThat(vm.state.value.flashEnabled).isTrue()
        vm.onIntent(ScannerIntent.ToggleFlash)
        assertThat(vm.state.value.flashEnabled).isFalse()
    }

    @Test
    fun `selecting a filter updates state`() {
        vm.onIntent(ScannerIntent.SelectFilter(ScanFilter.MAGIC_COLOR))
        assertThat(vm.state.value.selectedFilter).isEqualTo(ScanFilter.MAGIC_COLOR)
    }

    @Test
    fun `dragging a corner far from any guide leaves it where the user put it`() {
        vm.onIntent(ScannerIntent.CropCornerMoved(0, Offset(0.5f, 0.5f)))
        assertThat(vm.state.value.editingQuad.topLeft).isEqualTo(Offset(0.5f, 0.5f))
    }

    @Test
    fun `dragging a corner near the frame edge magnetically snaps it`() {
        vm.onIntent(ScannerIntent.CropCornerMoved(0, Offset(0.01f, 0.01f)))
        assertThat(vm.state.value.editingQuad.topLeft).isEqualTo(Offset(0f, 0f))
    }

    @Test
    fun `a magnetic snap fires the lock haptic`() = runTest(dispatcher) {
        val effects = mutableListOf<ScannerEffect>()
        val job = launch { vm.effects.toList(effects) }

        vm.onIntent(ScannerIntent.CropCornerMoved(0, Offset(0.01f, 0.01f)))

        assertThat(effects).contains(ScannerEffect.MagneticLockHaptic)
        job.cancel()
    }

    @Test
    fun `edge detection fires the snap haptic only on the rising edge`() = runTest(dispatcher) {
        val effects = mutableListOf<ScannerEffect>()
        val job = launch { vm.effects.toList(effects) }

        val present = EdgeResult(Quadrilateral.FULL, confidence = 0.9f)
        vm.onIntent(ScannerIntent.EdgeDetected(present))
        vm.onIntent(ScannerIntent.EdgeDetected(present)) // still present -> no repeat

        assertThat(effects.count { it is ScannerEffect.EdgeSnapHaptic }).isEqualTo(1)
        job.cancel()
    }

    @Test
    fun `low-confidence edges do not fire a haptic`() = runTest(dispatcher) {
        val effects = mutableListOf<ScannerEffect>()
        val job = launch { vm.effects.toList(effects) }

        vm.onIntent(ScannerIntent.EdgeDetected(EdgeResult(Quadrilateral.FULL, confidence = 0.1f)))

        assertThat(effects).isEmpty()
        job.cancel()
    }

    @Test
    fun `finishing with no pages surfaces an error`() = runTest(dispatcher) {
        val effects = mutableListOf<ScannerEffect>()
        val job = launch { vm.effects.toList(effects) }

        vm.onIntent(ScannerIntent.FinishSession)

        assertThat(effects.filterIsInstance<ScannerEffect.Error>()).isNotEmpty()
        job.cancel()
    }
}
