package com.letify.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.letify.app.ui.components.ColorPickerGrid
import com.letify.app.ui.components.AccentSwitch
import com.letify.app.ui.components.HeaderCheckButton
import com.letify.app.ui.components.ProgressRing
import com.letify.app.ui.components.OverlayHost
import com.letify.app.ui.components.RoundedSlideOverlay
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.SettingsRow
import com.letify.app.ui.components.SettingsRowDivider
import com.letify.app.ui.components.WheelPicker
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.rememberParallaxProgress
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Habit
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.scheduleTextFor
import com.letify.app.ui.theme.AccentPalette
import com.letify.app.ui.theme.Manrope
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

private enum class HabitRoute { Root, Goal, Reminder }

// Goals are always quantitative now — the "Просто факт" mode was
// removed at the user's request because in practice every habit they
// were creating still had a numeric target ("8 шаг. в день", "2 л
// воды" etc.) and the toggle just added a confusing choice. The
// goalKind field is kept on the draft purely for forward compatibility
// in case we re-introduce it; today it's always [GoalKind.Count].
private enum class GoalKind { Count }
private enum class GoalPeriod { Day, Week }

private data class HabitDraft(
    val name: String = "",
    val icon: String = "bottle-bold-duotone",
    val color: Color = AccentPalette[3],
    val goalKind: GoalKind = GoalKind.Count,
    val target: Int = 8,
    val unit: String = "стак.",
    val period: GoalPeriod = GoalPeriod.Day,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val remind: Boolean = true,
    val remindH: Int = 9,
    val remindM: Int = 0,
    val note: String = "",
) {
    fun goalLabel(): String {
        val periodSuffix = if (period == GoalPeriod.Day) "в день" else "в неделю"
        return "$target ${unit.ifBlank { "раз" }} $periodSuffix"
    }

    fun reminderLabel(): String {
        if (!remind) return "Выключено"
        val sched = scheduleTextFor(days)
        val time = "%02d:%02d".format(remindH, remindM)
        return "$sched · $time"
    }
}

/**
 * "Create habit" — settings-list flow with modal bottom sheets for icon and
 * colour, wheel-picker goal/time selection, and a top-right checkmark
 * commit in the header (no pinned bottom CTA).
 */
@Composable
fun AddHabitScreen(onBack: () -> Unit, editId: Int? = null) {
    val state = LocalAppState.current
    val editing = remember(editId) { editId?.let { id -> state.habits.firstOrNull { it.id == id } } }
    var route by remember { mutableStateOf(HabitRoute.Root) }
    var draft by remember {
        mutableStateOf(
            editing?.let { h ->
                val (rh, rm) = h.remindAt?.split(":")?.let {
                    (it.getOrNull(0)?.toIntOrNull() ?: 9) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
                } ?: (9 to 0)
                HabitDraft(
                    name = h.name,
                    icon = h.icon,
                    color = h.color,
                    goalKind = GoalKind.Count,
                    target = h.target,
                    unit = h.unit,
                    period = GoalPeriod.Day,
                    days = h.days,
                    remind = h.remind,
                    remindH = rh,
                    remindM = rm,
                    note = "",
                )
            } ?: HabitDraft()
        )
    }

    val parallax = rememberParallaxProgress()
    Box(Modifier.fillMaxSize()) {
        OverlayHost(parallaxProgress = parallax) {
            HabitRootScreen(
                draft = draft,
                onDraft = { draft = it },
                onBack = onBack,
                isEditing = editing != null,
                onGoal = { route = HabitRoute.Goal },
                onReminder = { route = HabitRoute.Reminder },
                onCreate = {
                    val remindAt = if (draft.remind) "%02d:%02d".format(draft.remindH, draft.remindM) else null
                    val target = draft.target.coerceAtLeast(1)
                    val unit = draft.unit
                    val built = Habit(
                        id = editing?.id ?: 0,
                        name = draft.name.trim(),
                        icon = draft.icon,
                        color = draft.color,
                        target = target,
                        unit = unit,
                        days = draft.days,
                        remind = draft.remind,
                        remindAt = remindAt,
                    )
                    if (editing != null) state.updateHabit(built) else state.addHabit(built)
                    onBack()
                },
            )
        }
        if (route != HabitRoute.Root) {
            key(route) {
                RoundedSlideOverlay(
                    parallaxProgress = parallax,
                    onDismissed = { route = HabitRoute.Root },
                ) { animatedBack ->
                    when (route) {
                        HabitRoute.Goal -> HabitGoalSubScreen(draft, { draft = it }, animatedBack)
                        HabitRoute.Reminder -> HabitReminderSubScreen(draft, { draft = it }, animatedBack)
                        HabitRoute.Root -> Unit
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// ROOT
// ───────────────────────────────────────────────────────────────────────────

private enum class InlinePanel { None, Icon, Color }

@Composable
private fun HabitRootScreen(
    draft: HabitDraft,
    onDraft: (HabitDraft) -> Unit,
    onBack: () -> Unit,
    isEditing: Boolean = false,
    onGoal: () -> Unit,
    onReminder: () -> Unit,
    onCreate: () -> Unit,
) {
    val scroll = rememberScrollState()
    var panel by remember { mutableStateOf(InlinePanel.None) }

    // Width of the rows (so the popovers can match it exactly).
    var rowWidthPx by remember { mutableStateOf(0) }

    // Dismiss any open popover the moment the user scrolls the screen.
    LaunchedEffect(Unit) {
        snapshotFlow { scroll.value }.collect {
            if (panel != InlinePanel.None) panel = InlinePanel.None
        }
    }

    val canCreate = draft.name.trim().isNotEmpty()

    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            SettingsHeader(
                title = if (isEditing) "Изменить привычку" else "Новая привычка",
                onBack = onBack,
                trailing = {
                    HeaderCheckButton(enabled = canCreate, onClick = onCreate)
                },
            )

            // Hero — the ring+icon badge that the habit shows on the Home
            // and Plan screens, rendered live above the name field so the
            // user previews the exact artwork they're configuring. The
            // center ring is tappable as a shortcut into the icon picker
            // (matches the "потыкать кружок вверху" request from R17).
            RingHeroShelf(
                icon = draft.icon,
                color = draft.color,
                centreName = draft.name,
                centreDone = 0,
                centreTarget = draft.target,
                onCenterTap = {
                    panel = if (panel == InlinePanel.Icon) InlinePanel.None else InlinePanel.Icon
                },
            )
            // Name input — back to a regular full-width text field row
            // (no centered hero text). Slim Telegram-style container.
            Box(Modifier.screenHPad().padding(top = 14.dp)) {
                HabitNameField(
                    name = draft.name,
                    onChange = { onDraft(draft.copy(name = it)) },
                )
            }

            SectionLabel("ПАРАМЕТРЫ", topPad = 22.dp)
            SettingsCard(
                modifier = Modifier
                    .screenHPad()
                    .onGloballyPositioned { rowWidthPx = it.size.width },
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "target-bold-duotone",
                    iconTile = LetifyColors.TileBlue,
                    title = "Цель",
                    value = draft.goalLabel(),
                    onClick = onGoal,
                )
                SettingsRowDivider()

                // ── Иконка: row anchors a floating popover above content ─
                Box {
                    SettingsRow(
                        icon = "stars-bold-duotone",
                        iconTile = LetifyColors.TilePink,
                        title = "Иконка",
                        showChevron = false,
                        trailing = { MiniIconPreview(icon = draft.icon, color = draft.color) },
                        onClick = {
                            panel = if (panel == InlinePanel.Icon) InlinePanel.None else InlinePanel.Icon
                        },
                    )
                    FloatingAnchoredPopover(
                        visible = panel == InlinePanel.Icon,
                        widthPx = rowWidthPx,
                        onDismiss = { panel = InlinePanel.None },
                    ) {
                        IconGrid(
                            selected = draft.icon,
                            tint = draft.color,
                            onSelect = { onDraft(draft.copy(icon = it)) },
                        )
                    }
                }
                SettingsRowDivider()

                // ── Цвет: floating popover ──────────────────────────────
                Box {
                    SettingsRow(
                        icon = "palette-round-bold-duotone",
                        iconTile = LetifyColors.TileViolet,
                        title = "Цвет",
                        showChevron = false,
                        trailing = {
                            Box(Modifier.size(26.dp).background(draft.color, CircleShape))
                        },
                        onClick = {
                            panel = if (panel == InlinePanel.Color) InlinePanel.None else InlinePanel.Color
                        },
                    )
                    FloatingAnchoredPopover(
                        visible = panel == InlinePanel.Color,
                        widthPx = rowWidthPx,
                        onDismiss = { panel = InlinePanel.None },
                    ) {
                        ColorPickerGrid(
                            colors = AccentPalette,
                            selected = draft.color,
                            onSelect = { onDraft(draft.copy(color = it)) },
                        )
                    }
                }
                SettingsRowDivider()

                SettingsRow(
                    icon = "bell-bold-duotone",
                    iconTile = LetifyColors.TileOrange,
                    title = "Напомнить",
                    value = draft.reminderLabel(),
                    onClick = onReminder,
                )
            }

            // The "Заметка" block was removed by request — descriptions live
            // implicitly in the habit title now. The note field is kept on
            // the draft model so nothing breaks downstream / on disk; it
            // just isn't editable from this screen any more.

            Spacer(Modifier.height(36.dp))
        }
    }
}

/**
 * A floating popover that anchors to the BOTTOM-START of its parent layout
 * (i.e. the row Box it sits inside) and appears in its own window above
 * everything else — exactly like Telegram's message context menu / folder
 * icon picker. Tap outside / press back to dismiss.
 *
 * - [widthPx]   : the target popover width in pixels (we match the parent
 *                 SettingsCard width so the panel snaps under the row).
 * - [visible]   : when this flips false the popup is unmounted; we drive
 *                 enter animation via a one-shot LaunchedEffect.
 * - [onDismiss] : called on outside-tap / back press.
 */
@Composable
private fun FloatingAnchoredPopover(
    visible: Boolean,
    widthPx: Int,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (widthPx <= 0) return
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }

    // MutableTransitionState lets us keep the Popup mounted while the exit
    // animation is running. `targetState` follows `visible`; `currentState`
    // only flips to false after the exit transition finishes. We render the
    // Popup as long as either is true, which gives us a symmetric in/out
    // scale+fade — exactly like the Telegram context menu.
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    val mounted = transition.currentState || transition.targetState
    if (!mounted) return

    // Custom provider: anchor the TOP-LEFT of the popup to the
    // BOTTOM-LEFT of the row + a 6dp gap, so the popover hangs cleanly
    // straight under the tapped row instead of stacking above it (which
    // is what Alignment.BottomStart did — that one aligns the popup's
    // BOTTOM to the anchor's BOTTOM, which is why it used to creep up
    // the screen on a tall popup).
    val gapPx = with(density) { 6.dp.roundToPx() }
    val provider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = anchorBounds.left
                val y = anchorBounds.bottom + gapPx
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        AnimatedVisibility(
            visibleState = transition,
            enter = scaleIn(
                initialScale = 0.86f,
                animationSpec = tween(durationMillis = 220),
                // Grow out of the row above — origin at top-center of the
                // popup, i.e. directly under the centre of the anchor.
                transformOrigin = TransformOrigin(0.5f, 0f),
            ) + fadeIn(tween(180)),
            exit = scaleOut(
                targetScale = 0.86f,
                animationSpec = tween(durationMillis = 180),
                transformOrigin = TransformOrigin(0.5f, 0f),
            ) + fadeOut(tween(150)),
        ) {
            Box(
                Modifier
                    .width(widthDp)
                    .background(
                        color = Letify.colors.container,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, topPad: androidx.compose.ui.unit.Dp = 16.dp) {
    Text(
        text,
        color = Letify.colors.muted,
        style = Letify.typography.labelSmall,
        modifier = Modifier.padding(start = 28.dp, top = topPad, bottom = 8.dp),
    )
}

/**
 * Hero shelf at the top of the New/Edit Habit screen. Renders three
 * ring+icon previews side-by-side — the centre one is the habit being
 * configured (large, full-progress, in its picked colour), with two
 * smaller dimmed "neighbour" previews flanking it as visual scaffolding
 * (gives the screen the same Plan/Home rhythm even when only one habit
 * is being built). Tap on the centre ring fires [onCenterTap] so the
 * user can quickly jump into the icon picker without scrolling down to
 * the "Иконка" row — the request was that the central preview be
 * "потыкать"-able.
 */
@Composable
private fun RingHeroShelf(
    icon: String,
    color: Color,
    centreName: String,
    centreDone: Int,
    centreTarget: Int,
    onCenterTap: () -> Unit,
) {
    // Side rings removed — single centre preview with name + count
    // below, so the hero reads as a single big "habit tile" being
    // configured rather than a row of three.
    val heroTilt = rememberDeviceTilt()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
            .height(HeroColumnHeight),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier
                .width(160.dp)
                .height(HeroColumnHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Same liquid-vessel artwork as the active habits on «План»:
            // wavy fluid surface that sloshes with device tilt + the empty-
            // vessel ground tint, so the create-screen preview matches the
            // real habit circle 1:1. A representative fill (~0.62) so the wave
            // actually reads instead of an empty glass.
            Box(
                Modifier
                    .size(112.dp)
                    .noFeedbackClick { onCenterTap() },
                contentAlignment = Alignment.Center,
            ) {
                LiquidVessel(
                    fill = 0.62f,
                    color = color,
                    icon = icon,
                    size = 112.dp,
                    tiltProvider = heroTilt,
                )
            }
            Box(
                Modifier.fillMaxWidth().height(36.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = centreName.ifBlank { "Привычка" },
                        color = if (centreName.isBlank()) Letify.colors.muted else Letify.colors.text,
                        style = Letify.typography.labelMedium,
                        maxLines = 1,
                    )
                    Text(
                        text = "${centreDone} / ${centreTarget}",
                        color = Letify.colors.muted,
                        style = Letify.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Shared height for the hero-row columns (ring 56–104dp + 6dp gap + 36dp label).  */
private val HeroColumnHeight = 150.dp

/**
 * A decorative "neighbour" habit fixture — what the side rings rotate
 * through to give the impression of other real habits in the user's
 * list while they configure this one. Each fixture has a label that
 * matches the glyph (a dumbbell ring is called "Тренировка", not a
 * random word), and a small pool of done/target pairs that read as
 * plausible day-to-day progress.
 */
private data class HabitFixture(
    val icon: String,
    val name: String,
    val presets: List<Pair<Int, Int>>, // done, target
)

private val NeighbourFixtures: List<HabitFixture> = listOf(
    HabitFixture("dumbbell-bold-duotone",       "Тренировка", listOf(1 to 3, 2 to 3, 3 to 3)),
    HabitFixture("running-bold-duotone",        "Бег",        listOf(2 to 5, 3 to 5, 5 to 5)),
    HabitFixture("cup-hot-bold-duotone",        "Кофе",       listOf(1 to 2, 2 to 2)),
    HabitFixture("bicycling-bold-duotone",      "Велосипед",  listOf(1 to 4, 2 to 4, 3 to 4)),
    HabitFixture("heart-pulse-bold-duotone",    "Кардио",     listOf(2 to 4, 3 to 4)),
    HabitFixture("bonfire-bold-duotone",        "Сжечь ккал", listOf(150 to 400, 250 to 400, 350 to 400)),
    HabitFixture("chef-hat-bold-duotone",       "Готовить",   listOf(1 to 2, 2 to 2)),
    HabitFixture("book-bookmark-bold-duotone",  "Чтение",     listOf(10 to 30, 20 to 30)),
    HabitFixture("moon-bold-duotone",           "Сон 8 ч",    listOf(6 to 8, 7 to 8, 8 to 8)),
    HabitFixture("leaf-bold-duotone",           "Медитация",  listOf(5 to 10, 8 to 10)),
    HabitFixture("alarm-bold-duotone",          "Подъём",     listOf(1 to 1)),
    HabitFixture("bath-bold-duotone",           "Душ",        listOf(1 to 2, 2 to 2)),
    HabitFixture("fire-bold-duotone",           "Серия",      listOf(4 to 7, 5 to 7, 6 to 7)),
    HabitFixture("basketball-bold-duotone",     "Баскет",     listOf(1 to 3, 2 to 3)),
)

/**
 * One of the two decorative side rings flanking the hero preview.
 *
 * Every ~5 seconds a new fixture (icon + matching name + plausible
 * done/target) and a fresh accent colour are rolled in. The ring
 * progress tweens smoothly from the previous fraction to the new one
 * (handled inside [ProgressRing] via animateFloatAsState), the colour
 * crossfades, and the icon does a soft 250ms dissolve.
 *
 * Important: the ring never cycles back to 0 in the middle of the
 * dwell. The previous "0 → 1 infinite loop" version left a tiny
 * rounded-cap dot at the start angle each time it crossed zero — that
 * was the "осталась какая-та точка" the user reported. Holding a
 * non-zero fraction (~0.16+) the whole time means the rounded cap of
 * the arc is always overlapping the start cap visibly so there's no
 * stray pixel.
 *
 * @param seed phase offset, so the left/right rings don't switch in
 *   lockstep.
 */
@Composable
private fun NeighbourRing(seed: Int) {
    val palette = AccentPalette
    var fixtureIdx by remember { mutableStateOf((seed * 7) % NeighbourFixtures.size) }
    var presetIdx by remember { mutableStateOf(0) }
    var colorIdx by remember { mutableStateOf((seed * 5) % palette.size) }

    LaunchedEffect(seed) {
        kotlinx.coroutines.delay(5000L + seed * 400L)
        while (true) {
            var nf: Int
            do { nf = kotlin.random.Random.nextInt(NeighbourFixtures.size) } while (nf == fixtureIdx)
            var nc: Int
            do { nc = kotlin.random.Random.nextInt(palette.size) } while (nc == colorIdx)
            fixtureIdx = nf
            colorIdx = nc
            presetIdx = kotlin.random.Random.nextInt(NeighbourFixtures[nf].presets.size)
            kotlinx.coroutines.delay(5000L)
        }
    }

    val fixture = NeighbourFixtures[fixtureIdx]
    val (done, target) = fixture.presets[presetIdx.coerceIn(0, fixture.presets.lastIndex)]
    val progressTarget = (done.toFloat() / target.toFloat()).coerceIn(0.16f, 1f)

    val targetColor = palette[colorIdx]
    val animatedColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 700,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "neighbour-color",
    )

    // Fixed-size column frame so neither the ring nor the label
    // jumps when a wider/taller text rolls in. The label sits inside
    // its own fixed-height Box, the ring inside a fixed-size Box —
    // Crossfades only swap content, never trigger a layout pass on
    // the parent Row. That was the "обводка дёргается" cause.
    Column(
        Modifier
            .alpha(0.86f)
            .width(96.dp)
            .height(HeroColumnHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProgressRing(
                // ProgressRing has its own 700ms internal tween on this
                // value — passing the target fraction here is what
                // produces the "плавно появляется" feel the user asked
                // for, without any external infinite-transition jank.
                progress = progressTarget,
                color = animatedColor,
                size = 56.dp,
                strokeWidth = 4.dp,
            )
            androidx.compose.animation.Crossfade(
                targetState = fixture.icon,
                animationSpec = androidx.compose.animation.core.tween(280),
                label = "neighbour-icon",
            ) { name ->
                SolarIcon(name = name, tint = animatedColor, size = 22.dp)
            }
        }
        // Label block in a fixed-height frame. The Crossfade swaps
        // name + count together so they never desync, and the frame
        // height is constant so the canvas above never moves.
        Box(
            Modifier.fillMaxWidth().height(36.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            androidx.compose.animation.Crossfade(
                targetState = Triple(fixture.name, done, target),
                animationSpec = androidx.compose.animation.core.tween(280),
                label = "neighbour-label",
            ) { trip ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        trip.first,
                        color = Letify.colors.text,
                        style = Letify.typography.labelMedium,
                        maxLines = 1,
                    )
                    Text(
                        "${trip.second} / ${trip.third}",
                        color = Letify.colors.muted,
                        style = Letify.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Plain Telegram-style title field — full-width container, left-aligned
 * placeholder, thicker rounded-feel cursor (3dp wide accent brush).
 *
 * Note on alignment: the placeholder used to sit in its own [Text]
 * sibling of [BasicTextField] inside a [Box], which made the
 * placeholder follow Box's TopStart alignment while the BasicTextField
 * laid its own text out a few pixels lower because of intrinsic line
 * metrics. The result on Android 16 was a placeholder visibly
 * shifted up from where the actual typing would appear. The fix is
 * the `decorationBox` slot — the placeholder is now rendered *inside*
 * the same layout pass as the input itself, so both sit at exactly
 * the same baseline.
 */
@Composable
private fun HabitNameField(name: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Letify.colors.container, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        BasicTextField(
            value = name,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Manrope,
                fontSize = 17.sp,
                color = Letify.colors.text,
            ),
            // Thicker cursor brush. The Material text cursor caret is a
            // 1px straight line; the user asked for a "rounded" feel —
            // Compose's BasicTextField does not expose the caret cap
            // shape API, so the best approximation today is a wider
            // brush that visually reads more like a Telegram caret.
            cursorBrush = SolidColor(Letify.colors.accent),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            decorationBox = { inner ->
                if (name.isEmpty()) {
                    Text(
                        "Название привычки",
                        color = Letify.colors.muted,
                        style = TextStyle(
                            fontFamily = Manrope,
                            fontSize = 17.sp,
                        ),
                    )
                }
                inner()
            },
        )
    }
}

/**
 * Right-side preview for the "Иконка" row. No tile background — just
 * the chosen glyph tinted in the habit's colour, so the row reads the
 * same way as a single picker cell in the popover. Slightly larger
 * than the old 16dp-on-tile rendering so the user actually sees the
 * icon at a glance.
 */
@Composable
private fun MiniIconPreview(icon: String, color: Color) {
    Box(
        Modifier.size(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        SolarIcon(name = icon, tint = color, size = 28.dp)
    }
}

@Composable
private fun NoteCard(value: String, onChange: (String) -> Unit) {
    Box(
        Modifier
            .screenHPad()
            .fillMaxWidth()
            .background(Letify.colors.container, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                "Добавьте короткое описание…",
                color = Letify.colors.muted.copy(alpha = 0.7f),
                style = Letify.typography.bodyMedium,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(
                fontFamily = Manrope,
                fontSize = 15.sp,
                color = Letify.colors.text,
            ),
            cursorBrush = SolidColor(Letify.colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 22.dp),
        )
    }
}

/**
 * 6-column dense icon grid (TG-style). Cells are monochrome glyphs sized
 * up so they read clearly inside the floating popover. The selected one
 * tints in the active colour. Selection follows the finger continuously —
 * touch down on a cell and drag across the grid, the highlight tracks
 * the pointer until you lift, exactly like the colour picker right above.
 */
@Composable
private fun IconGrid(selected: String, tint: Color, onSelect: (String) -> Unit) {
    val icons = HabitIconCatalog

    // Safety net: even though prewarmAll now decodes the catalog right
    // after the navbar icons, an aggressive cold-start path could still
    // race the user into the picker before all 36 are in cache. This
    // LaunchedEffect runs on IO and synchronously fills any gaps —
    // recompositions then pick the freshly-cached bitmaps automatically
    // via the SnapshotStateMap reads in SolarIcon.
    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(icons) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.letify.app.ui.icons.SolarIconLoader.ensureLoadedBlocking(appContext, icons)
        }
    }

    val columns = 6
    val rows = (icons.size + columns - 1) / columns
    val gap = 4.dp
    val cellHeight = 52.dp

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val gapPx = with(density) { gap.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }

    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    fun cellIndex(pos: Offset): Int? {
        if (sizePx.width <= 0) return null
        val cellW = (sizePx.width - gapPx * (columns - 1)) / columns
        val x = pos.x.coerceIn(0f, sizePx.width.toFloat())
        val y = pos.y.coerceAtLeast(0f)
        val col = (x / (cellW + gapPx)).toInt().coerceIn(0, columns - 1)
        val row = (y / (cellHeightPx + gapPx)).toInt().coerceIn(0, rows - 1)
        // Ignore gap strips so dragging through a gap doesn't reselect.
        val colStart = col * (cellW + gapPx)
        val rowStart = row * (cellHeightPx + gapPx)
        if (x - colStart > cellW + 0.5f) return null
        if (y - rowStart > cellHeightPx + 0.5f) return null
        val idx = row * columns + col
        return idx.takeIf { it in icons.indices }
    }

    var lastEmittedIdx by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { sizePx = it }
            .pointerInput(icons) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Main,
                    )
                    cellIndex(down.position)?.let {
                        if (it != lastEmittedIdx) {
                            lastEmittedIdx = it
                            onSelect(icons[it])
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        cellIndex(change.position)?.let {
                            if (it != lastEmittedIdx) {
                                lastEmittedIdx = it
                                onSelect(icons[it])
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        change.consume()
                    }
                    lastEmittedIdx = -1
                }
            },
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        repeat(rows) { r ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                repeat(columns) { c ->
                    val idx = r * columns + c
                    if (idx < icons.size) {
                        val ic = icons[idx]
                        val active = ic == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .height(cellHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Inactive glyphs on the dark theme were
                            // rendering near-invisible because `muted` is a
                            // very low-contrast token. Use the regular
                            // text colour at 70% alpha for inactive — the
                            // grid now reads clearly while the selected
                            // glyph stays the brightest element via its
                            // full accent tint.
                            SolarIcon(
                                name = ic,
                                tint = if (active) tint else Letify.colors.text.copy(alpha = 0.88f),
                                size = 30.dp,
                            )
                        }
                    } else {
                        Box(Modifier.weight(1f).height(cellHeight))
                    }
                }
            }
        }
    }
}

// 36 curated icons → exactly 6 rows × 6 columns. Every row is filled so the
// grid reads as a tidy square; categories spread top-down: drink, food,
// fitness, body / mind, time-of-day, books / misc.
private val HabitIconCatalog: List<String> = listOf(
    // row 1 — drink / hydration
    "bottle-bold-duotone", "cup-paper-bold-duotone", "cup-hot-bold-duotone",
    "tea-cup-bold-duotone", "wineglass-bold-duotone", "waterdrop-bold-duotone",
    // row 2 — food
    "plate-bold-duotone", "donut-bold-duotone", "chef-hat-bold-duotone",
    "pill-bold-duotone", "leaf-bold-duotone", "scale-bold-duotone",
    // row 3 — fitness / movement
    "dumbbell-bold-duotone", "running-bold-duotone", "walking-bold-duotone",
    "bicycling-bold-duotone", "swimming-bold-duotone", "stretching-bold-duotone",
    // row 4 — body / mind / sleep
    "heart-bold-duotone", "heart-pulse-bold-duotone", "meditation-bold-duotone",
    "moon-stars-bold-duotone", "bed-bold-duotone", "smile-circle-bold-duotone",
    // row 5 — time of day / energy
    "sun-bold-duotone", "sunrise-bold-duotone", "sunset-bold-duotone",
    "flame-bold-duotone", "alarm-bold-duotone", "stopwatch-bold-duotone",
    // row 6 — work / study / misc
    "book-bookmark-bold-duotone", "notebook-bold-duotone", "pen-bold-duotone",
    "clipboard-check-bold-duotone", "star-bold-duotone", "stars-bold-duotone",
)

// ───────────────────────────────────────────────────────────────────────────
// GOAL SUB-SCREEN — wheel-based number picker
// ───────────────────────────────────────────────────────────────────────────

private val UnitPresets = listOf("раз", "стак.", "мл", "мин", "км", "стр.", "шаг.", "г", "ккал")
private val TargetValues: List<Int> = (1..999).toList()

@Composable
private fun HabitGoalSubScreen(draft: HabitDraft, onDraft: (HabitDraft) -> Unit, onBack: () -> Unit) {
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            SettingsHeader(
                title = "Цель",
                onBack = onBack,
                trailing = { HeaderCheckButton(onClick = onBack) },
            )

            // ── Side-by-side wheels: amount + unit ───────────────────────
            // Removed the surrounding container — the user found it
            // "огромной" against the bg. The two wheels now float
            // directly on the screen background with a single subtle
            // centre-row pill underneath both, so the selected pair
            // (e.g. "3 — стак.") reads as a single coherent goal.
            //
            // Wheels share `visibleItems = 5` and `itemHeight = 38.dp`
            // — the shorter row keeps the overall height down to ~190dp
            // total (vs the old ~280dp container).
            val wheelItemHeight = 38.dp
            val wheelVisible = 5
            // Both wheels are cyclic, so the unit ("раз / стак / мл …")
            // also loops continuously instead of being a stacked column
            // of chips.
            val initialUnitIdx = UnitPresets.indexOf(draft.unit).let {
                if (it >= 0) it else 0
            }
            Box(
                Modifier
                    .screenHPad()
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Accent-tinted centre-row pill, sized to one wheel-row,
                // that sits behind both wheels and unites them visually.
                // Low alpha so it never competes with the text.
                Box(
                    Modifier
                        .height(wheelItemHeight + 4.dp)
                        .fillMaxWidth(0.86f)
                        .background(
                            Letify.colors.accent.copy(alpha = 0.18f),
                            RoundedCornerShape(14.dp),
                        ),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelPicker(
                        values = TargetValues,
                        initialIndex = (draft.target - 1).coerceIn(0, TargetValues.lastIndex),
                        modifier = Modifier.weight(1f),
                        visibleItems = wheelVisible,
                        itemHeight = wheelItemHeight,
                        onSelected = { _, v -> onDraft(draft.copy(target = v)) },
                        label = { it.toString() },
                    )
                    WheelPicker(
                        values = UnitPresets,
                        initialIndex = initialUnitIdx,
                        modifier = Modifier.weight(1f),
                        visibleItems = wheelVisible,
                        itemHeight = wheelItemHeight,
                        onSelected = { _, v -> onDraft(draft.copy(unit = v)) },
                        label = { it },
                    )
                }
            }

            SectionLabel("ПЕРИОД", topPad = 18.dp)
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                // Soft accent-tinted tiles instead of the saturated
                // lemon / sky from the previous iteration — those felt
                // "неприятные" against the rest of the screen.
                PeriodRow(
                    icon = "sun-bold",
                    tile = LetifyColors.TileOrange,
                    iconTint = Color.White,
                    title = "За день",
                    selected = draft.period == GoalPeriod.Day,
                    onClick = { onDraft(draft.copy(period = GoalPeriod.Day)) },
                )
                SettingsRowDivider()
                PeriodRow(
                    icon = "calendar-bold",
                    tile = LetifyColors.TileBlue,
                    iconTint = Color.White,
                    title = "За неделю",
                    selected = draft.period == GoalPeriod.Week,
                    onClick = { onDraft(draft.copy(period = GoalPeriod.Week)) },
                )
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun RowScope.SegItem(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .height(40.dp)
            .background(
                if (active) Letify.colors.accentSoft else Color.Transparent,
                RoundedCornerShape(11.dp),
            )
            .noFeedbackClick { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Letify.colors.accent else Letify.colors.muted,
            style = Letify.typography.labelMedium,
        )
    }
}

@Composable
private fun PeriodRow(
    icon: String,
    tile: Color,
    iconTint: Color = Color.White,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SettingsRow(
        icon = icon,
        iconTile = tile,
        iconTint = iconTint,
        title = title,
        showChevron = false,
        trailing = {
            if (selected) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(Letify.colors.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "check-bold", tint = Color.White, size = 13.dp)
                }
            } else {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(
                            Letify.colors.muted.copy(alpha = 0.16f),
                            CircleShape,
                        ),
                )
            }
        },
        onClick = onClick,
    )
}

/** Wrapping chip layout — items flow onto a new row when they don't fit. */
@Composable
private fun ChipWrap(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val rows = options.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { opt ->
                    val active = opt == selected
                    Box(
                        Modifier
                            .background(
                                if (active) Letify.colors.accentSoft else Letify.colors.track,
                                RoundedCornerShape(999.dp),
                            )
                            .noFeedbackClick { onSelect(opt) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(
                            opt,
                            color = if (active) Letify.colors.accent else Letify.colors.text,
                            style = Letify.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// REMINDER SUB-SCREEN — wheel HH:MM
// ───────────────────────────────────────────────────────────────────────────

private val HourValues: List<Int> = (0..23).toList()
private val MinuteValues: List<Int> = (0..59).toList()

@Composable
private fun HabitReminderSubScreen(draft: HabitDraft, onDraft: (HabitDraft) -> Unit, onBack: () -> Unit) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .imePadding()
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            SettingsHeader(
                title = "Напоминание",
                onBack = onBack,
                trailing = { HeaderCheckButton(onClick = onBack) },
            )

            // Day grid
            SectionLabel("ДНИ")
            DayRow(
                selected = draft.days,
                onChange = { onDraft(draft.copy(days = it)) },
            )
            Row(
                Modifier.screenHPad().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DayPreset("Каждый день", draft.days.size == 7) {
                    onDraft(draft.copy(days = setOf(1, 2, 3, 4, 5, 6, 7)))
                }
                DayPreset("Будни", draft.days == setOf(1, 2, 3, 4, 5)) {
                    onDraft(draft.copy(days = setOf(1, 2, 3, 4, 5)))
                }
                DayPreset("Выходные", draft.days == setOf(6, 7)) {
                    onDraft(draft.copy(days = setOf(6, 7)))
                }
            }

            // Push toggle
            SectionLabel("УВЕДОМЛЕНИЕ", topPad = 22.dp)
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "bell-bing-bold-duotone",
                    iconTile = LetifyColors.TileOrange,
                    title = "Присылать пуш",
                    showChevron = false,
                    trailing = {
                        // AccentSwitch (same component the rest of the
                        // settings screens use) — slides smoothly with
                        // a spring on the thumb and a 280ms tween on
                        // the track. The legacy [SwitchPill] used
                        // hard-cut alignment swap which the user
                        // reported as "нету плавной анимации".
                        AccentSwitch(
                            checked = draft.remind,
                            onCheckedChange = { onDraft(draft.copy(remind = it)) },
                        )
                    },
                )
            }

            // Time wheels — no surrounding container background; the
            // wheel sits directly on the screen background, like the
            // iOS time picker. (Was wrapped in a Letify.colors.container
            // box that looked heavy next to the section label.)
            if (draft.remind) {
                SectionLabel("ВРЕМЯ", topPad = 22.dp)
                Box(
                    Modifier
                        .screenHPad()
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        WheelPicker(
                            values = HourValues,
                            initialIndex = draft.remindH.coerceIn(0, 23),
                            modifier = Modifier.weight(1f),
                            visibleItems = 5,
                            onSelected = { _, v -> onDraft(draft.copy(remindH = v)) },
                            label = { "%02d".format(it) },
                        )
                        Text(
                            text = ":",
                            color = Letify.colors.text,
                            style = Letify.typography.displayMedium,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        WheelPicker(
                            values = MinuteValues,
                            initialIndex = draft.remindM.coerceIn(0, 59),
                            modifier = Modifier.weight(1f),
                            visibleItems = 5,
                            onSelected = { _, v -> onDraft(draft.copy(remindM = v)) },
                            label = { "%02d".format(it) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun DayRow(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Row(
        Modifier
            .screenHPad()
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val iso = i + 1
            val active = iso in selected
            // Day chip shape: 14dp rounded square. CircleShape on a
            // weight(1f) row produced visibly squashed ovals at phone
            // width (7 cells = ~44dp wide each but 44dp tall), which
            // the user flagged as "приплюснутые". Rounded squares
            // read cleanly at any column width.
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        if (active) Letify.colors.accentSoft else Letify.colors.track,
                        RoundedCornerShape(14.dp),
                    )
                    .noFeedbackClick {
                        onChange(if (active) selected - iso else selected + iso)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) Letify.colors.accent else Letify.colors.text,
                    style = Letify.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun RowScope.DayPreset(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .height(36.dp)
            .background(
                if (active) Letify.colors.accentSoft else Letify.colors.track,
                RoundedCornerShape(999.dp),
            )
            .noFeedbackClick { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Letify.colors.accent else Letify.colors.text,
            style = Letify.typography.labelMedium,
        )
    }
}

@Composable
private fun SwitchPill(on: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .background(
                if (on) Letify.colors.accent else Letify.colors.muted.copy(alpha = 0.35f),
                RoundedCornerShape(999.dp),
            )
            .noFeedbackClick { onToggle(!on) },
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(22.dp)
                .background(Color.White, CircleShape),
        )
    }
}
