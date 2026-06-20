package com.fluidscan.pro.ui.screens.ocr.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

/** Reveals [text] one character at a time — the "smart file name" typewriter effect. */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    charDelayMs: Long = 38L
) {
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        shown = 0
        for (i in 1..text.length) {
            shown = i
            delay(charDelayMs)
        }
    }
    Text(text = text.take(shown), modifier = modifier, style = style)
}
