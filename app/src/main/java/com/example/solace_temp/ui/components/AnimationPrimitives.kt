/**
 * AnimationPrimitives.kt
 *
 * Purpose: Reusable, 60 fps-safe, Olivier Larose-inspired animation composables optimized for
 * Android WebView / Emulator rendering. These primitives form the shared motion layer that
 * individual Owner screens can adopt without breaking existing routing or business logic.
 *
 * Performance contract:
 * ─ ALL transitions animate ONLY GPU-composited properties: graphicsLayer { translationY,
 *   scaleX, scaleY, alpha }. No width/height/margin/padding/top/left mutations.
 * ─ Zero box-shadow or blur-filter animations.
 * ─ Every animated value is derived from `Animatable` / `animate*AsState` and runs on the
 *   composition clock — no custom `ValueAnimator` or `Handler` loops.
 * ─ `ScratchRevealCanvas` uses `AndroidView` with an idle-free Canvas; the draw loop fires
 *   only on pointer events.
 * ─ `prefers-reduced-motion` honoured via `LocalReduceMotion`.
 *
 * Contents:
 * 1. LocalReduceMotion   – CompositionLocal for reduced-motion preference
 * 2. StackedParallaxCard – Scroll-linked sticky-deck card feed
 * 3. PerspectivePageTransition – AnimatedVisibility with scale/dim outgoing screen
 * 4. ScratchRevealCanvas – HTML5-style 2D canvas scratch overlay
 */
package com.example.solace_temp.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

// ═════════════════════════════════════════════════════════════════════════════
// 0. REDUCED MOTION PREFERENCE
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Composition-local flag for `prefers-reduced-motion`. Defaults to false.
 * Hosts should read the system accessibility setting and provide `true` when needed:
 *
 * ```kotlin
 * val reduceMotion = Settings.Global.getFloat(
 *     contentResolver,
 *     Settings.Global.ANIMATOR_DURATION_SCALE, 1f
 * ) == 0f
 *
 * CompositionLocalProvider(LocalReduceMotion provides reduceMotion) { … }
 * ```
 */
val LocalReduceMotion = compositionLocalOf { false }

// ═════════════════════════════════════════════════════════════════════════════
// 1. STACKED PARALLAX CARD
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A card wrapper that creates a **scroll-linked stacking-deck** effect.
 *
 * As the user scrolls, each card scales down and translates vertically so that earlier
 * cards appear to tuck behind later ones — like a physical card deck. The effect is
 * driven by `Modifier.onPlaced` (avoiding global layout-tree listeners) and evaluates
 * transforms strictly in `Modifier.graphicsLayer { scaleX, scaleY, translationY, alpha }`
 * so scrolling never triggers recomposition.
 *
 * When [reduceMotion] is `true`, the parallax math is bypassed and the card renders at
 * its natural position with no transform.
 *
 * @param index The zero-based index of this card inside the vertical list.
 * @param cardCount Total number of cards in the feed (used to normalize scale).
 * @param modifier Modifier applied to the outer wrapper [Box].
 * @param stickyTopOffset The sticky "rest" position from the top of the viewport. Cards that
 *   scroll above this offset begin to shrink and dim.
 * @param reduceMotion Whether reduced motion is enabled (can be hoisted by lazy list parents).
 * @param content The card content slot.
 */
@Composable
fun StackedParallaxCard(
    index: Int,
    cardCount: Int,
    modifier: Modifier = Modifier,
    stickyTopOffset: Dp = 0.dp,
    reduceMotion: Boolean = LocalReduceMotion.current,
    content: @Composable BoxScope.() -> Unit
) {
    // Track card's Y position relative to its parent viewport (LazyColumn) in Layout phase,
    // and read only inside graphicsLayer (Draw phase) so scrolling skips Composition.
    var positionY by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onPlaced { coords ->
                positionY = coords.positionInParent().y
                val parentH = coords.parentLayoutCoordinates?.size?.height?.toFloat()
                if (parentH != null && parentH > 1f) {
                    viewportHeight = parentH
                }
            }
            .graphicsLayer {
                if (reduceMotion) return@graphicsLayer

                val stickyTopPx = stickyTopOffset.toPx()
                // How far above the sticky offset the card has scrolled (0 = at rest).
                val scrollPast = (stickyTopPx - positionY).coerceAtLeast(0f)
                val maxTravel = (viewportHeight * 0.5f).coerceAtLeast(1f)

                // progress 0 → 1 as card scrolls above the sticky line
                val progress = (scrollPast / maxTravel).coerceIn(0f, 1f)

                // Scale down to 0.92 at max, offset-proportional so deeper cards shrink more.
                val depthFactor = 1f - (index.toFloat() / cardCount.coerceAtLeast(1).toFloat()) * 0.03f
                val targetScale = (1f - progress * 0.08f) * depthFactor
                scaleX = targetScale.coerceIn(0.88f, 1f)
                scaleY = scaleX

                // Translate upward slightly to create the tuck-under illusion.
                translationY = -scrollPast * 0.15f

                // Dim older cards gently.
                alpha = (1f - progress * 0.25f).coerceIn(0.6f, 1f)
            },
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. PERSPECTIVE PAGE TRANSITION
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Wraps two content slots — a **background** (the outgoing screen) and a **foreground**
 * (the incoming detail/sheet) — and applies a cinematic transition:
 *
 * • Background smoothly scales to 0.96 and dims to ~70 % opacity when foreground is visible.
 * • Foreground slides up from the bottom and fades in simultaneously.
 *
 * All animation targets are GPU-composited: `graphicsLayer { scaleX, scaleY, alpha }` for
 * the background (without recomposing [backgroundContent]); `AnimatedVisibility` with
 * `slideInVertically` + `fadeIn` for the foreground.
 *
 * When [LocalReduceMotion] is `true`, the foreground simply appears/disappears with a quick
 * fade (no slide or background scale).
 *
 * @param showForeground Whether the foreground content should be visible.
 * @param modifier Modifier for the root container.
 * @param backgroundContent The always-present outgoing screen content.
 * @param foregroundContent The incoming detail/sheet content shown when [showForeground] is true.
 */
@Composable
fun PerspectivePageTransition(
    showForeground: Boolean,
    modifier: Modifier = Modifier,
    backgroundContent: @Composable () -> Unit,
    foregroundContent: @Composable () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current

    val backgroundScale by animateFloatAsState(
        targetValue = if (showForeground && !reduceMotion) 0.96f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(220, easing = FastOutSlowInEasing),
        label = "perspective_bg_scale"
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (showForeground && !reduceMotion) 0.70f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(220, easing = FastOutSlowInEasing),
        label = "perspective_bg_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ── Background (outgoing) ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (reduceMotion) return@graphicsLayer
                    scaleX = backgroundScale
                    scaleY = backgroundScale
                    alpha = backgroundAlpha
                }
        ) {
            backgroundContent()
        }

        // ── Foreground (incoming) ────────────────────────────────────────
        val enterTransition = if (reduceMotion) {
            fadeIn(animationSpec = tween(150))
        } else {
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
        }

        val exitTransition = if (reduceMotion) {
            fadeOut(animationSpec = tween(150))
        } else {
            slideOutVertically(
                targetOffsetY = { it / 4 },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(180))
        }

        AnimatedVisibility(
            visible = showForeground,
            enter = enterTransition,
            exit = exitTransition,
            modifier = Modifier.fillMaxSize()
        ) {
            foregroundContent()
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. SCRATCH REVEAL CANVAS
// ═════════════════════════════════════════════════════════════════════════════

/**
 * A lightweight 2D `<canvas>`-style scratch overlay that uses
 * `PorterDuff.Mode.CLEAR` (the native equivalent of CSS
 * `globalCompositeOperation = 'destination-out'`) with pointer/touch support.
 *
 * Key design:
 * ─ **Zero idle animation loop.** The canvas only invalidates when the user touches it.
 * ─ **GPU-friendly.** Uses a single off-screen [Bitmap] blitted in `onDraw`; erasing is a
 *   `Path` drawn with `PorterDuff.Mode.CLEAR`.
 * ─ Ready for QR-code or lottery-winner reveals: place the content to reveal underneath and
 *   layer this composable on top.
 *
 * @param overlayColor The opaque color that covers the hidden content (scratched away on touch).
 * @param strokeWidth The radius of the scratch "brush" in pixels.
 * @param revealThreshold Fraction (0 → 1) of the surface that must be scratched before
 *   [onFullyRevealed] fires.
 * @param onFullyRevealed Optional callback when the scratch percentage exceeds [revealThreshold].
 * @param modifier Modifier for sizing. **Must have a fixed or fillMaxSize size.**
 */
@Composable
fun ScratchRevealCanvas(
    modifier: Modifier = Modifier,
    overlayColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    strokeWidth: Float = 60f,
    revealThreshold: Float = 0.55f,
    onFullyRevealed: (() -> Unit)? = null
) {
    var revealed by remember { mutableStateOf(false) }
    val currentOnFullyRevealed by rememberUpdatedState(onFullyRevealed)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ScratchCanvasView(
                context = ctx,
                overlayArgb = androidColorFromCompose(overlayColor),
                brushRadius = strokeWidth,
                revealThreshold = revealThreshold,
                onRevealed = {
                    if (!revealed) {
                        revealed = true
                        currentOnFullyRevealed?.invoke()
                    }
                }
            )
        }
    )
}

/** Converts a Compose [Color] to an Android `int` ARGB value. */
private fun androidColorFromCompose(color: androidx.compose.ui.graphics.Color): Int {
    return android.graphics.Color.argb(
        (color.alpha * 255).roundToInt(),
        (color.red * 255).roundToInt(),
        (color.green * 255).roundToInt(),
        (color.blue * 255).roundToInt()
    )
}

/**
 * Custom Android [View] that implements the scratch-to-reveal mechanic.
 * Uses a single offscreen [Bitmap] painted with the overlay color, and clears
 * pixels along the user's touch path with [PorterDuff.Mode.CLEAR].
 */
@SuppressLint("ClickableViewAccessibility", "ViewConstructor")
private class ScratchCanvasView(
    context: Context,
    private val overlayArgb: Int,
    private val brushRadius: Float,
    private val revealThreshold: Float,
    private val onRevealed: () -> Unit
) : View(context) {

    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null
    private val scratchPath = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var moveSampleCounter = 0
    private var hasRevealed = false

    private val erasePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = brushRadius
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val eraseDotPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val existing = overlayBitmap
            if (existing != null && existing.width == w && existing.height == h && !existing.isRecycled) {
                return
            }
            existing?.recycle()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(overlayArgb)
            overlayBitmap = bmp
            overlayCanvas = Canvas(bmp)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastX = event.x
                lastY = event.y
                moveSampleCounter = 0
                overlayCanvas?.drawCircle(lastX, lastY, brushRadius * 0.5f, eraseDotPaint)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val x = event.x
                val y = event.y
                // Rewind retained path buffer to draw only the latest segment in O(1)
                scratchPath.rewind()
                scratchPath.moveTo(lastX, lastY)
                scratchPath.lineTo(x, y)
                overlayCanvas?.drawPath(scratchPath, erasePaint)
                lastX = x
                lastY = y
                invalidate() // Only invalidate on actual touch — no idle loop
                moveSampleCounter++
                if (moveSampleCounter % 4 == 0) {
                    checkReveal()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                scratchPath.rewind()
                checkReveal()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        overlayBitmap?.let { canvas.drawBitmap(it, 0f, 0f, bitmapPaint) }
    }

    /**
     * Samples the bitmap to estimate the fraction of transparent (scratched) pixels.
     * Uses a 10 × 10 grid sample for O(1) performance instead of scanning every pixel.
     */
    private fun checkReveal() {
        if (hasRevealed) return
        val bmp = overlayBitmap ?: return
        val w = bmp.width
        val h = bmp.height
        if (w == 0 || h == 0) return

        val sampleCols = 10
        val sampleRows = 10
        var transparent = 0
        val total = sampleCols * sampleRows

        for (col in 0 until sampleCols) {
            for (row in 0 until sampleRows) {
                val px = (w * (col + 0.5f) / sampleCols).toInt().coerceIn(0, w - 1)
                val py = (h * (row + 0.5f) / sampleRows).toInt().coerceIn(0, h - 1)
                if (android.graphics.Color.alpha(bmp.getPixel(px, py)) == 0) {
                    transparent++
                }
            }
        }

        if (transparent.toFloat() / total >= revealThreshold) {
            hasRevealed = true
            onRevealed()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        overlayBitmap?.recycle()
        overlayBitmap = null
        overlayCanvas = null
    }
}
