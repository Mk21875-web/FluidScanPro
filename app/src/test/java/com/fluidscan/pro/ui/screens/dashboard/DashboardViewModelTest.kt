package com.fluidscan.pro.ui.screens.dashboard

import com.fluidscan.pro.core.common.ScanHandoff
import com.fluidscan.pro.domain.model.Document
import com.fluidscan.pro.testutil.FakeDocumentRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeDocumentRepository
    private val handoff = mockk<ScanHandoff>(relaxed = true)
    private lateinit var vm: DashboardViewModel

    private fun doc(id: String, title: String) =
        Document(id = id, title = title, pageUris = listOf("content://p/$id"))

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        repo = FakeDocumentRepository()
        vm = DashboardViewModel(repo, handoff)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `toggling view mode flips between grid and list`() {
        assertThat(vm.state.value.viewMode).isEqualTo(ViewMode.GRID)
        vm.onIntent(DashboardIntent.ToggleViewMode)
        assertThat(vm.state.value.viewMode).isEqualTo(ViewMode.LIST)
        vm.onIntent(DashboardIntent.ToggleViewMode)
        assertThat(vm.state.value.viewMode).isEqualTo(ViewMode.GRID)
    }

    @Test
    fun `setting a query updates state`() {
        vm.onIntent(DashboardIntent.SetQuery("invoice"))
        assertThat(vm.state.value.query).isEqualTo("invoice")
    }

    @Test
    fun `collapsing the search bar clears the query`() {
        vm.onIntent(DashboardIntent.SetQuery("invoice"))
        vm.onIntent(DashboardIntent.SetSearchExpanded(false))
        assertThat(vm.state.value.query).isEmpty()
    }

    @Test
    fun `expanding a document records its id`() {
        vm.onIntent(DashboardIntent.ExpandDocument("doc-1"))
        assertThat(vm.state.value.expandedDocId).isEqualTo("doc-1")
    }

    @Test
    fun `documents from the repository load into state after the debounce`() = runTest(dispatcher) {
        repo.setDocuments(listOf(doc("a", "Alpha"), doc("b", "Beta")))
        advanceUntilIdle()

        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.documents.map { it.id }).containsExactly("a", "b")
    }

    @Test
    fun `a query filters the observed documents`() = runTest(dispatcher) {
        repo.setDocuments(listOf(doc("a", "Alpha"), doc("b", "Beta")))
        vm.onIntent(DashboardIntent.SetQuery("alph"))
        advanceUntilIdle()

        assertThat(vm.state.value.documents.map { it.id }).containsExactly("a")
    }

    @Test
    fun `deleting a document removes it and collapses the sheet`() = runTest(dispatcher) {
        repo.setDocuments(listOf(doc("a", "Alpha")))
        advanceUntilIdle()
        vm.onIntent(DashboardIntent.ExpandDocument("a"))

        vm.onIntent(DashboardIntent.DeleteDocument("a"))
        advanceUntilIdle()

        assertThat(repo.deleted).containsExactly("a")
        assertThat(vm.state.value.expandedDocId).isNull()
    }

    @Test
    fun `new scan emits the scanner navigation effect`() = runTest(dispatcher) {
        val effects = mutableListOf<DashboardEffect>()
        val collector = launch { vm.effects.toList(effects) }

        vm.onIntent(DashboardIntent.NewScan)
        advanceUntilIdle()

        assertThat(effects).contains(DashboardEffect.NavigateToScanner)
        collector.cancel()
    }
}
