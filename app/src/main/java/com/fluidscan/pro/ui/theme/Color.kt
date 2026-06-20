package com.fluidscan.pro.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * FluidScan Pro — Brand & Material 3 tonal palette.
 *
 * These static tokens are the *fallback* palette used on Android < 12 (no dynamic
 * color) and as the brand seed. On Android 12+ the dynamic (Monet) palette derived
 * from the user's wallpaper takes precedence — see [com.fluidscan.pro.ui.theme.FluidScanTheme].
 */

// ---- Brand seed (electric "scan-line" blue + deep ink) ----
val BrandPrimary       = Color(0xFF2D6CFF) // electric scan blue
val BrandSecondary     = Color(0xFF00C2A8) // magic-color teal
val BrandTertiary      = Color(0xFF7C4DFF) // signature / AI violet
val BrandInk           = Color(0xFF0B1020) // deep document ink

// ---- Light scheme ----
val md_light_primary           = BrandPrimary
val md_light_onPrimary         = Color(0xFFFFFFFF)
val md_light_primaryContainer  = Color(0xFFD9E2FF)
val md_light_onPrimaryContainer= Color(0xFF001847)
val md_light_secondary         = BrandSecondary
val md_light_onSecondary       = Color(0xFF003731)
val md_light_secondaryContainer= Color(0xFF8FF6E4)
val md_light_onSecondaryContainer = Color(0xFF00201C)
val md_light_tertiary          = BrandTertiary
val md_light_onTertiary        = Color(0xFFFFFFFF)
val md_light_tertiaryContainer = Color(0xFFE9DDFF)
val md_light_onTertiaryContainer = Color(0xFF21005D)
val md_light_error             = Color(0xFFBA1A1A)
val md_light_onError           = Color(0xFFFFFFFF)
val md_light_errorContainer    = Color(0xFFFFDAD6)
val md_light_onErrorContainer  = Color(0xFF410002)
val md_light_background        = Color(0xFFFAF9FF)
val md_light_onBackground      = Color(0xFF1A1B20)
val md_light_surface           = Color(0xFFFAF9FF)
val md_light_onSurface         = Color(0xFF1A1B20)
val md_light_surfaceVariant    = Color(0xFFE1E2EC)
val md_light_onSurfaceVariant  = Color(0xFF44464F)
val md_light_outline           = Color(0xFF757780)

// ---- Dark scheme ----
val md_dark_primary            = Color(0xFFADC6FF)
val md_dark_onPrimary          = Color(0xFF002C72)
val md_dark_primaryContainer   = Color(0xFF0042A0)
val md_dark_onPrimaryContainer = Color(0xFFD9E2FF)
val md_dark_secondary          = Color(0xFF73D9C8)
val md_dark_onSecondary        = Color(0xFF003731)
val md_dark_secondaryContainer = Color(0xFF005048)
val md_dark_onSecondaryContainer = Color(0xFF8FF6E4)
val md_dark_tertiary           = Color(0xFFCFBCFF)
val md_dark_onTertiary         = Color(0xFF381E72)
val md_dark_tertiaryContainer  = Color(0xFF4F378A)
val md_dark_onTertiaryContainer= Color(0xFFE9DDFF)
val md_dark_error              = Color(0xFFFFB4AB)
val md_dark_onError            = Color(0xFF690005)
val md_dark_errorContainer     = Color(0xFF93000A)
val md_dark_onErrorContainer   = Color(0xFFFFDAD6)
val md_dark_background         = Color(0xFF111318)
val md_dark_onBackground       = Color(0xFFE3E1E9)
val md_dark_surface            = Color(0xFF111318)
val md_dark_onSurface          = Color(0xFFE3E1E9)
val md_dark_surfaceVariant     = Color(0xFF44464F)
val md_dark_onSurfaceVariant   = Color(0xFFC5C6D0)
val md_dark_outline            = Color(0xFF8F9099)

// ---- Functional accents (scan overlays / OCR highlight) ----
val ScanReticleActive  = Color(0xFF35F2C0) // breathing reticle when locked
val OcrLineHighlight    = Color(0x553D6CFF) // translucent line-by-line sweep
val EdgeDetectStroke    = Color(0xFF35F2C0) // spring-loaded crop border
