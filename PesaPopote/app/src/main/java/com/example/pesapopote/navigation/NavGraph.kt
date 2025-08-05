package com.example.pesapopote.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pesapopote.ui.screens.*
import com.example.pesapopote.util.UserSessionManager

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val context = navController.context
    val sessionManager = UserSessionManager(context)

    fun performLogout() {
        sessionManager.clearSession()
        navController.navigate("login") {
            popUpTo("home") { inclusive = true }
        }
    }

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate("register") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }

            composable("login") {
                LoginScreen(
                    onLogin = { email, password ->
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    onLogout = {
                        performLogout()
                    },
                    onNavigateToWallet = {
                        navController.navigate("wallet")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        performLogout()
                    }
                )
            }

            composable("send") {
                SendScreen(
                    onRechargeClick = {
                        // TODO: Navigate to recharge if implemented
                    },
                    onSendClick = { address, amount ->
                        // TODO: Handle sending logic
                    }
                )
            }

            composable("wallet") {
                WalletScreen(navController = navController)
            }
        }
    }
}
