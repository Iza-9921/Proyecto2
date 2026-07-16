package com.example.todoaccesible.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandPink,
    onPrimary = Color(0xFFFFFFFF),
    secondary = RequiredNavy,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = NivelOro,
    background = SurfaceLight,
    surface = SurfaceLight,
    error = EstadoNoCumple
)

private val DarkColors = darkColorScheme(
    primary = BrandPink,
    onPrimary = Color(0xFF3B0021),
    secondary = RequiredNavyLight,
    onSecondary = Color(0xFF00234B),
    tertiary = NivelOro,
    background = SurfaceDark,
    surface = SurfaceDark,
    error = Color(0xFFFFB4AB)
)

@Composable
fun TodoAccesibleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TodoAccesibleTypography,
        content = content
    )
}
