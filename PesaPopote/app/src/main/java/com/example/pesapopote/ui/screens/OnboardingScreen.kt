package com.example.pesapopote.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pesapopote.util.UserSessionManager

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(0) }

    val primaryBlue = Color(0xFF1E88E5)
    val accentGold = Color(0xFFFFC107)
    val backgroundLight = Color.White
    val textDark = Color(0xFF263238)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = step == 0,
            enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.9f)
        ) {
            Text(
                text = "Welcome to PesaPopote",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = primaryBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = step == 1,
            enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.9f)
        ) {
            Text(
                text = "Send & Receive Money Anywhere\nin Kenya 🇰🇪, Uganda 🇺🇬, Nigeria 🇳🇬, Ghana 🇬🇭, South Africa 🇿🇦, Rwanda 🇷🇼 & Tanzania 🇹🇿",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textDark,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = step == 2,
            enter = fadeIn(tween(600)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.9f)
        ) {
            Text(
                text = "Fast, Secure, and Low-Cost Transfers\nPowered by Stellar Blockchain",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textDark,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(visible = step == 3) {
            Button(
                onClick = {
                    UserSessionManager.setFirstLaunchDone(context)
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accentGold)
            ) {
                Text(
                    text = "Get Started",
                    color = primaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step in 1..3) {
                TextButton(onClick = { step-- }) {
                    Text("Back", color = primaryBlue)
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp))
            }

            if (step < 3) {
                TextButton(onClick = { step++ }) {
                    Text("Next", color = primaryBlue)
                }
            }
        }
    }
}
