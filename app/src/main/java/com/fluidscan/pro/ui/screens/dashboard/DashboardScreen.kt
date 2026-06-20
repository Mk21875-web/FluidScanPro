package com.fluidscan.pro.ui.screens.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluidscan.pro.ui.screens.dashboard.components.DocumentCard
import com.fluidscan.pro.ui.screens.dashboard.components.DocumentDetailSheet
import com.fluidscan.pro.ui.screens.dashboard.components.ExpandingSearchBar
import com.fluidscan.pro.ui.theme.FluidScanMotion

@Composable
fun DashboardScreen(
    onOpenScanner: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenOcr: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)
    ) { uris -> if (uris.isNotEmpty()) viewModel.onIntent(DashboardIntent.ImportImages(uris)) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.NavigateToScanner -> onOpenScanner()
                is DashboardEffect.NavigateToEditor -> onOpenEditor()
                is DashboardEffect.NavigateToOcr -> onOpenOcr()
                is DashboardEffect.Message -> snackbar.showSnackbar(effect.text)
            }
        }
    }

    val blurRadius by animateDpAsState(
        targetValue = if (state.isSearchExpanded) 18.dp else 0.dp,
        animationSpec = FluidScanMotion.Springs.expand(),
        label = "dashBlur"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Scan") },
                icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                onClick = { viewModel.onIntent(DashboardIntent.NewScan) }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                DashboardTopBar(
                    state = state,
                    onIntent = viewModel::onIntent,
                    onImport = { importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )

                Box(Modifier.fillMaxSize().blur(blurRadius)) {
                    when {
                        state.isEmpty -> EmptyState()
                        else -> DocumentsContent(state = state, onIntent = viewModel::onIntent)
                    }
                }
            }

            DocumentDetailSheet(
                document = state.expandedDocument,
                onDismiss = { viewModel.onIntent(DashboardIntent.ExpandDocument(null)) },
                onOpen = { state.expandedDocId?.let { viewModel.onIntent(DashboardIntent.OpenInEditor(it)) } },
                onOcr = { state.expandedDocId?.let { viewModel.onIntent(DashboardIntent.OpenOcr(it)) } },
                onDelete = { state.expandedDocId?.let { viewModel.onIntent(DashboardIntent.DeleteDocument(it)) } }
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    onImport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!state.isSearchExpanded) {
            Text(
                "FluidScan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onImport) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Import from gallery")
            }
            IconButton(onClick = { onIntent(DashboardIntent.ToggleViewMode) }) {
                Icon(
                    if (state.viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                    contentDescription = "Toggle view"
                )
            }
            ExpandingSearchBar(
                expanded = false,
                query = state.query,
                onQueryChange = { onIntent(DashboardIntent.SetQuery(it)) },
                onExpandedChange = { onIntent(DashboardIntent.SetSearchExpanded(it)) },
                modifier = Modifier.width(48.dp)
            )
        } else {
            ExpandingSearchBar(
                expanded = true,
                query = state.query,
                onQueryChange = { onIntent(DashboardIntent.SetQuery(it)) },
                onExpandedChange = { onIntent(DashboardIntent.SetSearchExpanded(it)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DocumentsContent(state: DashboardState, onIntent: (DashboardIntent) -> Unit) {
    // Grid ↔ List morph: cross-scale/fade between the two layouts.
    AnimatedContent(
        targetState = state.viewMode,
        transitionSpec = {
            (fadeIn(animationSpec = FluidScanMotion.standardTween()) +
                scaleIn(initialScale = 0.92f, animationSpec = FluidScanMotion.standardTween()))
                .togetherWith(fadeOut(animationSpec = FluidScanMotion.emphasizedExit()) +
                    scaleOut(targetScale = 0.95f, animationSpec = FluidScanMotion.emphasizedExit()))
        },
        label = "gridListMorph"
    ) { mode ->
        when (mode) {
            ViewMode.GRID -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.documents, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        viewMode = ViewMode.GRID,
                        onClick = { onIntent(DashboardIntent.ExpandDocument(doc.id)) },
                        onSync = { onIntent(DashboardIntent.SyncDocument(doc.id)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            ViewMode.LIST -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.documents, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        viewMode = ViewMode.LIST,
                        onClick = { onIntent(DashboardIntent.ExpandDocument(doc.id)) },
                        onSync = { onIntent(DashboardIntent.SyncDocument(doc.id)) },
                        modifier = Modifier.fillMaxWidth().animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.width(48.dp))
            Text("No documents yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Scan or import to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
