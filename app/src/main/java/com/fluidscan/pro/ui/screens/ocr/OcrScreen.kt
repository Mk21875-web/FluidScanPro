package com.fluidscan.pro.ui.screens.ocr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fluidscan.pro.domain.model.OcrScript
import com.fluidscan.pro.ui.screens.ocr.components.BrushSweepOverlay
import com.fluidscan.pro.ui.screens.ocr.components.OcrLineHighlightOverlay
import com.fluidscan.pro.ui.screens.ocr.components.TypewriterText
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    onBack: () -> Unit,
    viewModel: OcrViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // Drives the line-by-line highlight sweep after recognition completes.
    var revealedLines by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (state.pages.isEmpty()) viewModel.onIntent(OcrIntent.LoadFromScan)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OcrEffect.TextRecognized -> {
                    revealedLines = 0
                    for (i in 1..effect.lineCount) {
                        revealedLines = i
                        delay(70)
                    }
                }
                is OcrEffect.CopyText -> clipboard.setText(AnnotatedString(effect.text))
                is OcrEffect.NameSuggested -> Unit
                OcrEffect.CleanupDone -> snackbar.showSnackbar("Cleanup applied")
                is OcrEffect.Error -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.hasText) {
                        IconButton(onClick = { clipboard.setText(AnnotatedString(state.result.fullText)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy text")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScriptSelector(
                selected = state.script,
                onSelect = { viewModel.onIntent(OcrIntent.SetScript(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = state.currentUri,
                    contentDescription = "Page ${state.currentPage + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                OcrLineHighlightOverlay(
                    lines = state.result.lines,
                    revealedCount = revealedLines,
                    highlight = MaterialTheme.colorScheme.primary,
                    scanner = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxSize()
                )
                if (state.isCleaning) {
                    BrushSweepOverlay(color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxSize())
                }
                if (state.isRecognizing) {
                    CircularProgressIndicator()
                }
            }

            BottomPanel(state = state, onIntent = viewModel::onIntent)
        }
    }
}

@Composable
private fun ScriptSelector(
    selected: OcrScript,
    onSelect: (OcrScript) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OcrScript.entries.forEach { script ->
            FilterChip(
                selected = script == selected,
                onClick = { onSelect(script) },
                label = { Text(script.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun BottomPanel(state: OcrState, onIntent: (OcrIntent) -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(visible = state.suggestedName != null) {
                state.suggestedName?.let { name ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        TypewriterText(text = name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (state.hasText) {
                Text(
                    text = state.result.fullText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onIntent(OcrIntent.RunOcr) },
                    enabled = !state.isRecognizing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                    Text("  Recognize")
                }
                OutlinedButton(
                    onClick = { onIntent(OcrIntent.RunCleanup) },
                    enabled = !state.isCleaning
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = "AI cleanup")
                }
                OutlinedButton(
                    onClick = { onIntent(OcrIntent.GenerateName) },
                    enabled = state.hasText
                ) {
                    Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = "Smart name")
                }
            }
        }
    }
}
