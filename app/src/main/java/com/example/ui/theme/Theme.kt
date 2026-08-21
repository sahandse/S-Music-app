package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Monochrome (white-on-black / black-on-white), flat cards, no glass blur — Aparat Music's look
private val AparatDarkColorScheme = darkColorScheme(
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

private val AparatLightColorScheme = lightColorScheme(
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

@Composable
fun MyApplicationTheme(
    themeMode: String = "aparat", // "aparat" (dark) or "aparat-light"
    lang: String = "fa", // "fa" uses Vazirmatn, "en" uses the Poppins Google font
    content: @Composable () -> Unit,
) {
    val colorScheme = if (themeMode == "aparat-light") AparatLightColorScheme else AparatDarkColorScheme

    CompositionLocalProvider(
        LocalAppFontFamily provides appFontFamily(lang),
        LocalFlatTheme provides true
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(lang),
            content = content
        )
    }
}
