package com.allai.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GachaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7B2CBF),
    primaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF00D4FF),
    secondaryContainer = Color(0xFF006B9E),
    tertiary = Color(0xFFE879F9),
    background = Color(0xFF05050A),
    surface = Color(0xFF0F0A1E),
    surfaceVariant = Color(0xFF1A102E),
    error = Color(0xFFFF4D4D),
    onPrimary = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE8EAF6),
    onSurface = Color(0xFFFFFFFF),
    onError = Color(0xFF000000),
    onSurfaceVariant = Color(0xFFB0A8C8),
    outline = Color(0xFF4A148C),
    outlineVariant = Color(0xFF7B2CBF),
    inverseSurface = Color(0xFFE8EAF6),
    inverseOnSurface = Color(0xFF05050A),
    inversePrimary = Color(0xFFB388FF)
)

@Composable
fun AllaiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GachaDarkColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

val AllaiSurface = Color(0xFF0F0A1E)
val AllaiOnSurface = Color(0xFFFFFFFF)
val AllaiOnSurfaceVariant = Color(0xFFB0A8C8)
val AllaiPrimary = Color(0xFF7B2CBF)
val AllaiSecondary = Color(0xFF00D4FF)
val AllaiTertiary = Color(0xFFE879F9)
val AllaiBackground = Color(0xFF05050A)
