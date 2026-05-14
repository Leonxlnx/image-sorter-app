package com.leonxlnx.imagesorter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

sealed class ThemeMode(val id: String) {
    data object System : ThemeMode("system")
    data object Light : ThemeMode("light")
    data object Dark : ThemeMode("dark")
}

private val BrandPrimary = Color(0xFF22D3EE)
private val BrandPrimaryDark = Color(0xFF06B6D4)
private val BrandSecondary = Color(0xFFE879F9)
private val BrandSurface = Color(0xFF0F172A)

private val LightColors = lightColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color.White,
    secondary = BrandSecondary,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFF0F172A),
    secondary = BrandSecondary,
    background = BrandSurface,
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    onSurface = Color(0xFFE5E7EB),
)

@Composable
fun ImageSorterTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
