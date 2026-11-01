package com.allai.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography as MaterialTypography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.allai.app.R

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    primaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF0EA5E9),
    secondaryContainer = Color(0xFFCFFAFE),
    background = Color(0xFF0B1120),
    surface = Color(0xFF0F172A),
    error = Color(0xFFEF4444),
    onPrimary = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFF1E1B4B),
    onSecondary = Color(0xFF0B1120),
    onSecondaryContainer = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    onError = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0B1120),
    inversePrimary = Color(0xFF81ECCC)
)

private val LightColorScheme = lightColorScheme()

@Composable
fun AllaiTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (context.resources.configuration.uiMode and 
        android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
        android.content.res.Configuration.UI_MODE_NIGHT_YES) {
        DarkColorScheme
    } else {
        LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTypography(),
        content = content
    )
}
