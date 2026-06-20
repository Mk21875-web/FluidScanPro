package com.fluidscan.pro.ui.screens.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fluidscan.pro.BuildConfig
import com.fluidscan.pro.core.haptics.HapticEngine
import com.fluidscan.pro.domain.model.ShapeKind
import com.fluidscan.pro.domain.model.StampSource
import com.fluidscan.pro.ui.screens.editor.components.AnnotationCanvas
import com.fluidscan.pro.ui.screens.editor.components.PageManagerStrip
import com.fluidscan.pro.ui.screens.editor.components.PasswordLockDialog
import com.fluidscan.pro.ui.screens.editor.components.SignatureStampLayer
import com.fluidscan.pro.ui.screens.editor.components.nearestAnnotationId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

/**
 * Phase 2 — PDF Construction & Full Editor. Stacks the page image, the ink/shape
 * [AnnotationCanvas], and the [SignatureStampLayer] (stamps/text). Hosts the tool palette,
 * the spread/collapse [PageManagerStrip], the key-turn [PasswordLockDialog] and PDF export.
 */
@Composable
fun EditorScreen(
    initialImageUris: List<Uri> = emptyList(),
    title: String = "Scan",
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = remember { HapticEngine(context.applicationContext) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (state.document.pages.isEmpty()) {
            if (initialImageUris.isNotEmpty()) {
                viewModel.onIntent(EditorIntent.LoadPages(initialImageUris, title))
            } else {
                viewModel.onIntent(EditorIntent.LoadFromScan)
            }
        }
    }

    val addPageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onIntent(EditorIntent.AddPage(it)) } }

    val signatureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onIntent(EditorIntent.AddStamp(StampSource.Image(it.toString()), androidx.compose.ui.geometry.Offset(0.5f, 0.5f))) } }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                EditorEffect.SealPressHaptic -> haptics.sealPress()
                EditorEffect.LockHaptic -> haptics.detectionSuccess()
                is EditorEffect.Error -> scope.launch { snackbar.showSnackbar(effect.message) }
                is EditorEffect.Exported -> sharePdf(context, effect.uri)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        EditorTopBar(
            title = state.document.title,
            isProtected = state.document.isPasswordProtected,
            isExporting = state.isExporting,
            onBack = onBack,
            onLock = { viewModel.onIntent(EditorIntent.RequestPasswordDialog) },
            onExport = { viewModel.onIntent(EditorIntent.Export) }
        )

        ToolPalette(
            tool = state.tool,
            onTool = { viewModel.onIntent(EditorIntent.SelectTool(it)) },
            colorArgb = state.colorArgb,
            onColor = { viewModel.onIntent(EditorIntent.SelectColor(it)) }
        )

        AnimatedVisibility(visible = state.tool == EditorTool.SHAPE) {
            ShapeKindRow(selected = state.shapeKind, onSelect = { viewModel.onIntent(EditorIntent.SelectShapeKind(it)) })
        }
        AnimatedVisibility(visible = state.tool == EditorTool.STAMP) {
            StampPickerRow(
                onBuiltin = { label, tint ->
                    viewModel.onIntent(EditorIntent.AddStamp(StampSource.Builtin(label, tint), androidx.compose.ui.geometry.Offset(0.5f, 0.5f)))
                },
                onSignature = { signatureLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
        }

        // Page editing surface.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val page = state.page
            if (page != null) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = page.imageUri,
                        contentDescription = "Page",
                        modifier = Modifier.fillMaxSize()
                    )
                    AnnotationCanvas(
                        page = page,
                        tool = state.tool,
                        colorArgb = state.colorArgb,
                        strokeWidth = state.strokeWidth,
                        liveInk = state.liveInk,
                        liveShape = state.liveShape,
                        onInkStart = { viewModel.onIntent(EditorIntent.InkStart(it)) },
                        onInkMove = { viewModel.onIntent(EditorIntent.InkMove(it)) },
                        onInkEnd = { viewModel.onIntent(EditorIntent.InkEnd) },
                        onShapeStart = { viewModel.onIntent(EditorIntent.ShapeStart(it)) },
                        onShapeMove = { viewModel.onIntent(EditorIntent.ShapeMove(it)) },
                        onShapeEnd = { viewModel.onIntent(EditorIntent.ShapeEnd) },
                        onTapPlace = { p ->
                            if (state.tool == EditorTool.TEXT) viewModel.onIntent(EditorIntent.AddText(p))
                        },
                        onEraseAt = { p ->
                            nearestAnnotationId(page, p)?.let { viewModel.onIntent(EditorIntent.DeleteAnnotation(it)) }
                        }
                    )
                    SignatureStampLayer(
                        page = page,
                        selectedId = state.selectedAnnotationId,
                        onMoveStamp = { id, c -> viewModel.onIntent(EditorIntent.MoveStamp(id, c)) },
                        onDropStamp = { viewModel.onIntent(EditorIntent.DropStamp(it)) },
                        onEditText = { id, v -> viewModel.onIntent(EditorIntent.EditText(id, v)) },
                        onSelect = { viewModel.onIntent(EditorIntent.SelectAnnotation(it)) }
                    )
                }
            } else {
                Text("No pages", color = MaterialTheme.colorScheme.onBackground)
            }
        }

        PageManagerStrip(
            pages = state.document.pages,
            currentIndex = state.currentPage,
            onSelect = { viewModel.onIntent(EditorIntent.SelectPage(it)) },
            onRemove = { viewModel.onIntent(EditorIntent.RemovePage(it)) },
            onAddPage = { addPageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(96.dp).padding(vertical = 8.dp)
        )

        SnackbarHost(snackbar)
    }

    if (state.showPasswordDialog) {
        PasswordLockDialog(
            isCurrentlyProtected = state.document.isPasswordProtected,
            onConfirm = { viewModel.onIntent(EditorIntent.SetPassword(it)) },
            onRemove = { viewModel.onIntent(EditorIntent.RemovePassword) },
            onDismiss = { viewModel.onIntent(EditorIntent.DismissPasswordDialog) }
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    isProtected: Boolean,
    isExporting: Boolean,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            icon = Icons.Filled.Lock,
            desc = "Password",
            onClick = onLock,
            tint = if (isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isExporting) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp).padding(end = 8.dp))
        } else {
            IconButton(Icons.Filled.Share, "Export PDF", onExport)
        }
    }
}

@Composable
private fun ToolPalette(
    tool: EditorTool,
    onTool: (EditorTool) -> Unit,
    colorArgb: Long,
    onColor: (Long) -> Unit
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(EditorTool.entries) { t ->
                ToolChip(label = t.name, selected = t == tool, onClick = { onTool(t) })
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PALETTE) { argb ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(argb.toInt()))
                        .border(
                            width = if (argb == colorArgb) 3.dp else 1.dp,
                            color = if (argb == colorArgb) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onColor(argb) }
                )
            }
        }
    }
}

@Composable
private fun ShapeKindRow(selected: ShapeKind, onSelect: (ShapeKind) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ShapeKind.entries) { kind ->
            ToolChip(label = kind.name, selected = kind == selected, onClick = { onSelect(kind) })
        }
    }
}

@Composable
private fun StampPickerRow(onBuiltin: (String, Long) -> Unit, onSignature: () -> Unit) {
    val builtins = listOf(
        "APPROVED" to 0xFF1B873F,
        "CONFIDENTIAL" to 0xFFD32F2F,
        "DRAFT" to 0xFF616161,
        "PAID" to 0xFF2D6CFF
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onSignature() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Text(" Signature", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        items(builtins) { (label, tint) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color(tint.toInt()), RoundedCornerShape(8.dp))
                    .clickable { onBuiltin(label, tint) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(label, color = Color(tint.toInt()), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ToolChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.lowercase().replaceFirstChar { it.uppercase() },
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun IconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint)
    }
}

private val PALETTE = listOf(
    0xFF000000, 0xFFFFFFFF, 0xFFD32F2F, 0xFF2D6CFF, 0xFF1B873F, 0xFFFFC107, 0xFF7C4DFF
)

/** Shares the exported PDF via a content:// URI (FileProvider). */
private fun sharePdf(context: android.content.Context, uri: Uri) {
    val file = File(requireNotNull(uri.path))
    val shareUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
