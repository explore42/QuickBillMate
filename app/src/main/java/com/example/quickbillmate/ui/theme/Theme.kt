package com.example.quickbillmate.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val BrandGold = Color(0xFFB8942E)
val BrandGoldLight = Color(0xFFE2C968)

private val LightColors = lightColorScheme(
    primary = BrandGold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4E8C8),
    onPrimaryContainer = Color(0xFF3D3108),
    secondary = Color(0xFF6E6550),
    onSecondary = Color.White,
    background = Color(0xFFFFF9EC),
    onBackground = Color(0xFF211B0E),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF211B0E),
    surfaceVariant = Color(0xFFF0EBDD),
    onSurfaceVariant = Color(0xFF4F4A3E),
)

private val DarkColors = darkColorScheme(
    primary = BrandGoldLight,
    onPrimary = Color(0xFF3D3108),
    primaryContainer = Color(0xFF574808),
    onPrimaryContainer = Color(0xFFFFE9A8),
    secondary = Color(0xFFD8CCAE),
    onSecondary = Color(0xFF3B3525),
    background = Color(0xFF1C1B16),
    onBackground = Color(0xFFE9E2D1),
    surface = Color(0xFF24231D),
    onSurface = Color(0xFFE9E2D1),
    surfaceVariant = Color(0xFF4A463B),
    onSurfaceVariant = Color(0xFFCBC4B4),
)

@Composable
fun QuickBillMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
