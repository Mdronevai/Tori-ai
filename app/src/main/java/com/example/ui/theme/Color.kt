package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dola & Cyber-OLED Glowing Palette (Black, Electric Pink, Magenta, Neon Violet)
val ToriElectricPink = Color(0xFFFF2A85)
val ToriMagenta = Color(0xFFE11D48)
val ToriNeonViolet = Color(0xFF9333EA)
val ToriRubyGlow = Color(0xFFFF0055)
val ToriAmberGlow = Color(0xFFFF7A00)
val ToriFuchsia = Color(0xFFD946EF)

// Backwards-compatible aliases aligned to the Electric Pink / Magenta / Neon Violet theme
val ToriCyan = ToriElectricPink
val ToriCyanDark = ToriMagenta
val ToriBlue = ToriFuchsia
val ToriIndigo = ToriMagenta
val ToriViolet = ToriNeonViolet

// OLED Pure Black Theme Colors
val OledBlack = Color(0xFF070709)
val OledPureBlack = Color(0xFF000000)
val ToriDarkBackground = OledBlack
val ToriFrostedSurface = Color(0xFF12141C)
val ToriFrostedCard = Color(0xFF161922)
val ToriFrostedBorder = Color(0x33FFFFFF)

val ToriDarkSurface = ToriFrostedSurface
val ToriDarkSurfaceVariant = ToriFrostedCard
val ToriDarkOnSurface = Color(0xFFFFFFFF)
val ToriDarkOnSurfaceVariant = Color(0xFFE2E8F0)

val ToriLightBackground = OledBlack
val ToriLightSurface = ToriFrostedSurface
val ToriLightSurfaceVariant = ToriFrostedCard
val ToriLightOnSurface = Color(0xFFFFFFFF)
val ToriLightOnSurfaceVariant = Color(0xFFCBD5E1)

// Accent & Status Colors
val ToriSuccessGreen = Color(0xFF10B981)
val ToriWarningAmber = Color(0xFFF59E0B)
val ToriErrorRed = Color(0xFFEF4444)
val ToriAccentPurple = ToriNeonViolet
val ToriShieldBorder = ToriElectricPink
val ToriSurfaceDark = ToriDarkSurface
val ToriMutedText = Color(0xFF94A3B8)
val ToriCardDark = ToriDarkSurfaceVariant
val ToriBorderDark = ToriFrostedBorder
