/**
 * Color.kt
 *
 * Purpose: Defines the refined color primitives and semantic tokens used throughout the
 * Campsite Owner visual system, locked to the app's signature palette:
 * cool soft lavender-grey background (#EBEBF2), pure white elevated cards (#FFFFFF),
 * deep navy-slate text (#1A1D2E), and crimson-red primary CTAs (#C91E3E / #D61F3C).
 */
package com.example.solace_temp.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Legacy palette (kept for backward compat, prefer semantic tokens below) ───
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ─── Brand ────────────────────────────────────────────────────────────────────
/** Primary crimson-red brand accent — primary CTAs, links, price callouts. */
val SolaceRed = Color(0xFFC91E3E)
val SolaceRedVibrant = Color(0xFFD61F3C)
val SolaceRedSoft = Color(0xFFFCE8EC)
val SolaceRedDark = Color(0xFFEF5350)

// ─── Neutral palette (cool navy-slate scale harmonized with #EBEBF2) ──────────
val Neutral50 = Color(0xFFF7F7FB)
val Neutral100 = Color(0xFFF1F1F7)
val Neutral200 = Color(0xFFE4E4EE)
val Neutral300 = Color(0xFFD4D5E2)
val Neutral400 = Color(0xFFA8ABC0)
val Neutral500 = Color(0xFF7B7F98)
val Neutral600 = Color(0xFF5D617B)
val Neutral700 = Color(0xFF3E425B)
val Neutral800 = Color(0xFF2B2E45)
/** Deep navy-slate primary text. */
val Neutral900 = Color(0xFF1A1D2E)

// ─── Surface tokens ───────────────────────────────────────────────────────────
/** Cool soft lavender-grey background canvas for Owner screens (#EBEBF2). */
val OwnerBackground = Color(0xFFEBEBF2)

/** Pure white elevated card surface (#FFFFFF). */
val CardSurface = Color(0xFFFFFFFF)
val CardSurfaceDark = Color(0xFF1E1E1E)

/** Hairline divider / border color tuned for #EBEBF2 / #FFFFFF contrast. */
val HairlineBorder = Color(0xFFDCDDE9)
val HairlineBorderDark = Color(0xFF2E2E2E)

// ─── Status pill palette ──────────────────────────────────────────────────────
val StatusActiveGreen = Color(0xFF16A34A)
val StatusActiveBg = Color(0xFFDCFCE7)
val StatusUpcomingBlue = Color(0xFF2563EB)
val StatusUpcomingBg = Color(0xFFDBEAFE)
val StatusBookingPurple = Color(0xFF7C3AED)
val StatusBookingBg = Color(0xFFEDE9FE)
val StatusFinishedGray = Color(0xFF6B7280)
val StatusFinishedBg = Color(0xFFF3F4F6)
val StatusNAGray = Color(0xFF9CA3AF)
val StatusNABg = Color(0xFFF9FAFB)

// ─── Semantic accents ─────────────────────────────────────────────────────────
val SuccessGreen = Color(0xFF16A34A)
val ErrorRed = Color(0xFFC91E3E)
val WarningAmber = Color(0xFFD97706)
val WarningAmberBg = Color(0xFFFEF3C7)
val InfoBlue = Color(0xFF2563EB)

// ─── Overlay / scrim ──────────────────────────────────────────────────────────
val ScrimLight = Color(0x33000000)
val ScrimDark = Color(0x66000000)
