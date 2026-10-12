package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EnginePrimary,
    onPrimary = Color.Black,
    primaryContainer = EnginePrimaryContainer,
    onPrimaryContainer = EnginePrimary,
    secondary = EngineSecondary,
    onSecondary = Color.Black,
    tertiary = EngineTertiary,
    onTertiary = Color.Black,
    background = EngineBackground,
    onBackground = EngineTextPrimary,
    surface = EngineSurface,
    onSurface = EngineTextPrimary,
    surfaceVariant = EngineSurfaceVariant,
    onSurfaceVariant = EngineTextSecondary,
    error = EngineError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
