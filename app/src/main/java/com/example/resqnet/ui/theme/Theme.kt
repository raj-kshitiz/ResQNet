package com.example.resqnet.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light scheme: calm sky-blue tint on background gives a "safe, clear sky" feeling.
 * Red primary is used sparingly — only on SOS-related actions.
 * Blue secondary carries navigation and volunteer features (trust, reliability).
 */
private val LightColorScheme = lightColorScheme(
    primary              = Red700,
    onPrimary            = Color.White,
    primaryContainer     = Red50,
    onPrimaryContainer   = Red900,

    secondary            = Blue800,
    onSecondary          = Color.White,
    secondaryContainer   = Blue50,
    onSecondaryContainer = Blue900,

    tertiary             = Green700,
    onTertiary           = Color.White,
    tertiaryContainer    = Green50,
    onTertiaryContainer  = Green900,

    background           = Color(0xFFF0F4FF), // subtle sky-blue tint — calm and safe
    onBackground         = Color(0xFF0F172A),
    surface              = Color.White,
    onSurface            = Color(0xFF0F172A),
    surfaceVariant       = Color(0xFFEEF2FF), // light indigo tint — cards float on background
    onSurfaceVariant     = Color(0xFF44546A),
    outline              = Color(0xFFBCC8DC),
    outlineVariant       = Color(0xFFDDE3F0),

    error                = Red700,
    onError              = Color.White,
    errorContainer       = Red50,
    onErrorContainer     = Red900,
)

/**
 * Dark scheme: deep navy (GitHub-style) — calm like a night sky, easy on the eyes.
 * Soft warm-coral red replaces harsh bright red, reducing alarm fatigue.
 * GitHub blue and mint green provide clear, readable accent colors.
 */
private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFFFF8A80), // warm coral-red — calm emergency
    onPrimary            = Color(0xFF690005),
    primaryContainer     = Color(0xFF520000),
    onPrimaryContainer   = Color(0xFFFFDAD6),

    secondary            = Color(0xFF79B8FF), // GitHub's trusted blue
    onSecondary          = Color(0xFF003258),
    secondaryContainer   = Color(0xFF003A6E),
    onSecondaryContainer = Color(0xFFD2E4FF),

    tertiary             = Color(0xFF6EE7B7), // mint-green — life, success, helping
    onTertiary           = Color(0xFF003826),
    tertiaryContainer    = Color(0xFF004D36),
    onTertiaryContainer  = Color(0xFFA7F3D0),

    background           = Navy950,
    onBackground         = NavyText,
    surface              = Navy900,
    onSurface            = NavyText,
    surfaceVariant       = Navy800,
    onSurfaceVariant     = NavyTextMuted,
    outline              = Navy700,
    outlineVariant       = Navy800,

    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

@Composable
fun ResQNetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = Shapes,
        content     = content
    )
}
