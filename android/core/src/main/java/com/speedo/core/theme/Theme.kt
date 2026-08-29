package com.speedo.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpeedoLightColorScheme = lightColorScheme(
    primary = SpeedoOrange,
    onPrimary = SpeedoWhite,
    primaryContainer = SpeedoOrangeContainer,
    onPrimaryContainer = SpeedoOnOrangeContainer,
    secondary = SpeedoAmber,
    onSecondary = SpeedoWhite,
    secondaryContainer = SpeedoAmberContainer,
    onSecondaryContainer = SpeedoTextPrimary,
    tertiary = SpeedoInfo,
    onTertiary = SpeedoWhite,
    tertiaryContainer = SpeedoInfoContainer,
    onTertiaryContainer = SpeedoTextPrimary,
    background = SpeedoBackground,
    onBackground = SpeedoTextPrimary,
    surface = SpeedoSurface,
    onSurface = SpeedoTextPrimary,
    surfaceVariant = SpeedoSurfaceVariant,
    onSurfaceVariant = SpeedoTextSecondary,
    outline = SpeedoDivider,
    error = SpeedoError,
    onError = SpeedoWhite,
    errorContainer = SpeedoErrorContainer,
    onErrorContainer = SpeedoError
)

@Composable
fun SpeedoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = SpeedoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SpeedoWhite.toArgb()
                window.navigationBarColor = SpeedoWhite.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpeedoTypography,
        shapes = SpeedoShapes,
        content = content
    )
}
