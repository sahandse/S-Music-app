package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

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
    themeMode: String = "dark", // "light", "dark", "amoled", "glass"
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
            surface = Color(0xF0FFFFFF),
            onPrimary = Color.White,
            surfaceVariant = Color(0xFFE8EBF1),
            onSurface = Color(0xFF17171D),
            onBackground = Color(0xFF17171D)
        )
        else -> DarkColorScheme
    }

    val appShapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )

    CompositionLocalProvider(LocalAppFontFamily provides appFontFamily(lang)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(lang),
            shapes = appShapes,
            content = content
        )
    }
}
