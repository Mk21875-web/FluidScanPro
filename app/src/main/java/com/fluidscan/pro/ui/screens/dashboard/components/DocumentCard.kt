package com.fluidscan.pro.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluidscan.pro.domain.model.Document
import com.fluidscan.pro.domain.model.SyncState
import com.fluidscan.pro.ui.screens.dashboard.ViewMode

@Composable
fun DocumentCard(
    document: Document,
    viewMode: ViewMode,
    onClick: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (viewMode == ViewMode.GRID) GridContent(document, onSync) else ListContent(document, onSync)
    }
}

@Composable
private fun GridContent(document: Document, onSync: () -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.74f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Thumbnail(document)
            SyncBadge(
                document = document,
                onSync = onSync,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
            if (document.isPasswordProtected) {
                LockBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(
                document.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${document.pageCount} pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ListContent(document: Document, onSync: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Thumbnail(document)
            if (document.isPasswordProtected) LockBadge(Modifier.align(Alignment.TopStart).padding(2.dp), small = true)
        }
        Column(Modifier.weight(1f)) {
            Text(
                document.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${document.pageCount} pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SyncBadge(document = document, onSync = onSync)
    }
}

@Composable
private fun Thumbnail(document: Document) {
    val model = document.thumbnailUri
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = document.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun SyncBadge(document: Document, onSync: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(34.dp), contentAlignment = Alignment.Center) {
        when (document.syncState) {
            SyncState.SYNCING -> {
                WaveSyncIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(50))
                )
            }
            SyncState.SYNCED -> Icon(
                Icons.Filled.CloudDone,
                contentDescription = "Synced",
                tint = MaterialTheme.colorScheme.primary
            )
            else -> Icon(
                Icons.Filled.CloudUpload,
                contentDescription = "Sync now",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onSync() }
            )
        }
    }
}

@Composable
private fun LockBadge(modifier: Modifier = Modifier, small: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(3.dp)
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = "Protected",
            tint = Color.White,
            modifier = Modifier.size(if (small) 12.dp else 16.dp)
        )
    }
}
