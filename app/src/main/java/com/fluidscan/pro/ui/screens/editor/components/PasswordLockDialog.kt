package com.fluidscan.pro.ui.screens.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.fluidscan.pro.R

/**
 * Password-protection dialog. The header runs the **key-turn / locking micro Lottie**
 * (res/raw/lock_key.json) that plays once when a valid password is entered, then commits.
 */
@Composable
fun PasswordLockDialog(
    isCurrentlyProtected: Boolean,
    onConfirm: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var locking by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lock_key))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = locking,
        iterations = 1
    )

    // When the key-turn animation finishes, commit the password (off the composition pass).
    LaunchedEffect(locking, progress) {
        if (locking && progress >= 0.99f) onConfirm(password)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password protection") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(visible = composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(96.dp)
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.length >= 4 && !locking,
                onClick = { locking = true } // play the key-turn, then commit
            ) { Text("Lock") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isCurrentlyProtected) {
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
