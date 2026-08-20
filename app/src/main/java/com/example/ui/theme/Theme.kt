package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WhitePrimary,
    onPrimary = WhitePrimaryDark,
    primaryContainer = WhitePrimaryContainer,
    onPrimaryContainer = WhitePrimaryLight,
    secondary = Color(0xFFE4E4E7),
    onSecondary = Color(0xFF18181B),
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color(0xFFF4F4F5),
    tertiary = EmeraldAccent,
    onTertiary = Color(0xFF000000),
    background = VaultBackground,
    onBackground = TextPrimary,
    surface = VaultSurface,
    onSurface = TextPrimary,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VaultCardBorder,
    error = RedDanger,
    onError = Color(0xFF000000)
)

private val LightColorScheme = DarkColorScheme // Vault is best in deep secure dark mode for high security privacy

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep private vault dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
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

