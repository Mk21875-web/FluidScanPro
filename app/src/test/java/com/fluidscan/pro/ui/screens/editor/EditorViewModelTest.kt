package com.fluidscan.pro.ui.screens.editor

import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.fluidscan.pro.core.common.ScanHandoff
import com.fluidscan.pro.domain.model.Annotation
import com.fluidscan.pro.service.pdf.PageFlattener
import com.fluidscan.pro.service.pdf.PdfBuilder
import com.fluidscan.pro.testutil.FakeDocumentRepository
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var vm: EditorViewModel

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        vm = EditorViewModel(
            context = mockk<Context>(relaxed = true),
            dispatchers = TestDispatcherProvider(dispatcher),
            handoff = mockk<ScanHandoff>(relaxed = true),
            documents = FakeDocumentRepository(),
            flattener = mockk<PageFlattener>(relaxed = true),
            pdfBuilder = mockk<PdfBuilder>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    private fun uri(n: Int): Uri = Uri.parse("content://p/$n")

    private fun loadOnePage() {
        vm.onIntent(EditorIntent.LoadPages(listOf(uri(1)), "Doc"))
    }

    @Test
    fun `loading pages populates the document`() {
        vm.onIntent(EditorIntent.LoadPages(listOf(uri(1), uri(2)), "Doc"))
        assertThat(vm.state.value.document.pages).hasSize(2)
        assertThat(vm.state.value.document.title).isEqualTo("Doc")
        assertThat(vm.state.value.currentPage).isEqualTo(0)
    }

    @Test
    fun `selecting a tool clears the current annotation selection`() {
        loadOnePage()
        vm.onIntent(EditorIntent.AddText(Offset(0.2f, 0.2f)))
        assertThat(vm.state.value.selectedAnnotationId).isNotNull()

        vm.onIntent(EditorIntent.SelectTool(EditorTool.PEN))
        assertThat(vm.state.value.tool).isEqualTo(EditorTool.PEN)
        assertThat(vm.state.value.selectedAnnotationId).isNull()
    }

    @Test
    fun `a multi-sample pen stroke commits one ink annotation`() {
        loadOnePage()
        vm.onIntent(EditorIntent.InkStart(Offset(0.1f, 0.1f)))
        vm.onIntent(EditorIntent.InkMove(Offset(0.2f, 0.2f)))
        vm.onIntent(EditorIntent.InkEnd)

        val page = vm.state.value.document.pages.first()
        assertThat(page.annotations.filterIsInstance<Annotation.Ink>()).hasSize(1)
        assertThat(vm.state.value.liveInk).isEmpty()
    }

    @Test
    fun `a single-sample stroke is discarded`() {
        loadOnePage()
        vm.onIntent(EditorIntent.InkStart(Offset(0.1f, 0.1f)))
        vm.onIntent(EditorIntent.InkEnd)

        assertThat(vm.state.value.document.pages.first().annotations).isEmpty()
        assertThat(vm.state.value.liveInk).isEmpty()
    }

    @Test
    fun `adding then editing text updates the annotation`() {
        loadOnePage()
        vm.onIntent(EditorIntent.AddText(Offset(0.3f, 0.3f)))
        val id = vm.state.value.selectedAnnotationId!!

        vm.onIntent(EditorIntent.EditText(id, "Hello"))

        val text = vm.state.value.document.pages.first()
            .annotations.filterIsInstance<Annotation.Text>().single()
        assertThat(text.text).isEqualTo("Hello")
    }

    @Test
    fun `adding and removing pages adjusts the document`() {
        loadOnePage()
        vm.onIntent(EditorIntent.AddPage(uri(2)))
        assertThat(vm.state.value.document.pages).hasSize(2)

        val firstId = vm.state.value.document.pages.first().id
        vm.onIntent(EditorIntent.RemovePage(firstId))
        assertThat(vm.state.value.document.pages).hasSize(1)
    }

    @Test
    fun `reordering pages swaps their order`() {
        vm.onIntent(EditorIntent.LoadPages(listOf(uri(1), uri(2)), "Doc"))
        val original = vm.state.value.document.pages.map { it.id }

        vm.onIntent(EditorIntent.ReorderPages(0, 1))

        val reordered = vm.state.value.document.pages.map { it.id }
        assertThat(reordered).containsExactly(original[1], original[0]).inOrder()
    }

    @Test
    fun `setting a password marks the document protected and fires the lock haptic`() = runTest(dispatcher) {
        loadOnePage()
        val effects = mutableListOf<EditorEffect>()
        val collector = launch { vm.effects.toList(effects) }

        vm.onIntent(EditorIntent.SetPassword("secret"))

        assertThat(vm.state.value.document.isPasswordProtected).isTrue()
        assertThat(effects).contains(EditorEffect.LockHaptic)
        collector.cancel()
    }

    @Test
    fun `removing the password clears protection`() {
        loadOnePage()
        vm.onIntent(EditorIntent.SetPassword("secret"))
        vm.onIntent(EditorIntent.RemovePassword)
        assertThat(vm.state.value.document.isPasswordProtected).isFalse()
    }

    @Test
    fun `exporting an empty document surfaces an error`() = runTest(dispatcher) {
        val effects = mutableListOf<EditorEffect>()
        val collector = launch { vm.effects.toList(effects) }

        vm.onIntent(EditorIntent.Export)

        assertThat(effects.filterIsInstance<EditorEffect.Error>()).isNotEmpty()
        collector.cancel()
    }
}
