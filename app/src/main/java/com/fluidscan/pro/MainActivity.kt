package com.fluidscan.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fluidscan.pro.ui.screens.scanner.ScannerScreen
import com.fluidscan.pro.ui.theme.FluidScanTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FluidScanTheme {
                // Phase 1 entrypoint — full navigation graph arrives in Phase 3.
                ScannerScreen(onFinished = { /* Phase 2: hand pages to the PDF editor */ })
            }
        }
    }
}
