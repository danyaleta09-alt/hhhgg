package com.letify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.Tab
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

/**
 * One icon per tab — the SAME asset is used in both active and inactive
 * state; only the tint changes (with a soft cross-fade).
 *
 * For the Profile tab we don't render a glyph at all: the slot shows a
 * 28 dp circular avatar (Telegram photo when bound, otherwise a gradient
 * disc with the first letter of the user's name as fallback) — same
 * pattern as the reference island-nav.
 */
fun tabIcon(tab: Tab): String = when (tab) {
    Tab.Home -> "home-2-bold-duotone"
    Tab.Nutrition -> "apple-bold-duotone"
    Tab.Plan -> "calendar-bold-duotone"
    Tab.Profile -> "user-bold-duotone"
}

/** Human-readable name for every supported tab. */
fun tabTitle(tab: Tab): String = when (tab) {
    Tab.Home -> "Главная"
    Tab.Nutrition -> "Питание"
    Tab.Plan -> "План"
    Tab.Profile -> "Профиль"
}

// === Geometry copied 1:1 from the reference island-nav ===
// Buttons: 52 × 44, rounded 20. Gap 4. Container padding 10/8, radius 28.
// Pill move: 320 ms with Cubic(.32, .72, .00, 1).
// Pill squash on tap: parabolic scaleX 1.0 → 1.12 → 1.0 over 320 ms
// (Y stays 1 — icons themselves do NOT move/scale, per user feedback).
private val ButtonWidth = 52.dp
private val ButtonHeight = 44.dp
private val NavGap = 4.dp
private val NavPadH = 10.dp
private val NavPadV = 8.dp
private val NavCornerRadius = 28.dp
private val PillCornerRadius = 20.dp

// Same easing curve as the reference island-nav for both pill motion
// and squash recovery — gentle ease-out, no overshoot, no bounce.
private val NavEasing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)
private const val NavMoveMs = 320

@Composable
fun Navbar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    // Lambda (not float) so the caller can drive a parallax/fade
    // without triggering recomposition of the navbar subtree. The
    // graphicsLayer below reads it once per frame at draw time.
    alpha: () -> Float = { 1f },
    // True while a tab-switch slide is in flight. While true we drop the
    // per-frame frosted-glass blur (show a flat tint of the same colour) so the
    // moving content under the bar can't make haze shimmer; we crossfade the
    // real blur back in once the screen has settled. See LetifyApp for why.
    tabAnimating: () -> Boolean = { false },
    // 0..1 — how strongly a long-press "peek" dim covers the bar. Read at
    // draw time (lambda) so the dim animates without recomposing the navbar.
    // During a peek the bar stays fully visible (icons included) but is darkened
    // by the SAME theme-coloured scrim as the rest of the screen — one uniform
    // dim, not a vanishing bar and not a self-blur.
    peekDim: () -> Float = { 0f },
) {
    val state = LocalAppState.current
    val tabs = state.navbarOrder
    // Captured here (composable scope) so the draw-phase peek-dim lambda can use
    // it without a @Composable read.
    val peekDimColor = Letify.colors.bg
    val idx = tabs.indexOf(current).coerceAtLeast(0)

    // Pill X — animates with the same tween + curve as the reference.
    // Spring is intentionally NOT used here: the user said the springy
    // motion "moves horribly"; the reference glides linearly with a
    // soft ease-out and no overshoot.
    val indicatorOffset by animateDpAsState(
        targetValue = (ButtonWidth + NavGap) * idx,
        animationSpec = tween(durationMillis = NavMoveMs, easing = NavEasing),
        label = "indicator",
    )

    // Pill squash on tab change: parabolic scaleX 1.0 → 1.12 → 1.0 over
    // 320 ms. Reset to 0 on every change so a fast double-tap still
    // produces a fresh squash.
    val squash = remember { Animatable(0f) }
    LaunchedEffect(idx) {
        squash.snapTo(0f)
        squash.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = NavMoveMs, easing = LinearEasing),
        )
    }
    // 0 → 0.5 → 1 maps to scaleX 1.0 → 1.12 → 1.0 (parabola peaked at .5)
    val pillScaleX = 1f + 0.12f * (1f - kotlin.math.abs(2f * squash.value - 1f))

    // Same hue as r32 (dark = container, light = white), alpha lowered
    // so the frosted-glass blur underneath can read through. No theme
    // colour change, no shadow — per user spec.
    val bg = if (Letify.colors.isDark) {
        Letify.colors.container.copy(alpha = 0.62f)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
    // Frosted-glass blur. Radius 24 dp + a touch of noise (0.18) —
    // the noise grain masks haze 0.7.3's per-frame sub-pixel sampling
    // jitter that shows up as shimmer under scrolling antialiased text.
    // Without noise the blur reads cleaner standing still but visibly
    // dances when the source moves; with this much grain it stays
    // calm and still looks like a real frosted-glass surface.
    val hazeStyle = HazeStyle(
        tint = bg,
        blurRadius = 24.dp,
        noiseFactor = 0.18f,
    )

    // Blur opacity: 1 at rest (real frosted glass), 0 while a tab-switch slide
    // is in flight. We crossfade rather than hard-toggle so the blur returns
    // without a visible "pop". Because the swap is driven off `tabAnimating`
    // (which only flips ~48ms AFTER the slide stops), the blur fades back in
    // while the screen is already at rest — never during the slow ease-out tail
    // where the re-sampling shimmer used to show.
    // Blur is dropped both during a tab-switch slide AND while a screen is being
    // scrolled/flung (`state.contentScrolling`). Re-sampling the frosted backdrop
    // on every frame as the list slides under the bar is the dominant scroll-jank
    // cost; with the blur off (alpha 0) haze does no per-frame sampling, so the
    // list scrolls smoothly. The 180 ms fade brings the frosted glass back the
    // moment the gesture ends — at rest the look is unchanged.
    val blurAlpha by animateFloatAsState(
        targetValue = if (tabAnimating() || state.contentScrolling) 0f else 1f,
        animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        label = "navBlurAlpha",
    )

    Box(
        modifier
            // Parent-driven fade lives HERE, on the same layer that
            // owns the backdrop draw, so the blurred backdrop fades
            // in lock-step with the icons and pill. Putting the layer
            // on a parent Box (as we did in r38) made the backdrop draw
            // at full alpha while the icons faded — net effect:
            // "empty navbar" during an overlay slide-in.
            .graphicsLayer { this.alpha = alpha().coerceIn(0f, 1f) }
            // Bottom safe-area gap — matches the 20 px the reference
            // shell leaves between the island and the bottom of the
            // device. Keeps the nav floating, not glued to the edge.
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(NavCornerRadius))
    ) {
        // ── Frosted-glass backdrop ──
        // A flat translucent tint sits underneath at all times (same hue/alpha
        // as the blur's own tint, so when the blur fades out the panel colour
        // never shifts). The real RenderEffect blur is drawn ON TOP with an
        // animated alpha: full at rest, 0 during a tab slide. While its alpha is
        // 0, haze does no per-frame sampling, so content sliding under the bar
        // can't make it shimmer. Without a haze source we fall through to the
        // plain tint only — same look, no crash.
        if (hazeState != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(bg, RoundedCornerShape(NavCornerRadius)),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { this.alpha = blurAlpha }
                    .hazeChild(
                        state = hazeState,
                        shape = RoundedCornerShape(NavCornerRadius),
                        style = hazeStyle,
                    ),
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .background(bg, RoundedCornerShape(NavCornerRadius)),
            )
        }

        Box(
            Modifier
                .padding(horizontal = NavPadH, vertical = NavPadV)
                .width(ButtonWidth * tabs.size + NavGap * (tabs.size - 1))
                .height(ButtonHeight)
        ) {
            // The indicator pill — squashes on tap, then settles to a
            // calm rounded rect under the active slot.
            Box(
                Modifier
                    .offset(x = indicatorOffset, y = 0.dp)
                    .size(width = ButtonWidth, height = ButtonHeight)
                    .graphicsLayer {
                        scaleX = pillScaleX
                        // Reference squashes ONLY scaleX. The icons
                        // sitting on top don't move at all.
                        scaleY = 1f
                    }
                    .background(
                        Letify.colors.accent.copy(alpha = 0.18f),
                        RoundedCornerShape(PillCornerRadius),
                    )
            )

            Row(Modifier.fillMaxSize()) {
                tabs.forEachIndexed { i, tab ->
                    if (i > 0) Spacer(Modifier.width(NavGap))
                    NavItem(
                        tab = tab,
                        active = current == tab,
                        onClick = { onSelect(tab) },
                    )
                }
            }
        }

        // Peek dim — drawn LAST (on top of backdrop + icons) and clipped to
        // the bar's rounded shape by the outer Box's clip(). The same 0.6 theme
        // background scrim used over the rest of the screen, so the bar reads as
        // dimmed (not gone) while a long-press menu is open.
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(peekDimColor, alpha = (0.6f * peekDim()).coerceIn(0f, 1f))
                },
        )
    }
}

@Composable
private fun NavItem(tab: Tab, active: Boolean, onClick: () -> Unit) {
    // Icon tint cross-fades between accent and muted over 200 ms —
    // same as the reference's AnimatedSwitcher between two const
    // colored icons, but cheaper: a single tint animation, no swap.
    val tint by animateColorAsState(
        targetValue = if (active) Letify.colors.accent else Letify.colors.muted,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "navTint",
    )

    NoFeedbackButton(
        onClick = onClick,
        modifier = Modifier.size(width = ButtonWidth, height = ButtonHeight),
        // Nav icons must stay perfectly still — only the pill underneath
        // moves/squashes. Opt out of the shared press squat.
        feedback = false,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (tab == Tab.Profile) {
                ProfileNavSlot()
            } else {
                // No scale, no lift — icons stay perfectly still
                // (only the pill underneath moves and squashes).
                SolarIcon(name = tabIcon(tab), tint = tint, size = 24.dp)
            }
        }
    }
}

/**
 * Profile slot: 28 dp circular avatar.
 *  - Gradient backdrop (accent → pink) + first letter of `userName`
 *    is painted first so the slot never flashes empty.
 *  - When a Telegram photo is bound, an `AsyncImage` crossfades on
 *    top of the backdrop.
 *
 * No active-state scale — matches the reference, where the avatar
 * stays the same size whether selected or not (the pill behind it
 * is the only thing that changes).
 */
@Composable
private fun ProfileNavSlot() {
    val state = LocalAppState.current
    val context = LocalContext.current
    val photoUrl = state.telegramUser?.photoUrl
    val letter = state.userName.firstOrNull()?.uppercase() ?: "?"

    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Letify.colors.accent, LetifyColors.TilePink),
                ),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        if (photoUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .crossfade(260)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
