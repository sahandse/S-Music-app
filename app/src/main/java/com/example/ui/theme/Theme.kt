package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = AppleRed,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = GlassCardLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = AppleRed,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = GlassCardDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val AmoledColorScheme = darkColorScheme(
    primary = AppleRed,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = BackgroundAmoled,
    surface = Color.Black, // True black background, but we can overlay with GlassCardDark dynamically
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "dark", // "light", "dark", "amoled", "glass", "spotify", "aparat", "aparat-light"
    themeAccent: String = "red", // "red", "blue", "green", "orange", "purple", "pink"
    lang: String = "fa", // "fa" uses Vazirmatn, "en" uses the Poppins Google font
    content: @Composable () -> Unit,
) {
    val accentColor = when (themeAccent) {
        "blue" -> Color(0xFF007AFF)
        "green" -> Color(0xFF34C759)
        "orange" -> Color(0xFFFF9500)
        "purple" -> Color(0xFFAF52DE)
        "pink" -> Color(0xFFFF2D55)
        else -> Color(0xFFFA2D48) // AppleRed
    }

    val LightColorScheme = lightColorScheme(
        primary = accentColor,
        secondary = PurpleGrey40,
        tertiary = Pink40,
        background = Color.White,
        surface = GlassCardLight,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.Black,
        onSurface = Color.Black
    )

    val DarkColorScheme = darkColorScheme(
        primary = accentColor,
        secondary = PurpleGrey80,
        tertiary = Pink80,
        background = BackgroundDark,
        surface = GlassCardDark,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )

    val AmoledColorScheme = darkColorScheme(
        primary = accentColor,
        secondary = PurpleGrey80,
        tertiary = Pink80,
        background = BackgroundAmoled,
        surface = Color.Black,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )

    val colorScheme = when (themeMode) {
        "light" -> LightColorScheme
        "amoled" -> AmoledColorScheme
        "glass" -> LightColorScheme.copy(
            background = Color(0xFFF0F2F6),
            surface = Color(0xC0FFFFFF),
            onSurface = Color.Black,
            onBackground = Color.Black
        )
        // Spotify preset always uses Spotify's own green, regardless of the accent picker
        "spotify" -> darkColorScheme(
            primary = SpotifyGreen,
            secondary = SpotifyGreen,
            tertiary = SpotifyGreen,
            background = SpotifyBackground,
            surface = SpotifySurface,
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
        // Aparat Music preset: monochrome (white-on-black / black-on-white), flat cards, no glass blur
        "aparat" -> darkColorScheme(
            primary = Color.White,
            secondary = AparatTextDimDark,
            tertiary = Color.White,
            background = AparatBackgroundDark,
            surface = AparatSurfaceDark,
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
        "aparat-light" -> lightColorScheme(
            primary = Color.Black,
            secondary = AparatTextDimLight,
            tertiary = Color.Black,
            background = AparatBackgroundLight,
            surface = AparatSurfaceLight,
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
        else -> DarkColorScheme
    }

    CompositionLocalProvider(LocalAppFontFamily provides appFontFamily(lang)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(lang),
            content = content
        )
    }
}
