package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Pure Clean OLED Black & Glass Theme
val VaultBackground = Color(0xFF000000)
val VaultSurface = Color(0xFF0D0D0F)
val VaultSurfaceVariant = Color(0xFF16161A)
val VaultCardBorder = Color(0x2EFFFFFF) // 18% translucent white glass border

// Glassmorphism & Translucency Tokens
val VaultGlassSurface = Color(0x14FFFFFF) // 8% white glass
val VaultGlassSurfaceElevated = Color(0x1FFFFFFF) // 12% white glass
val VaultGlassBorder = Color(0x26FFFFFF) // 15% white outline
val VaultGlassBorderSubtle = Color(0x14FFFFFF) // 8% white outline
val VaultGlassHighlight = Color(0x33FFFFFF) // 20% white highlight
val VaultGlassNavBg = Color(0xE60A0A0C) // 90% blur glass for navigation

// Linear gradient for frosted glass cards
val GlassGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x1AFFFFFF),
        Color(0x08FFFFFF)
    )
)

val GlassHeaderGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x26FFFFFF),
        Color(0x0DFFFFFF)
    )
)

// Primary Crisp White Accents
val WhitePrimary = Color(0xFFFFFFFF)
val WhitePrimaryDark = Color(0xFF18181B)
val WhitePrimaryContainer = Color(0xFF27272A)
val WhitePrimaryLight = Color(0xFFF4F4F5)

// Legacy aliases mapped to White / Monochromatic Minimalist styling
val LilacPrimary = WhitePrimary
val LilacPrimaryDark = WhitePrimaryDark
val LilacPrimaryContainer = WhitePrimaryContainer
val LilacPrimaryLight = WhitePrimaryLight

val CyanPrimary = WhitePrimary
val CyanPrimaryDark = WhitePrimaryDark
val CyanPrimaryLight = WhitePrimaryLight
val PurpleAccent = WhitePrimary

// Action Buttons
val ActionButtonBg = Color(0xFFFFFFFF)
val ActionButtonText = Color(0xFF000000)
val DarkButtonBg = Color(0x1FFFFFFF)
val DarkButtonText = Color(0xFFFFFFFF)

// Status & Security Accents (Minimalist & Clean)
val EmeraldAccent = Color(0xFF34D399) // Clean subtle mint/emerald
val AmberWarning = Color(0xFFFBBF24)
val RedDanger = Color(0xFFF87171)

// Clean High-Legibility Typography
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA1A1AA)
val TextMuted = Color(0xFF52525B)

// Lock Screen Keypad Glass Styling
val KeypadButtonBg = Color(0x12FFFFFF)
val KeypadButtonActive = Color(0x2EFFFFFF)
