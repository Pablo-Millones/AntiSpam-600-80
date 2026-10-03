package com.antispam.blocker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Color.Black,
    secondary = Indigo500,
    onSecondary = Color.White,
    tertiary = Amber500,
    background = Color(0xFF0B1120),
    surface = Color(0xFF161F33),
    surfaceVariant = Color(0xFF23304A),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = Slate900,
    onPrimary = Color.White,
    secondary = Emerald600,
    onSecondary = Color.White,
    tertiary = Red500,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun SpamBlockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
