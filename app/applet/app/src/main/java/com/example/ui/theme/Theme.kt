package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = OledBlack,
    primaryContainer = ChipSelectedBackground,
    onPrimaryContainer = NeonPurple,
    secondary = CyberCyan,
    onSecondary = OledBlack,
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = CyberCyan,
    tertiary = UpvoteOrange,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = DarkSurfaceLow,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceMedium,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkSurfaceHigh
)

@Composable
fun OmniBoxTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = OledBlack.toArgb()
                window.navigationBarColor = OledBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
