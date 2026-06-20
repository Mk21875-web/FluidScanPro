package com.fluidscan.pro.ui.screens.utilities.batch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluidscan.pro.core.util.ShareUtils
import com.fluidscan.pro.ui.components.PaperPlaneOverlay
import com.fluidscan.pro.ui.screens.utilities.batch.components.AssemblyLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    onBack: () -> Unit,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var planeKey by remember { mutableIntStateOf(0) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris -> if (uris.isNotEmpty()) viewModel.onIntent(BatchIntent.AddImages(uris)) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BatchEffect.SharePdf -> {
                    planeKey++
                    ShareUtils.sharePdf(context, effect.uri)
                }
                is BatchEffect.Message -> snackbar.showSnackbar(effect.text)
            }
        }
    }

    val progress by animateFloatAsState(targetValue = state.job.progress, label = "batchProgress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { importLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Text("  Add")
                    }
                    Button(
                        onClick = { viewModel.onIntent(BatchIntent.Start) },
                        enabled = state.job.items.isNotEmpty() && !state.job.isRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("  Run")
                    }
                }

                if (state.job.items.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AssemblyLine(
                    items = state.job.items,
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.onIntent(BatchIntent.Share) },
                        enabled = state.job.outputPdfUri != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null)
                        Text("  Share")
                    }
                    OutlinedButton(
                        onClick = { state.job.outputPdfUri?.let { ShareUtils.printPdf(context, it) } },
                        enabled = state.job.outputPdfUri != null
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                }
            }

            PaperPlaneOverlay(playKey = planeKey, modifier = Modifier.fillMaxSize())
        }
    }
}
