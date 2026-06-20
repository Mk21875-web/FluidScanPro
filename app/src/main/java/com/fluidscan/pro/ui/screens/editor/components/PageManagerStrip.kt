package com.fluidscan.pro.ui.screens.editor.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluidscan.pro.domain.model.EditablePage
import com.fluidscan.pro.ui.theme.FluidScanMotion

/**
 * Filmstrip of the document's pages with **spread/collapse** add/remove animations:
 * inserting a page springs it in (scale + fade) and shifts neighbours apart via
 * `Modifier.animateItem`; removing one collapses the gap as the list re-lays-out.
 */
@Composable
fun PageManagerStrip(
    pages: List<EditablePage>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onRemove: (String) -> Unit,
    onAddPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
            val selected = index == currentIndex
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.06f else 1f,
                animationSpec = FluidScanMotion.Springs.snap(),
                label = "pageScale"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .animateItem(
                        // Spread/collapse spring for placement; fade for appear/vanish.
                        placementSpec = FluidScanMotion.Springs.OffsetSpring,
                        fadeInSpec = null,
                        fadeOutSpec = null
                    )
                    .scale(scale)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(index) }
            ) {
                AsyncImage(
                    model = page.displayImageUri(),
                    contentDescription = "Page ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxHeight()
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { onRemove(page.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                )
            }
        }
        item(key = "add") {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .animateItem(fadeInSpec = null, fadeOutSpec = null)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onAddPage() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add page", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun EditablePage.displayImageUri() = imageUri
