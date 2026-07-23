package com.letify.app.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.letify.app.ui.AppIconVariant
import com.letify.app.ui.applyAppIcon
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.letify.app.ui.components.AccentSwitch
import com.letify.app.ui.components.ColorPickerGrid
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.OverlayHost
import com.letify.app.ui.components.RoundedSlideOverlay
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.SettingsRow
import com.letify.app.ui.components.SettingsRowDivider
import com.letify.app.ui.components.overlayHostShiftFraction
import com.letify.app.ui.components.rememberParallaxProgress
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.components.tabIcon
import com.letify.app.ui.components.tabTitle
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.Tab
import com.letify.app.ui.state.TransitionStyle
import com.letify.app.ui.theme.ThemeMode
import com.letify.app.ui.theme.ThemePalette
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

private enum class AppearanceRoute { Root, Navbar, Animation }

@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    var route by remember { mutableStateOf(AppearanceRoute.Root) }
    // No external BackHandler needed: RoundedSlideOverlay registers its
    // own PredictiveBackHandler, which intercepts the system back gesture
    // and plays the slide-out (then calls onDismissed → route = Root).
    // Adding an extra BackHandler here would bypass the animation and
    // pop the overlay instantly.
    //
    // Same transition mechanic as the OUTER settings overlays —
    // RoundedSlideOverlay + OverlayHost give us the iOS-style stack push:
    //   - the Appearance Root parallax-translates left under the cover
    //   - the Navbar settings slide in from the right with the device’s
    //     rounded-corner clip while it crosses the edge
    //   - swipe-back-from-the-left dismisses, same as everywhere else
    val parallax = rememberParallaxProgress()
    Box(Modifier.fillMaxSize()) {
        OverlayHost(parallaxProgress = parallax) {
            AppearanceRoot(
                onBack = onBack,
                onNavbar = { route = AppearanceRoute.Navbar },
                onAnimation = { route = AppearanceRoute.Animation },
            )
        }
        if (route != AppearanceRoute.Root) {
            key(route) {
                RoundedSlideOverlay(
                    parallaxProgress = parallax,
                    onDismissed = { route = AppearanceRoute.Root },
                ) { animatedBack ->
                    when (route) {
                        AppearanceRoute.Navbar -> NavbarSettingsScreen(onBack = animatedBack)
                        AppearanceRoute.Animation -> AnimationSettingsScreen(onBack = animatedBack)
                        AppearanceRoute.Root -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceRoot(onBack: () -> Unit, onNavbar: () -> Unit, onAnimation: () -> Unit) {
    val state = LocalAppState.current
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 60.dp),
        ) {
            SettingsHeader(title = "Оформление", onBack = onBack)

            // Theme toggle
            Text(
                "ТЕМА",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 14.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "moon-bold",
                    iconTile = LetifyColors.TileViolet,
                    title = "Тёмная тема",
                    value = if (state.themeMode == ThemeMode.Dark) "Включена" else "Выключена",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.themeMode == ThemeMode.Dark,
                            onCheckedChange = { dark ->
                                state.themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light
                            },
                        )
                    },
                )
            }

            // Accent color picker
            Text(
                "АКЦЕНТНЫЙ ЦВЕТ",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            ) {
                ColorPickerGrid(
                    colors = ThemePalette,
                    selected = state.accent,
                    onSelect = { state.accent = it },
                )
            }

            // Navbar settings entry
            Text(
                "НАВИГАЦИЯ",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "settings-bold",
                    iconTile = LetifyColors.TileBlue,
                    title = "Навбар",
                    value = tabTitle(state.defaultTab),
                    onClick = onNavbar,
                )
                SettingsRowDivider()
                SettingsRow(
                    icon = "magic-stick-3-bold-duotone",
                    iconTile = LetifyColors.TilePink,
                    title = "Анимация",
                    value = transitionStyleTitle(state.transitionStyle),
                    onClick = onAnimation,
                )
            }

            // ---- App icon picker --------------------------------------------
            // One row of rounded launcher-icon thumbnails. Tapping (or dragging
            // a finger across the row) picks one; the chosen variant's manifest
            // alias is enabled and the home-screen icon swaps to it.
            Text(
                "ИКОНКА",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
            )
            // No card/container — the icons sit directly on the screen background.
            Box(modifier = Modifier.screenHPad().padding(top = 2.dp, bottom = 4.dp)) {
                AppIconPicker(
                    selectedKey = state.appIcon,
                    onSelect = { state.appIcon = it.key },
                )
            }
        }
    }
}

/**
 * Horizontal row of rounded launcher-icon thumbnails. Supports tap-to-pick and
 * drag-across-to-pick (the reference "пуши" accent picker behaviour). The
 * selected thumbnail gets an accent ring and a slight inner inset so the size
 * of the row never changes. Selecting also swaps the real launcher icon.
 */
@Composable
private fun AppIconPicker(selectedKey: String, onSelect: (AppIconVariant) -> Unit) {
    val context = LocalContext.current
    val variants = AppIconVariant.entries

    // Each thumbnail is its OWN tappable target (direct click, not window-
    // coordinate hit-testing — the old version compared a local touch offset
    // against window-space bounds, so they never matched and nothing happened).
    // Equal-width square cells with even gaps → identical size, even spacing.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        variants.forEach { v ->
            AppIconThumb(
                variant = v,
                selected = v.key == selectedKey,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (v.key != selectedKey) {
                        onSelect(v)
                        applyAppIcon(context, v)
                    }
                },
            )
        }
    }
}

@Composable
private fun AppIconThumb(
    variant: AppIconVariant,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Selected = full strength; the others are simply dimmed. No accent ring,
    // no inset/size change — every thumbnail stays exactly the same size.
    val dim by animateFloatAsState(
        targetValue = if (selected) 1f else 0.4f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconDim",
    )
    val corner = 16.dp
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(corner))
            .noFeedbackClick(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = variant.thumbRes),
            contentDescription = variant.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dim },
        )
    }
}

private fun transitionStyleTitle(style: TransitionStyle): String = when (style) {
    TransitionStyle.Push -> "Сдвиг"
    TransitionStyle.Cover -> "Наплыв"
}

@Composable
private fun NavbarSettingsScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 60.dp),
        ) {
            SettingsHeader(
                title = "Навбар",
                onBack = onBack,
                trailing = { NavbarResetButton(onReset = { state.resetNavbar() }) },
            )

            // ---- Live navbar preview with long-press drag reorder ------------
            // The user asked for the actual navbar to be shown on the editor
            // ("сделай там чтобы отображался сам навбар и пункты можно
            // перетаскивать внутри него удержанием"). Drag-to-reorder gives the
            // editor the same shape as the real navbar at the bottom of the app.
            Text(
                "ПОРЯДОК",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 10.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                DraggableNavbarPreview()
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 4.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Нажми и удержи иконку, чтобы перетащить",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }

            // ---- Default landing tab -----------------------------------------
            Text(
                "СТАРТОВЫЙ ЭКРАН",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                state.navbarOrder.forEachIndexed { i, tab ->
                    val active = tab == state.defaultTab
                    SettingsRow(
                        icon = tabIcon(tab),
                        iconTile = tileForTab(tab),
                        title = tabTitle(tab),
                        showChevron = false,
                        trailing = {
                            if (active) {
                                Box(
                                    Modifier
                                        .size(22.dp)
                                        .background(Letify.colors.accent, RoundedCornerShape(999.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    SolarIcon(
                                        name = "check-bold",
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        size = 14.dp,
                                    )
                                }
                            } else {
                                Box(Modifier.size(22.dp))
                            }
                        },
                        onClick = { state.defaultTab = tab },
                    )
                    if (i < state.navbarOrder.size - 1) SettingsRowDivider()
                }
            }
        }
    }
}

// Top-right reset affordance for the navbar editor header. Restores the
// factory order + default landing tab in one tap (asked for in n1365 — a
// "вернуть по умолчанию" button in the header).
@Composable
private fun NavbarResetButton(onReset: () -> Unit) {
    NoFeedbackButton(
        onClick = onReset,
        modifier = Modifier.size(44.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SolarIcon(
                name = "restart-bold-duotone",
                tint = Letify.colors.muted,
                size = 24.dp,
            )
        }
    }
}

// A 1:1 replica of the actual bottom navbar that lets the user long-press
// any icon and drag it to a new slot. Layout is identical to the real
// navbar, just inverted: it sits inside a settings page rather than at
// the bottom of the screen.
@Composable
private fun DraggableNavbarPreview() {
    val state = LocalAppState.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val itemSize = 56.dp
    val gap = 6.dp
    val padding = 8.dp
    val cornerRadius = 28.dp

    val itemSizePx = with(density) { itemSize.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val slotPitchPx = itemSizePx + gapPx

    // Which slot is currently being dragged (-1 = none) and how far it
    // has been pulled from its slot center.
    var draggedIndex by remember { mutableIntStateOf(-1) }
    val dragOffsetPx = remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .background(
                color = Letify.colors.container.copy(alpha = 0.94f),
                shape = RoundedCornerShape(cornerRadius),
            )
            .padding(padding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            state.navbarOrder.forEachIndexed { i, tab ->
                if (i > 0) Box(Modifier.width(gap))
                val isDragged = i == draggedIndex
                // Slots that aren't being dragged shift gently to make space
                // when the user drags an icon past their slot center.
                val sideShift = if (draggedIndex < 0) 0f else {
                    val from = draggedIndex
                    val targetFloat = (from + dragOffsetPx.floatValue / slotPitchPx)
                        .coerceIn(0f, state.navbarOrder.lastIndex.toFloat())
                    val to = kotlin.math.round(targetFloat).toInt()
                    when {
                        i == from -> 0f
                        from < to && i in (from + 1)..to -> -slotPitchPx
                        from > to && i in to..(from - 1) -> slotPitchPx
                        else -> 0f
                    }
                }
                val animShift by animateFloatAsState(
                    targetValue = sideShift,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "shift",
                )
                val scale by animateFloatAsState(
                    targetValue = if (isDragged) 1.12f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "scale",
                )
                Box(
                    Modifier
                        .size(itemSize)
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationX = if (isDragged) dragOffsetPx.floatValue else animShift
                            scaleX = scale
                            scaleY = scale
                        }
                        .pointerInput(state.navbarOrder.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = i
                                    dragOffsetPx.floatValue = 0f
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.LongPress,
                                    )
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val maxLeft = -i * slotPitchPx
                                    val maxRight = (state.navbarOrder.lastIndex - i) * slotPitchPx
                                    dragOffsetPx.floatValue =
                                        (dragOffsetPx.floatValue + drag.x)
                                            .coerceIn(maxLeft, maxRight)
                                },
                                onDragEnd = {
                                    val from = draggedIndex
                                    if (from >= 0) {
                                        val targetFloat = (from + dragOffsetPx.floatValue / slotPitchPx)
                                            .coerceIn(0f, state.navbarOrder.lastIndex.toFloat())
                                        val to = kotlin.math.round(targetFloat).toInt()
                                        if (to != from) {
                                            val moved = state.navbarOrder.removeAt(from)
                                            state.navbarOrder.add(to, moved)
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.TextHandleMove,
                                            )
                                        }
                                    }
                                    draggedIndex = -1
                                    dragOffsetPx.floatValue = 0f
                                },
                                onDragCancel = {
                                    draggedIndex = -1
                                    dragOffsetPx.floatValue = 0f
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                color = if (isDragged) Letify.colors.accentSoft
                                else androidx.compose.ui.graphics.Color.Transparent,
                                shape = RoundedCornerShape(999.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        SolarIcon(
                            name = tabIcon(tab),
                            tint = if (isDragged) Letify.colors.accent else Letify.colors.text,
                            size = 26.dp,
                        )
                    }
                }
            }
        }
    }
}

// ── Анимация ────────────────────────────────────────────────────────────────

// Same gentle ease-out the real transitions use, so the looping previews move
// with exactly the feel the user gets in the app.
private val PreviewEasing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)

@Composable
private fun AnimationSettingsScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 60.dp),
        ) {
            SettingsHeader(title = "Анимация", onBack = onBack)

            Text(
                "ПЕРЕХОД МЕЖДУ ЭКРАНАМИ",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 12.dp),
            )

            Row(
                modifier = Modifier.screenHPad().fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TransitionOptionCard(
                    style = TransitionStyle.Push,
                    title = "Сдвиг",
                    subtitle = "Единое полотно",
                    selected = state.transitionStyle == TransitionStyle.Push,
                    onSelect = { state.transitionStyle = TransitionStyle.Push },
                    modifier = Modifier.weight(1f),
                )
                TransitionOptionCard(
                    style = TransitionStyle.Cover,
                    title = "Наплыв",
                    subtitle = "Наезжает поверх",
                    selected = state.transitionStyle == TransitionStyle.Cover,
                    onSelect = { state.transitionStyle = TransitionStyle.Cover },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                "ЖЕСТЫ",
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 8.dp),
            )
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "arrow-left-bold",
                    iconTile = LetifyColors.TileViolet,
                    title = "Свайп назад",
                    value = if (state.swipeBackEnabled) "Включён" else "Выключен",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = state.swipeBackEnabled,
                            onCheckedChange = { state.swipeBackEnabled = it },
                        )
                    },
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 8.dp),
            ) {
                Text(
                    "Когда выключено, экраны нельзя закрывать свайпом от левого края — только кнопкой «Назад».",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TransitionOptionCard(
    style: TransitionStyle,
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor =
        if (selected) Letify.colors.accent
        else Letify.colors.text.copy(alpha = 0.07f)
    Column(
        modifier
            .clip(shape)
            .background(Letify.colors.container)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .noFeedbackClick(onClick = onSelect),
    ) {
        // Full-bleed preview, flush to the card top/sides (no inner framing gap).
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f)
                .clip(RoundedCornerShape(topStart = 19.dp, topEnd = 19.dp)),
        ) {
            TransitionPreview(style = style, modifier = Modifier.fillMaxSize())
            // Selection badge in the corner.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) Letify.colors.accent
                        else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f),
                    )
                    .let {
                        if (selected) it
                        else it.border(
                            1.5.dp,
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f),
                            RoundedCornerShape(999.dp),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    SolarIcon(
                        name = "check-bold",
                        tint = androidx.compose.ui.graphics.Color.White,
                        size = 13.dp,
                    )
                }
            }
        }
        // Fixed single-line subtitle keeps BOTH cards exactly the same height
        // regardless of text length (the previous 2-line wrap on "Наплыв" made
        // that card taller than "Сдвиг").
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
            Text(title, color = Letify.colors.text, style = Letify.typography.titleSmall)
            Box(Modifier.height(2.dp))
            Text(
                subtitle,
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

/**
 * A small looping schematic of a transition. Two fake "screens" are driven by
 * the SAME 0..1 progress formula the real overlay uses, so the preview behaves
 * exactly like the option it represents:
 *   - back (outgoing): translationX = -(1-p) * w * hostShift   (1.0 push / 0.16 cover) + iOS-style dim on cover
 *   - front (incoming): translationX = p * w, with rounded leading corners in cover
 */
@Composable
private fun TransitionPreview(style: TransitionStyle, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "transitionPreview")
    // Cover rests slightly OPEN (restP=0.16) so the incoming screen's rounded
    // leading corner always sits over the still-visible back screen — that reads
    // as an intentional stack, not the dark notch/gap the user reported. Push
    // rests fully closed (one seamless canvas, sharp edges).
    val restP = if (style == TransitionStyle.Cover) 0.16f else 0f
    val p by transition.animateFloat(
        initialValue = 1f,
        targetValue = restP,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1700
                1f at 0
                1f at 350 using PreviewEasing
                restP at 1200
                restP at 1700
            },
            repeatMode = RepeatMode.Reverse,
        ),
        label = "p",
    )
    val shift = overlayHostShiftFraction(style)
    // Constant radius for Cover so the curve is clearly visible the whole time
    // (matches the real transition, which now also holds full radius while moving).
    val radiusPx = with(LocalDensity.current) {
        (if (style == TransitionStyle.Cover) 13.dp else 0.dp).toPx()
    }
    // Parent already clips the card's top corners; fill flush (no inner frame gap).
    Box(modifier.background(Letify.colors.bg)) {
        MiniScreen(
            accent = false,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = -(1f - p) * size.width * shift },
        )
        // Cover only: dim the receding back screen, mirroring the real transition's
        // iOS-style parallax-dim so the preview matches what ships.
        if (style == TransitionStyle.Cover) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -(1f - p) * size.width * shift
                        alpha = (1f - p) * 0.16f
                    }
                    .background(Color.Black),
            )
        }
        MiniScreen(
            accent = true,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = p * size.width
                    // Symmetric radius → Outline.Rounded (GPU setClipToOutline +
                    // cheap shadow), not an Outline.Generic Path that would force
                    // a per-frame clipPath/shadow-path on this continuously
                    // looping preview. The trailing corners are off-screen while
                    // the card is shifted right, so this looks identical to the
                    // real transition's leading-corner rounding.
                    shape = RoundedCornerShape(radiusPx)
                    clip = true
                    shadowElevation = 10f
                },
        )
    }
}

/** A schematic fake app screen used inside [TransitionPreview]. */
@Composable
private fun MiniScreen(accent: Boolean, modifier: Modifier) {
    val screenBg = if (accent) Letify.colors.container else Letify.colors.bg
    val headerColor = if (accent) Letify.colors.accent else Letify.colors.muted
    val avatarColor =
        if (accent) Letify.colors.accent.copy(alpha = 0.55f)
        else Letify.colors.text.copy(alpha = 0.14f)
    val cardColor =
        if (accent) Letify.colors.accent.copy(alpha = 0.16f)
        else Letify.colors.text.copy(alpha = 0.06f)
    val lineColor = Letify.colors.text.copy(alpha = 0.08f)
    Column(
        modifier
            .background(screenBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(14.dp).clip(RoundedCornerShape(999.dp)).background(avatarColor))
            Box(
                Modifier
                    .width(48.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(headerColor),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(cardColor),
        )
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth(if (it == 2) 0.55f else 1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(lineColor),
            )
        }
    }
}

private fun tileForTab(tab: Tab): androidx.compose.ui.graphics.Color = when (tab) {
    Tab.Home -> LetifyColors.TileBlue
    Tab.Nutrition -> LetifyColors.TileOrange
    Tab.Plan -> LetifyColors.TileViolet
    Tab.Profile -> LetifyColors.TileTeal
}

