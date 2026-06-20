package com.fluidscan.pro.ui.screens.scanner.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluidscan.pro.core.util.toLiveColorFilter
import com.fluidscan.pro.domain.model.ScanFilter
import com.fluidscan.pro.ui.theme.FluidScanMotion

/**
 * Horizontal filmstrip of live filter previews. Each chip renders the *same* thumbnail
 * with that filter's GPU [androidx.compose.ui.graphics.ColorFilter] — so the user sees the
 * real effect with zero extra bitmaps. The selected chip springs up (scale) and gains an
 * accent ring; the main preview cross-fades separately (see `ScannerScreen`).
 */
@Composable
fun FilterFilmstrip(
    previewUri: Uri?,
    selected: ScanFilter,
    onSelect: (ScanFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(ScanFilter.entries, key = { it.name }) { filter ->
            val isSelected = filter == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1f,
                animationSpec = FluidScanMotion.Springs.snap(),
                label = "filterScale"
            )
            val ring by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "filterRing"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onSelect(filter) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                        .border(2.dp, ring, RoundedCornerShape(12.dp))
                ) {
                    if (previewUri != null) {
                        AsyncImage(
                            model = previewUri,
                            contentDescription = filter.displayName,
                            contentScale = ContentScale.Crop,
                            colorFilter = filter.toLiveColorFilter(),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                Text(
                    text = filter.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }
    }
}
