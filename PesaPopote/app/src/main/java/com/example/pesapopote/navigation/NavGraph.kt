package com.example.pesapopote.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.pesapopote.ui.screens.HomeScreen
import com.example.pesapopote.ui.screens.LoginScreen
import com.example.pesapopote.ui.screens.OnboardingScreen
import com.example.pesapopote.ui.screens.RegisterScreen
import com.example.pesapopote.ui.screens.SendScreen
import com.example.pesapopote.ui.screens.SettingsScreen
import com.example.pesapopote.ui.screens.WalletScreen
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
                    onLoginSuccess = {
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
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToSend = {
                        navController.navigate("send")
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
                    },
                    onSendClick = { address, amount ->
                    }
                )
            }

            composable("wallet") {
                WalletScreen(navController = navController)
            }
        }
    }
}
