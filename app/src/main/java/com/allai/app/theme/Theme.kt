package com.allai.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GachaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7B2CBF),
    primaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF00D4FF),
    secondaryContainer = Color(0xFF006B9E),
    background = Color(0xFF05050A),
    surface = Color(0xFF0F0A1E),
    error = Color(0xFFFF4D4D),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE8EAF6),
    onSurface = Color(0xFFFFFFFF),
    onError = Color(0xFF000000),
    surfaceVariant = Color(0xFF1A102E),
    onSurfaceVariant = Color(0xFFB0A8C8),
    outline = Color(0xFF4A148C),
    inverseSurface = Color(0xFFE8EAF6),
    inverseOnSurface = Color(0xFF05050A),
    inversePrimary = Color(0xFFB388FF)
)

@Composable
fun AllaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GachaDarkColorScheme,
        typography = MaterialTypography(),
        content = content
    )
}
