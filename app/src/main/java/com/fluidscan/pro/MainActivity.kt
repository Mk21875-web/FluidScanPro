package com.fluidscan.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fluidscan.pro.ui.screens.dashboard.DashboardScreen
import com.fluidscan.pro.ui.screens.editor.EditorScreen
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
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            onOpenScanner = { navController.navigate("scanner") },
                            onOpenEditor = { navController.navigate("editor") }
                        )
                    }
                    composable("scanner") {
                        ScannerScreen(
                            onFinished = { navController.navigate("editor") }
                        )
                    }
                    composable("editor") {
                        // Pages are pulled from the scanner/import→editor handoff (ScanHandoff).
                        EditorScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
