package com.allai.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.TextStyle
import androidx.compose.ui.text.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    body1 = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    displayLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
)

private object FontFamily {
    val Default = androidx.compose.ui.text.FontFamily.Default
}
