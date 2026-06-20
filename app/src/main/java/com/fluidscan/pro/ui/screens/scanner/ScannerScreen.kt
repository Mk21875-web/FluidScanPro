package com.fluidscan.pro.ui.screens.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluidscan.pro.core.haptics.HapticEngine
import com.fluidscan.pro.ui.screens.scanner.components.CameraPreview
import com.fluidscan.pro.ui.screens.scanner.components.CapturedPagesBar
import com.fluidscan.pro.ui.screens.scanner.components.CropEditor
import com.fluidscan.pro.ui.screens.scanner.components.EdgeDetectionOverlay
import com.fluidscan.pro.ui.screens.scanner.components.FilterFilmstrip
import com.fluidscan.pro.ui.screens.scanner.components.rememberCameraController
import com.fluidscan.pro.ui.theme.FluidScanMotion
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

/**
 * Phase 1 — Core Scanning Engine screen. Composes the live camera, the spring-loaded
 * edge overlay, the cross-fading filter filmstrip, the capture shutter, and the
 * drag-to-reorder captured-pages strip. All heavy work is delegated to the ViewModel.
 */
@Composable
fun ScannerScreen(
    onFinished: (pageIds: List<String>) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val controller = rememberCameraController()
    val haptics = remember { HapticEngine(context.applicationContext) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // One-shot effects → haptics, capture, navigation, errors.
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ScannerEffect.TakePicture -> controller.capture(
                    context = context,
                    target = File(effect.targetPath),
                    onSaved = { path, rot -> viewModel.onIntent(ScannerIntent.CapturedRawFile(path, rot)) },
                    onError = { msg -> viewModel.onIntent(ScannerIntent.CaptureFailed(msg)) }
                )
                ScannerEffect.EdgeSnapHaptic -> haptics.edgeSnap()
                ScannerEffect.CaptureHaptic -> haptics.captureConfirm()
                ScannerEffect.MagneticLockHaptic -> haptics.magneticLock()
                is ScannerEffect.Error -> scope.launch { snackbarHost.showSnackbar(effect.message) }
                is ScannerEffect.NavigateToReview -> onFinished(effect.pageIds)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(
                controller = controller,
                detector = viewModel.edgeDetector,
                flashEnabled = state.flashEnabled,
                onEdge = { viewModel.onIntent(ScannerIntent.EdgeDetected(it)) },
                modifier = Modifier.fillMaxSize()
            )
            EdgeDetectionOverlay(edge = state.liveEdge, modifier = Modifier.fillMaxSize())
        } else {
            PermissionPrompt(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Top bar: flash + "document found" hint.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentFoundChip(visible = state.liveEdge.hasDocument)
            IconToggle(
                checked = state.flashEnabled,
                onToggle = { viewModel.onIntent(ScannerIntent.ToggleFlash) }
            )
        }

        // Bottom controls: filter filmstrip + captured pages + shutter + done.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(bottom = 16.dp)
        ) {
            if (state.hasPages) {
                CapturedPagesBar(
                    pages = state.capturedPages,
                    onReorder = { f, t -> viewModel.onIntent(ScannerIntent.ReorderPages(f, t)) },
                    onOpenCrop = { viewModel.onIntent(ScannerIntent.OpenCrop(it)) },
                    onRemove = { viewModel.onIntent(ScannerIntent.RemovePage(it)) },
                    onLiftHaptic = { haptics.magneticLock() }
                )
            }

            FilterFilmstrip(
                previewUri = state.capturedPages.lastOrNull()?.displayUri,
                selected = state.selectedFilter,
                onSelect = { viewModel.onIntent(ScannerIntent.SelectFilter(it)) },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(56.dp)) // spacer to balance the Done button
                ShutterButton(
                    enabled = hasCameraPermission && !state.isCapturing,
                    isBusy = state.isCapturing || state.isProcessing,
                    onCapture = { viewModel.onIntent(ScannerIntent.CaptureRequested) }
                )
                DoneButton(
                    visible = state.hasPages,
                    count = state.pageCount,
                    onClick = { viewModel.onIntent(ScannerIntent.FinishSession) }
                )
            }
        }

        // Manual crop editor overlays everything, cross-fading in.
        Crossfade(targetState = state.editing, label = "cropEditor") { editing ->
            if (editing != null) {
                CropEditor(
                    imageUri = editing.displayUri,
                    quad = state.editingQuad,
                    onCornerMove = { i, n -> viewModel.onIntent(ScannerIntent.CropCornerMoved(i, n)) },
                    onConfirm = { viewModel.onIntent(ScannerIntent.CropConfirmed) },
                    onDismiss = { viewModel.onIntent(ScannerIntent.CropDismissed) }
                )
            }
        }

        SnackbarHost(snackbarHost, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, isBusy: Boolean, onCapture: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isBusy) 0.86f else 1f,
        animationSpec = FluidScanMotion.Springs.snap(),
        label = "shutter"
    )
    Box(
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 1f else 0.4f))
            .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(enabled = enabled) { onCapture() },
        contentAlignment = Alignment.Center
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun DoneButton(visible: Boolean, count: Int, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(FluidScanMotion.Springs.snap()) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Check, contentDescription = "Finish", tint = MaterialTheme.colorScheme.onPrimary)
                Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun IconToggle(checked: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
            contentDescription = "Toggle flash",
            tint = Color.White
        )
    }
}

@Composable
private fun DocumentFoundChip(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(FluidScanMotion.Springs.snap()) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                "Document found",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Camera access is needed to scan documents.",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onRequest() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Grant camera access", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
