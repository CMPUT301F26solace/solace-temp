/**
 * SolaceDesignSystem.kt
 *
 * Purpose: Reusable, Awwwards-caliber design tokens and composable primitives that form the
 * shared visual system for Campsite Owner screens, locked to the app's signature palette:
 * #EBEBF2 cool lavender-grey canvas, #FFFFFF elevated cards, #1A1D2E deep navy-slate text,
 * and #C91E3E / #D61F3C crimson-red primary CTAs.
 *
 * Performance notes:
 * • Press feedback animates strictly inside `Modifier.graphicsLayer { scaleX; scaleY }`.
 * • Colors use pre-allocated `Color(0x…)` tokens to avoid allocation during recomposition.
 */
package com.example.solace_temp.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.solace_temp.ui.components.LocalReduceMotion

// ═════════════════════════════════════════════════════════════════════════════
// 1. SPACING, DIMENSION & SIGNATURE PALETTE TOKENS
// ═════════════════════════════════════════════════════════════════════════════

/** Central repository of spacing, radius, elevation, and locked signature color tokens. */
@Immutable
object SolaceTokens {
    // ── Signature Palette Locks ──────────────────────────────────────────
    val ScreenBackground: Color = OwnerBackground
    val CardBackground: Color = CardSurface
    val PrimaryText: Color = Neutral900
    val PrimaryCta: Color = SolaceRed
    val PrimaryCtaVibrant: Color = SolaceRedVibrant

    // ── Spacing (mobile-first generous whitespace) ───────────────────────
    val SpaceXXS: Dp = 2.dp
    val SpaceXS: Dp = 4.dp
    val SpaceSM: Dp = 8.dp
    val SpaceMD: Dp = 12.dp
    val SpaceLG: Dp = 16.dp
    val SpaceXL: Dp = 20.dp
    val SpaceXXL: Dp = 24.dp
    val Space3XL: Dp = 32.dp
    val Space4XL: Dp = 40.dp

    // ── Corner radius ────────────────────────────────────────────────────
    val RadiusSM: Dp = 8.dp
    val RadiusMD: Dp = 12.dp
    val RadiusLG: Dp = 16.dp
    val RadiusXL: Dp = 20.dp
    val RadiusXXL: Dp = 24.dp
    val RadiusSheet: Dp = 28.dp
    val RadiusPill: Dp = 100.dp

    // ── Elevation (subtle, modern flat-ish) ──────────────────────────────
    val ElevationNone: Dp = 0.dp
    val ElevationLow: Dp = 1.dp
    val ElevationMedium: Dp = 2.dp
    val ElevationHigh: Dp = 4.dp

    // ── Hairline border ──────────────────────────────────────────────────
    val HairlineWidth: Dp = 0.5.dp
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. CARD STYLING HELPERS
// ═════════════════════════════════════════════════════════════════════════════

/** Pre-built card shape with the refined XL radius. */
val SolaceCardShape = RoundedCornerShape(SolaceTokens.RadiusXL)

/** Pre-built rounded shape for inner sections / status areas. */
val SolaceInnerCardShape = RoundedCornerShape(SolaceTokens.RadiusMD)

/** Returns a subtle hairline [BorderStroke] for card outlines. */
@Composable
fun solaceCardBorder(): BorderStroke =
    BorderStroke(SolaceTokens.HairlineWidth, HairlineBorder)

/** Default card colors with pure white (#FFFFFF) surface. */
@Composable
fun solaceCardColors(): CardColors =
    CardDefaults.cardColors(containerColor = CardSurface)

/** Default card elevation — subtle 2 dp. */
@Composable
fun solaceCardElevation(): CardElevation =
    CardDefaults.cardElevation(defaultElevation = SolaceTokens.ElevationMedium)

// ═════════════════════════════════════════════════════════════════════════════
// 3. TACTILE GPU-COMPOSITED PRESS FEEDBACK
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Applies tactile press scale strictly via `Modifier.graphicsLayer` so pointer presses
 * never trigger layout or recomposition overhead.
 */
fun Modifier.solaceTactileScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reduceMotion) pressedScale else 1f,
        animationSpec = if (reduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "solace_tactile_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. STATUS PILL
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Returns the (background, foreground) color pair for a lottery/campsite status string.
 */
fun statusColors(status: String): Pair<Color, Color> = when (status) {
    "Active"   -> StatusActiveBg to StatusActiveGreen
    "Upcoming" -> StatusUpcomingBg to StatusUpcomingBlue
    "Booking"  -> StatusBookingBg to StatusBookingPurple
    "Finished" -> StatusFinishedBg to StatusFinishedGray
    else       -> StatusNABg to StatusNAGray
}

/**
 * A compact, accessible status badge (pill).
 */
@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = statusColors(status)
    Surface(
        shape = RoundedCornerShape(SolaceTokens.RadiusSM),
        color = bg,
        modifier = modifier
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. TABULAR NUMBERS TEXT
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A [Text] composable that renders with **tabular (monospaced) figures** (`tnum`) so that
 * numeric columns (capacity, pricing, GPS coordinates, countdown timers) stay aligned.
 */
@Composable
fun TabularText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. HAIRLINE DIVIDER
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A hairline horizontal divider.
 */
@Composable
fun SolaceDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = SolaceTokens.HairlineWidth,
        color = HairlineBorder
    )
}
