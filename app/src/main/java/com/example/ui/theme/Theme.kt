package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VexColorScheme = darkColorScheme(
    primary = VexCyan,
    onPrimary = VexDarkBackground,
    primaryContainer = VexPillTeal,
    onPrimaryContainer = VexCyan,
    secondary = VexCyanDim,
    onSecondary = VexDarkBackground,
    background = VexDarkBackground,
    onBackground = VexTextPrimary,
    surface = VexDarkBackground,
    onSurface = VexTextPrimary,
    surfaceVariant = VexSurfaceDark,
    onSurfaceVariant = VexTextMuted,
    outline = VexSurfaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VexDarkBackground.toArgb()
                window.navigationBarColor = VexDarkBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = VexColorScheme,
        typography = Typography,
        content = content
    )
}
