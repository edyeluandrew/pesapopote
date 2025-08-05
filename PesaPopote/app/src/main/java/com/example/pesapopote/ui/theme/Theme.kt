package com.example.pesapopote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

// Import from Color.kt
// You already have these colors defined — just use them
private val LightColorScheme = lightColorScheme(
    primary = primaryBlue,
    secondary = accentGold,
    background = backgroundLight,
    surface = backgroundLight,
    onPrimary = backgroundLight,
    onSecondary = backgroundLight,
    onBackground = textDark,
    onSurface = textDark,
)

@Composable
fun PesaPopoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
