package com.kartak.autosms.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val KartakColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF334155),
    onSecondary = Color.White,
    background = Color(0xFFF6F8F7),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEDF2EF),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF991B1B),
    outline = Color(0xFFE2E8E5)
)

private val KartakTypography = Typography(
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun KartakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KartakColorScheme,
        typography = KartakTypography,
        content = content
    )
}
