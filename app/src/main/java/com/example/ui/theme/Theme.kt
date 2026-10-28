package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ToriElectricPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0025),
    onPrimaryContainer = Color(0xFFFFD8E6),
    secondary = ToriMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C0519),
    onSecondaryContainer = Color(0xFFFFE4E6),
    tertiary = ToriNeonViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3B0764),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = OledBlack,
    onBackground = Color.White,
    surface = OledPureBlack,
    onSurface = Color.White,
    surfaceVariant = ToriFrostedCard,
    onSurfaceVariant = Color(0xFFE2E8F0),
    error = ToriErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Unify all screens to the OLED Black and Electric Pink / Magenta aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
