package com.example.pesapopote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.pesapopote.navigation.NavGraph
import com.example.pesapopote.ui.theme.PesaPopoteTheme
import com.example.pesapopote.util.UserSessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PesaPopoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination = determineStartDestination()
                    val navController = rememberNavController()
                    NavGraph(navController = navController, startDestination = startDestination)
                }
            }
        }
    }

    private fun determineStartDestination(): String {
        val sessionManager = UserSessionManager(this)

        return when {
            UserSessionManager.isFirstLaunch(this) -> {
                UserSessionManager.setFirstLaunchDone(this)
                "onboarding"
            }
            sessionManager.isLoggedIn() -> {
                "home"
            }
            else -> {
                "register"
            }
        }
    }
}
