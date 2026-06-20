package com.fluidscan.pro.ui.screens.utilities.batch.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fluidscan.pro.domain.model.BatchItem
import com.fluidscan.pro.domain.model.BatchStage

/**
 * Visual assembly line: one lane per [BatchStage]. Items appear as chips in the lane matching
 * their current stage and animate (placement + colour) as they advance down the line.
 */
@Composable
fun AssemblyLine(
    items: List<BatchItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BatchStage.entries.forEach { stage ->
            StageLane(stage = stage, items = items.filter { it.stage == stage })
        }
    }
}

@Composable
private fun StageLane(stage: BatchStage, items: List<BatchItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.width(150.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(stage.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(stage.label(), style = MaterialTheme.typography.labelLarge)
        }

        LazyRow(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ItemChip(item = item, stage = stage, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ItemChip(item: BatchItem, stage: BatchStage, modifier: Modifier = Modifier) {
    val target = if (stage == BatchStage.DONE) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondaryContainer
    val color by animateColorAsState(targetValue = target, label = "chipColor")
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(item.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun BatchStage.label(): String = when (this) {
    BatchStage.QUEUED -> "Queued"
    BatchStage.CLEANING -> "Cleaning"
    BatchStage.RECOGNIZING -> "Reading"
    BatchStage.PACKAGING -> "Packaging"
    BatchStage.DONE -> "Done"
}

private fun BatchStage.icon(): ImageVector = when (this) {
    BatchStage.QUEUED -> Icons.Filled.HourglassEmpty
    BatchStage.CLEANING -> Icons.Filled.AutoFixHigh
    BatchStage.RECOGNIZING -> Icons.Filled.TextFields
    BatchStage.PACKAGING -> Icons.Filled.PictureAsPdf
    BatchStage.DONE -> Icons.Filled.CheckCircle
}
