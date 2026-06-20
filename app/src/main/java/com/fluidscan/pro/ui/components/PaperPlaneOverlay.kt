package com.fluidscan.pro.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.fluidscan.pro.R

/**
 * Plays the paper-plane "takeoff" Lottie once whenever [playKey] changes (e.g. on a Share/Print
 * tap). Render it as a full-size overlay above the triggering content.
 */
@Composable
fun PaperPlaneOverlay(
    playKey: Int,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {}
) {
    if (playKey <= 0) return
    key(playKey) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.paper_plane))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            isPlaying = true,
            iterations = 1,
            restartOnPlay = true
        )
        LottieAnimation(composition = composition, progress = { progress }, modifier = modifier)

        LaunchedEffect(progress) {
            if (progress >= 0.99f) onFinished()
        }
    }
}
