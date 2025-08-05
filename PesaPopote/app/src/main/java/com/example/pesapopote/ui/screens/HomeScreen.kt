package com.example.pesapopote.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pesapopote.ui.theme.PesaPopoteTheme
import com.example.pesapopote.ui.theme.backgroundLight
import com.example.pesapopote.ui.theme.primaryBlue

@Composable
fun HomeScreen(
    onLogout: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {}  // Added navigation callback here
) {
    var selectedIndex by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = backgroundLight,
        bottomBar = {
            NavigationBar(
                containerColor = backgroundLight,
                contentColor = primaryBlue
            ) {
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    icon = { Icon(Icons.Default.Send, contentDescription = "Send") },
                    label = { Text("Send") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = {
                        // Instead of switching tab, navigate to Wallet screen
                        onNavigateToWallet()
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                    label = { Text("Wallet") }
                )
                NavigationBarItem(
                    selected = selectedIndex == 3,
                    onClick = { selectedIndex = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedIndex) {
                0 -> HomeTab()
                1 -> SendScreen()
                // Remove WalletTab() because Wallet is now a separate screen
                3 -> SettingsScreen(
                    onBackPressed = { selectedIndex = 0 },
                    onLogout = {
                        onLogout()
                        selectedIndex = 0
                    }
                )
            }
        }
    }
}

@Composable
fun HomeTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("🏠 Home Tab", style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    PesaPopoteTheme {
        HomeScreen()
    }
}
