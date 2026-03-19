package com.vlsch.fluiddarkmode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Moss,
    onPrimary = Ink,
    secondary = NightMist,
    onSecondary = Ink,
    tertiary = Dune,
    onTertiary = Ink,
    background = Ink,
    onBackground = Ivory,
    surface = Slate,
    onSurface = Ivory,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = NightMist,
    surfaceContainerLowest = ColorTokens.DarkSurfaceLowest,
    surfaceContainer = ColorTokens.DarkSurface,
    surfaceContainerHigh = ColorTokens.DarkSurfaceHigh,
    secondaryContainer = Forest,
    onSecondaryContainer = Ivory,
    tertiaryContainer = ColorTokens.DarkTag,
    onTertiaryContainer = Ivory,
)

private val LightColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Ivory,
    secondary = Cocoa,
    onSecondary = Ivory,
    tertiary = Dune,
    onTertiary = Cocoa,
    background = Cloud,
    onBackground = Ink,
    surface = Ivory,
    onSurface = Ink,
    surfaceVariant = ColorTokens.LightSurfaceVariant,
    onSurfaceVariant = Mist,
    surfaceContainerLowest = ColorTokens.LightSurfaceLowest,
    surfaceContainer = ColorTokens.LightSurface,
    surfaceContainerHigh = ColorTokens.LightSurfaceHigh,
    secondaryContainer = Dune,
    onSecondaryContainer = Cocoa,
    tertiaryContainer = ColorTokens.LightTag,
    onTertiaryContainer = Forest,
)

@Composable
fun FluidDarkModeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}

private object ColorTokens {
    val LightSurfaceLowest = Color(0xFFF2ECE3)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceHigh = Color(0xFFEDE2D2)
    val LightSurfaceVariant = Color(0xFFE8DED0)
    val LightTag = Color(0xFFD9ECD9)
    val DarkSurfaceLowest = Color(0xFF0F141A)
    val DarkSurface = Color(0xFF171D24)
    val DarkSurfaceHigh = Color(0xFF202832)
    val DarkSurfaceVariant = Color(0xFF29323E)
    val DarkTag = Color(0xFF243A31)
}
