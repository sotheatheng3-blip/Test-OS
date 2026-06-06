package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DeviceViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.FirmwareScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: DeviceViewModel = viewModel()
        var currentTab by remember { mutableStateOf("dashboard") } // "dashboard", "scanner", "control", "firmware"
        
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            NavigationBar(
              containerColor = BentoGreySurface,
              tonalElevation = 8.dp,
              windowInsets = WindowInsets.navigationBars
            ) {
              // Dashboard Tab
              NavigationBarItem(
                selected = currentTab == "dashboard",
                onClick = { currentTab = "dashboard" },
                label = { Text("Hub", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.DeveloperBoard, contentDescription = "Dashboard") },
                colors = navigationBarColorsStyle()
              )

              // Connect Scanner Tab
              NavigationBarItem(
                selected = currentTab == "scanner",
                onClick = { currentTab = "scanner" },
                label = { Text("Scanner", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.BluetoothSearching, contentDescription = "Scan") },
                colors = navigationBarColorsStyle()
              )

              // Realtime Control Tab
              NavigationBarItem(
                selected = currentTab == "control",
                onClick = { currentTab = "control" },
                label = { Text("Control", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Tune, contentDescription = "Controller") },
                colors = navigationBarColorsStyle()
              )

              // Automated Firmware OTA Tab
              NavigationBarItem(
                selected = currentTab == "firmware",
                onClick = { currentTab = "firmware" },
                label = { Text("Updater", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "OTA Update") },
                colors = navigationBarColorsStyle()
              )
            }
          },
          contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
          Surface(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .background(BentoBg),
            color = BentoBg
          ) {
            when (currentTab) {
              "dashboard" -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToScan = { currentTab = "scanner" },
                onNavigateToControl = { currentTab = "control" }
              )
              "scanner" -> ScannerScreen(
                viewModel = viewModel
              )
              "control" -> ControllerScreen(
                viewModel = viewModel,
                onNavigateToScan = { currentTab = "scanner" }
              )
              "firmware" -> FirmwareScreen(
                viewModel = viewModel,
                onNavigateToScan = { currentTab = "scanner" }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun navigationBarColorsStyle() = NavigationBarItemDefaults.colors(
  selectedIconColor = BentoTextDeep,
  selectedTextColor = BentoPurpleAccent,
  indicatorColor = BentoPurpleBg,
  unselectedIconColor = BentoTextSecondary,
  unselectedTextColor = BentoTextSecondary
)

// Legacy Greeting helper for retro-compatibility unit and screenshot testing
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", color = Color.White, modifier = modifier)
}
