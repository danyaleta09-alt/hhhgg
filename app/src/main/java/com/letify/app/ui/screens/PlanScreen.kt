package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.noFeedbackCombinedClick
import com.letify.app.ui.components.peekGesture
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.IconButtonRound
import com.letify.app.ui.components.ProgressRing
import com.letify.app.ui.components.ScreenHorizontalPadding
import com.letify.app.ui.components.SectionTitle
import com.letify.app.ui.components.rememberElasticOverscroll
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Dates
import com.letify.app.ui.state.Habit
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.Tab
import com.letify.app.ui.state.TaskItem
import com.letify.app.ui.state.TaskStatus
import com.letify.app.ui.theme.Letify
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.runtime.movableContentOf
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.round
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.geometry.Rect
import java.time.LocalDate
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.PI
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.rememberCoroutineScope
import com.letify.app.ui.components.noFeedbackClick
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import java.time.LocalTime

// -------- Plan screen layout constants ----------------------------------------

private val TitleHeight      = 56.dp  // pinned «план» header row height
private val ScrollTopPadding = 6.dp   // space above the title

// -------- Collapsing-strip tuning (restored from the original «kruzhki» design)
private val RingNatural    = 64.dp    // vessel diameter when fully expanded
private val RingTarget      = 32.dp   // vessel diameter docked in the title row
private val TargetSpacing   = 19.dp   // center-to-center in the collapsed cluster
private const val MiniRingCount = 3   // only the first N vessels dock into the cluster
private val StripCellWidth  = 72.dp   // fixed cell width → strip scrolls sideways
private val StripCellGap    = 4.dp
private val SectionTitleHeight = 40.dp // reserved height for the «Привычки» row
private val StripHeight      = 110.dp  // habit strip height (vessel + label)
// Scroll distance over which the collapse animation completes.
private val CollapseScrollDistance = SectionTitleHeight + StripHeight + 10.dp

// Once the user lets go, the strip must ALWAYS settle either fully expanded or
// fully collapsed — it can never rest half-way. The rest state is decided by
// POSITION (how far it has travelled), not by how far the finger happened to
// drag: once past ~30 % of the travel the collapse finishes on its own; below
// that it springs back open. A real flick overrides the position rule in its
// own direction. This is what makes the circles "snap from position" instead
// of staying stuck wherever the scroll stopped.
private const val CollapseCommitFraction = 0.30f
private const val CollapseFlickVelocity = 120f // px/s — above this it's a flick
// Settle stiffness for the collapse/dock snap. Between StiffnessLow (50) and
// StiffnessMediumLow (400): smooth, unhurried glide to rest, no abrupt snap.
private const val CollapseSnapStiffness = 220f
private fun collapseSnapTarget(value: Float, velocity: Float, collapsePx: Float): Float = when {
    velocity > CollapseFlickVelocity -> collapsePx
    velocity < -CollapseFlickVelocity -> 0f
    value > collapsePx * CollapseCommitFraction -> collapsePx
    else -> 0f
}

// ── Long-press / multi-select plumbing ─────────────────────────────────────
// Threaded to TaskCard / SubtaskTaskCard / HabitCell via a CompositionLocal so
// the movableContent task cards don't need new parameters (which would break
// their identity and the flight animation). null = feature inactive.
private enum class PeekKind { Task, Habit }
private data class PeekTarget(val kind: PeekKind, val id: Int)

private class PlanInteraction(
    val selectionMode: Boolean,
    val selectedIds: Set<Int>,
    // The item currently "peeked" (long-pressed). Its real card/vessel is
    // hidden in the (blurred) content layer so the sharp replica in the overlay
    // is the ONLY visible copy — no duplication, no blur halo around it.
    val peekTarget: PeekTarget?,
    val onToggleSelect: (Int) -> Unit,
    // Long-press "peek": start opens the menu and records the finger position
    // (window coords); move feeds the live finger position so the overlay can
    // highlight the menu row under the finger; end lifts the finger (fires the
    // highlighted row, or keeps the menu open if none).
    val onTaskPeekStart: (Int, Offset, Boolean) -> Unit,
    val onHabitPeekStart: (Int, Offset) -> Unit,
    val onPeekMove: (Offset) -> Unit,
    val onPeekEnd: () -> Unit,
    val onTaskBounds: (Int, Rect) -> Unit,
    val onHabitBounds: (Int, Rect) -> Unit,
)

private val LocalPlanInteraction = compositionLocalOf<PlanInteraction?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanScreen(
    onBack: () -> Unit = {},
    onAddHabit: () -> Unit = {},
    onAddTask: () -> Unit = {},
    onEditHabit: (Int) -> Unit = {},
    onEditTask: (Int) -> Unit = {},
) {
    val state = LocalAppState.current
    val density = LocalDensity.current
    // Horizontal strip scroll state. A PLAIN horizontal ScrollState (NOT a
    // LazyRow). A LazyRow recycled/disposed the off-screen vessels when the
    // strip was scrolled sideways — so during the collapse the docking circles
    // were simply not composed → missing from the cluster → they "vanished",
    // then snapped back only when the strip silently reset to item 0 at full
    // dock. A plain Row keeps EVERY vessel composed at all times, so the morph
    // into the cluster always has all of them. (Reorder glide is preserved
    // per-cell via a small slot-offset animation.)
    val stripState = rememberScrollState()

    // Whether the «план» header create popover (Привычка/Задача) is open.

    // Shared iOS-style elastic translation overscroll for the schedule list.
    val elastic = rememberElasticOverscroll()
    val verticalOverscroll = elastic.verticalOverscroll

    // Tick once a minute so live/upcoming/past partitioning stays current.
    val dateKey = Dates.todayKey()
    var nowMin by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay() / 60) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = LocalTime.now().toSecondOfDay() / 60
        }
    }
    val sortedTasks = state.tasksToday().sortedBy { it.startMinutes }
    val pastTasks = sortedTasks.filter { it.statusAt(nowMin, dateKey) == TaskStatus.Done }
    val liveTasks = sortedTasks.filter { it.statusAt(nowMin, dateKey) == TaskStatus.Live }
    val upcomingTasks = sortedTasks.filter { it.statusAt(nowMin, dateKey) == TaskStatus.Upcoming }

    // ===== Long-press peek + multi-select state =============================
    // peek: the item whose context menu (blurred background) is currently open.
    // selectionMode: multi-select for tasks (entered from the task «Выбрать»).
    var peek by remember { mutableStateOf<PeekTarget?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }
    // Live finger position (window coords) during a peek drag, plus a counter
    // bumped on lift — the overlay uses these for drag-to-select.
    var peekPointer by remember { mutableStateOf<Offset?>(null) }
    var peekRelease by remember { mutableIntStateOf(0) }
    // Whether the long-pressed task card was expanded (subtask checklist open),
    // so the peek replica can match its on-screen height (#n9215).
    var peekTaskExpanded by remember { mutableStateOf(false) }
    // Window-space bounds of every visible card / habit vessel, reported via
    // onGloballyPositioned, so the peek replica + menu and drag-paint selection
    // can be positioned/hit-tested precisely.
    val taskBounds = remember { mutableStateMapOf<Int, Rect>() }
    val habitBounds = remember { mutableStateMapOf<Int, Rect>() }

    fun exitSelection() { selectionMode = false; selectedIds.clear() }

    // Peek no longer blurs the content at all — instead PeekOverlay lays down a
    // gentle theme-coloured dim. `peekProgress` drives the perceived transition
    // (scrim alpha, menu + replica scale). `peekActive` is kept so the overlay
    // and the navbar can react to the peek being open. This block is defined
    // BEFORE `interaction` so the "hide the real item" target can stay set
    // through the whole fade-out.
    val peekProgress = remember { Animatable(0f) }
    LaunchedEffect(peek != null) {
        if (peek != null) {
            peekProgress.animateTo(1f, tween(190, easing = FastOutSlowInEasing))
        } else {
            peekProgress.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
        }
    }
    val blurActive = peek != null || peekProgress.value > 0.001f
    // Blur removed per design — keep a 0.dp placeholder so the layout modifiers
    // below stay structurally identical (no GPU blur pass during peek).
    val peekBlurDp = 0.dp
    // Hoist to the shell so the bottom navbar dims in lockstep with the peek.
    LaunchedEffect(blurActive) { state.peekActive = blurActive }
    // Publish the EXACT peek-transition value to the shell every frame so the
    // navbar's dim rides the same curve as the background scrim — fixes the
    // navbar darkening on its own separate (lagging) animation, most visibly
    // when the menu closes.
    LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { peekProgress.value }
            .collect { state.peekDim = it }
    }
    // Retain the target during the fade-out so the overlay can finish animating.
    var displayPeek by remember { mutableStateOf<PeekTarget?>(null) }
    LaunchedEffect(peek) { if (peek != null) displayPeek = peek }

    val interaction = PlanInteraction(
        selectionMode = selectionMode,
        selectedIds = selectedIds.toSet(),
        // Keep the real item hidden for the WHOLE peek lifetime — including the
        // fade-out (`blurActive`), not just while `peek != null`. Otherwise the
        // real card/vessel snapped back to full brightness the instant the menu
        // dismissed, overlapping the still-fading replica → the habit circle
        // "flashed"/glowed for a frame. Now it reappears only once the overlay
        // is fully gone, so the hand-off is seamless.
        peekTarget = if (blurActive) (peek ?: displayPeek) else null,
        onToggleSelect = { id -> if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id) },
        onTaskPeekStart = { id, p, exp -> peekRelease = 0; peekPointer = p; peekTaskExpanded = exp; peek = PeekTarget(PeekKind.Task, id) },
        onHabitPeekStart = { id, p -> peekRelease = 0; peekPointer = p; peek = PeekTarget(PeekKind.Habit, id) },
        onPeekMove = { p -> peekPointer = p },
        onPeekEnd = { peekRelease++ },
        onTaskBounds = { id, r -> taskBounds[id] = r },
        onHabitBounds = { id, r -> habitBounds[id] = r },
    )

    // ===== Habits: original collapsing-vessel strip (restored) ==============
    // As you scroll, the SAME vessels physically slide up + shrink into a tight
    // cluster docked next to the pinned «план» title (no overlay, no new
    // circles). Driven by scroll progress with a snap-to-rest fling so it
    // always lands fully expanded or fully collapsed.
    val habitsList = state.habits
    val doneCount = habitsList.count { it.isDoneOn(dateKey) }

    // A habit only dims + glides to the END of the strip AFTER its water has
    // finished filling to the top — not the instant it's marked done. Each cell
    // reports when its fill animation visually tops out via this map; the strip
    // is ordered by it (stable sort: not-yet-filled first, filled last) so the
    // vessel stays in place while the liquid rises, then glides away.
    val filledDone = remember { mutableStateMapOf<Int, Boolean>() }
    val orderedHabits = habitsList.sortedBy { filledDone[it.id] == true }

    // Device lean → water surface stays level to gravity + gentle slosh.
    val tiltProvider = rememberDeviceTilt()

    val scrollState = rememberScrollState()

    val collapsePx = with(density) { CollapseScrollDistance.toPx() }
    // derivedStateOf so the morph chain only recomposes when the clamped
    // progress actually changes, not on every scrolled pixel.
    val collapseProgressState = remember(scrollState, collapsePx) {
        androidx.compose.runtime.derivedStateOf {
            (scrollState.value / collapsePx).coerceIn(0f, 1f)
        }
    }

    // Frosted navbar drops its real-time blur while scrolling.
    LaunchedEffect(scrollState) {
        androidx.compose.runtime.snapshotFlow { scrollState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { state.contentScrolling = it }
    }
    DisposableEffect(Unit) { onDispose { state.contentScrolling = false } }

    // On Plan tab entry: hard-reset both scrolls so the cluster always starts
    // from the fully-expanded position.
    LaunchedEffect(Unit) { scrollState.scrollTo(0); stripState.scrollTo(0) }

    // When the Plan tab is LEFT (navbar switch), immediately settle the habit
    // strip: kill any in-flight horizontal/vertical overscroll AND snap the
    // strip back to the start, so nothing is mid-spring during the slide.
    LaunchedEffect(state.currentTab) {
        if (state.currentTab != Tab.Plan) {
            elastic.horizontalOverscroll.floatValue = 0f
            verticalOverscroll.floatValue = 0f
            stripState.scrollTo(0)
        }
    }

    // The habit strip is the ONLY full-width Plan element whose circles can sit
    // half-clipped right at the screen edge (everything else is inset by the
    // screen padding). During a «Сдвиг» tab switch the whole Plan screen slides
    // off, so that half-clipped circle/label visibly sweeps across — it read as
    // "text shooting past the border" over the incoming tab. Fix: fade the strip
    // out almost instantly the moment Plan stops being the current tab, so its
    // edge circles never appear during the slide. The rest of Plan still slides
    // normally (shared-axis preserved). Fades back in when Plan returns.
    val stripTabAlpha = animateFloatAsState(
        targetValue = if (state.currentTab == Tab.Plan) 1f else 0f,
        animationSpec = tween(durationMillis = 70, easing = LinearEasing),
        label = "stripTabAlpha",
    )

    // Once fully docked the strip's horizontal scroll is invisible — reset it
    // silently so re-expanding starts from the beginning.
    LaunchedEffect(scrollState, collapsePx) {
        androidx.compose.runtime.snapshotFlow { scrollState.value >= collapsePx - 0.5f }
            .collect { docked ->
                if (docked && stripState.value != 0) {
                    stripState.scrollTo(0)
                }
            }
    }

    // Safety net: if the screen settles inside the collapse zone with no fling
    // (finger lifted nearly still), snap to the nearest rest position.
    LaunchedEffect(scrollState, collapsePx) {
        androidx.compose.runtime.snapshotFlow { scrollState.isScrollInProgress to scrollState.value }
            .collect { (inProgress, value) ->
                if (!inProgress) {
                    val v = value.toFloat()
                    if (v > 0.5f && v < collapsePx - 0.5f) {
                        scrollState.animateScrollTo(
                            collapseSnapTarget(v, 0f, collapsePx).toInt(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                // Gentler than StiffnessMedium so the dock settle glides
                                // smoothly into place instead of snapping abruptly.
                                stiffness = CollapseSnapStiffness,
                            ),
                        )
                    }
                }
            }
    }

    // Snap-to-rest fling. Runs INSIDE the scroll mutex (custom FlingBehavior),
    // so the snap animation is never preempted by the inner scrollable's own
    // fling. Delegates to the default decay fling outside the collapse zone.
    val defaultFling = ScrollableDefaults.flingBehavior()
    val snapFling = remember(scrollState, collapsePx, defaultFling) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val value = scrollState.value.toFloat()
                if (value <= 0.5f || value >= collapsePx - 0.5f) {
                    with(defaultFling) { return performFling(initialVelocity) }
                }
                val target = collapseSnapTarget(value, initialVelocity, collapsePx)
                var prev = value
                animate(
                    initialValue = value,
                    targetValue = target,
                    initialVelocity = initialVelocity,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        // Gentler than StiffnessMedium so the dock settle glides
                        // smoothly into place instead of snapping abruptly.
                        stiffness = CollapseSnapStiffness,
                    ),
                ) { latest, _ ->
                    scrollBy(latest - prev)
                    prev = latest
                }
                return 0f
            }
        }
    }

    // Kills the system stretch overscroll on every scroll container here.
    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null,
        LocalPlanInteraction provides interaction,
    ) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .nestedScroll(elastic.connection),
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val statusBarPx = WindowInsets.statusBars.getTop(density)
        val statusBarDp = with(density) { statusBarPx.toDp() }

        // Pinned title geometry.
        val titleTopPx = statusBarPx + with(density) { ScrollTopPadding.toPx() }
        val titleHeightPx = with(density) { TitleHeight.toPx() }
        val titleCenterY = titleTopPx + titleHeightPx / 2f

        // Strip vessel center when scroll == 0 (constant per layout).
        val stripTopAtZeroScrollPx =
            titleTopPx + titleHeightPx + with(density) { SectionTitleHeight.toPx() }
        val stripRingCenterAtZeroScroll =
            stripTopAtZeroScrollPx + with(density) { RingNatural.toPx() } / 2f

        // Per-vessel horizontal centers, expanded layout (fixed-width cells).
        val hPadPx = with(density) { ScreenHorizontalPadding.toPx() }
        val cellWidthPx = with(density) { StripCellWidth.toPx() }
        val cellGapPx = with(density) { StripCellGap.toPx() }

        // Collapsed mini-cluster: docks against the right edge at the title's Y
        //     [        план        ][ ○ ○ ○ ]
        // Slot 0 = left-most (same L→R order as the strip → no crossing).
        val targetSpacingPx = with(density) { TargetSpacing.toPx() }
        val visibleMiniCount = minOf(MiniRingCount, habitsList.size)
        val ringTargetPx = with(density) { RingTarget.toPx() }
        val rightEdgePadPx = with(density) { 18.dp.toPx() }
        val clusterStartX = screenWidthPx - rightEdgePadPx -
            ringTargetPx / 2f - (visibleMiniCount - 1).coerceAtLeast(0) * targetSpacingPx
        val ringTargetScale = RingTarget.value / RingNatural.value

        // Floor the column height so there's always enough scroll range to
        // fully collapse, even with a short schedule list.
        val viewportPx = with(density) { maxHeight.toPx() }.toInt()
        val verticalChromePx = with(density) {
            (ScrollTopPadding + TitleHeight + 140.dp).toPx()
        }.toInt() + statusBarPx
        val collapsePxInt = collapsePx.toInt()
        val minColumnHeightPx = (viewportPx + collapsePxInt - verticalChromePx).coerceAtLeast(0)

        // ------------- LAYER 1 : scrolling content ----------------------------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(peekBlurDp)
                .graphicsLayer { translationY = verticalOverscroll.floatValue }
                .verticalScroll(scrollState, flingBehavior = snapFling)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = ScrollTopPadding + TitleHeight, bottom = 140.dp)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val h = maxOf(placeable.height, minColumnHeightPx)
                    layout(placeable.width, h) { placeable.place(0, 0) }
                },
        ) {
            run {
                // «Привычки» row — fixed height so the strip Y math is exact.
                // Fades out as the cluster collapses. Always shown (even with an
                // empty habit list) so the trailing «+» create-cell is reachable
                // — otherwise, now that there's no demo seed, a user with zero
                // habits would have no way to add one.
                Box(
                    modifier = Modifier
                        .height(SectionTitleHeight)
                        .graphicsLayer {
                            alpha = (1f - collapseProgressState.value * 1.8f).coerceIn(0f, 1f)
                        },
                ) {
                    SectionTitle(
                        "Привычки",
                        topPadding = 8.dp,
                        trailing = {
                            if (habitsList.isNotEmpty()) {
                                Text(
                                    text = "сегодня $doneCount / ${habitsList.size}",
                                    color = Letify.colors.accent,
                                    style = Letify.typography.labelMedium,
                                )
                            }
                        },
                    )
                }

                HabitStrip(
                    habits = orderedHabits,
                    listState = stripState,
                    tabAlpha = { stripTabAlpha.value },
                    onFilledChange = { id, filled -> filledDone[id] = filled },
                    tilt = tiltProvider,
                    dateKey = dateKey,
                    collapseProgress = { collapseProgressState.value },
                    stripLiftPx = {
                        // translationY = vScroll − yTravel·t − overscrollY·t
                        //   • +vScroll      cancels the column's own scroll once
                        //                   collapsed (cluster stays pinned).
                        //   • −yTravel·t    pulls the vessel from its natural Y
                        //                   down to the title row linearly with t.
                        //   • −overscrollY·t cancels the column elastic overscroll
                        //                   proportionally to t (pinned at t=1).
                        val t = collapseProgressState.value
                        val yTravelPx = stripRingCenterAtZeroScroll - titleCenterY
                        scrollState.value.toFloat() - yTravelPx * t -
                            verticalOverscroll.floatValue * t
                    },
                    hScrollOffset = {
                        // Pixels the strip has scrolled horizontally (stable Int).
                        stripState.value.toFloat()
                    },
                    hPadPx = hPadPx,
                    cellWidthPx = cellWidthPx,
                    cellGapPx = cellGapPx,
                    clusterStartX = clusterStartX,
                    targetSpacingPx = targetSpacingPx,
                    ringTargetScale = ringTargetScale,
                    visibleMiniCount = visibleMiniCount,
                    horizontalOverscrollPx = {
                        elastic.horizontalOverscroll.floatValue * (1f - collapseProgressState.value)
                    },
                    onAddHabit = onAddHabit,
                )
            }

            SectionTitle(
                "Расписание на сегодня",
                trailing = { CreateTaskButton(onClick = onAddTask) },
            )

            if (sortedTasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, bottom = 32.dp)
                        .screenHPad(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "На сегодня пусто",
                        color = Letify.colors.text,
                        style = Letify.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Нажмите + Создать, чтобы добавить первую задачу",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // ── Schedule: ONE shared Column for active cards + «Прошедшие» ──
            // Every card and the section header are siblings of the same
            // Column, and each card is movableContent. When a task completes,
            // the SAME card node is re-slotted into the past group and
            // animatePlacement() glides it there physically — it never
            // disappears/reappears — while neighbours smoothly close the gap.
            // Tasks currently playing their completion choreography. The
            // «Прошедшие» header watches this so it can slide in EARLY, during
            // the dim phase — by the time the card takes off, the layout below
            // is already settled and the flight is one clean motion (no
            // mid-air retargeting while the header is still expanding).
            val completingTasks = remember { mutableStateMapOf<Int, Boolean>() }
            val flyingCards = remember { mutableMapOf<Int, @Composable (TaskItem) -> Unit>() }
            fun flyingCard(task: TaskItem): @Composable (TaskItem) -> Unit =
                flyingCards.getOrPut(task.id) {
                    movableContentOf { t: TaskItem ->
                        Box(Modifier.screenHPad().animatePlacement()) {
                            TaskCard(t, nowMin, dateKey, completingTasks)
                        }
                    }
                }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (liveTasks + upcomingTasks).forEach { key(it.id) { flyingCard(it)(it) } }
                AnimatedVisibility(
                    visible = pastTasks.isNotEmpty() || completingTasks.isNotEmpty(),
                    enter = fadeIn(tween(360, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(360, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(280)) + shrinkVertically(tween(280)),
                ) {
                    SectionTitle("Прошедшие", topPadding = 2.dp)
                }
                pastTasks.forEach { key(it.id) { flyingCard(it)(it) } }
            }
        }

        // ------------- LAYER 3 : pinned title + create button -----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .blur(peekBlurDp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = ScrollTopPadding)
                .height(TitleHeight)
                .zIndex(3f)
                .screenHPad(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "план",
                color = Letify.colors.text,
                style = Letify.typography.displayMedium,
            )
            // Back to home (navbar removed)
            NoFeedbackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text("←", color = Letify.colors.text, style = Letify.typography.titleLarge)
            }
        }

        // ------------- LAYER 4 : long-press context menu (peek) ---------------
        // Rendered from the LIVE `peek` the instant it is set, falling back to
        // `displayPeek` only during the fade-out. Using `displayPeek` alone lagged
        // one frame behind (it's assigned from a LaunchedEffect), so on press the
        // real item was already hidden while the replica had not yet appeared —
        // a 1-frame gap that read as a sharp blink/flicker. `peek ?: displayPeek`
        // closes that gap: replica appears the same frame the real item hides.
        if (blurActive) (peek ?: displayPeek)?.let { target ->
            when (target.kind) {
                PeekKind.Task -> {
                    val t = sortedTasks.firstOrNull { it.id == target.id }
                    val rect = taskBounds[target.id]
                    if (t != null && rect != null) {
                        PeekOverlay(
                            rect = rect,
                            screenHeightPx = viewportPx.toFloat(),
                            screenWidthPx = screenWidthPx,
                            progress = { peekProgress.value },
                            pointer = { peekPointer },
                            releaseSignal = peekRelease,
                            items = listOf(
                                PeekMenuItem("Выбрать", "check-circle-bold") {
                                    peek = null
                                    selectionMode = true
                                    if (target.id !in selectedIds) selectedIds.add(target.id)
                                },
                                PeekMenuItem("Изменить", "pen-bold") {
                                    peek = null
                                    onEditTask(target.id)
                                },
                            ),
                            onDismiss = { peek = null },
                        ) {
                            PeekTaskCard(t, nowMin, dateKey, expanded = peekTaskExpanded)
                        }
                    }
                }
                PeekKind.Habit -> {
                    val h = habitsList.firstOrNull { it.id == target.id }
                    val rect = habitBounds[target.id]
                    if (h != null && rect != null) {
                        PeekOverlay(
                            rect = rect,
                            screenHeightPx = viewportPx.toFloat(),
                            screenWidthPx = screenWidthPx,
                            progress = { peekProgress.value },
                            pointer = { peekPointer },
                            releaseSignal = peekRelease,
                            items = listOf(
                                PeekMenuItem("Сбросить", "restart-bold") {
                                    peek = null
                                    state.resetHabitProgress(target.id)
                                },
                                PeekMenuItem("Изменить", "pen-bold") {
                                    peek = null
                                    onEditHabit(target.id)
                                },
                                PeekMenuItem("Удалить", "trash-bin-trash-bold", destructive = true) {
                                    peek = null
                                    state.deleteHabit(target.id)
                                },
                            ),
                            onDismiss = { peek = null },
                        ) {
                            PeekHabitVessel(h, dateKey)
                        }
                    }
                }
            }
        }

        // ------------- LAYER 5 : multi-select top bar ------------
        // The former full-screen gesture layer was removed (#n4552): it sat
        // above the schedule list at zIndex 8 and swallowed vertical drags, so
        // the list could not be scrolled while selecting. Selection is now
        // driven by tapping the cards themselves (see TaskCard /
        // SubtaskTaskCard `onTap`), which leaves vertical scrolling to the
        // list underneath.

        // Top bar animates in/out smoothly (slide down + fade), so entering and
        // leaving selection mode is a soft transition rather than a hard cut.
        // Kept OUTSIDE the `if` so the exit animation can play out.
        var lastSelCount by remember { mutableIntStateOf(0) }
        if (selectionMode) lastSelCount = selectedIds.size
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(220)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it },
            modifier = Modifier.zIndex(9f),
        ) {
            SelectionTopBar(
                count = if (selectionMode) selectedIds.size else lastSelCount,
                statusBarDp = statusBarDp,
                onSelectAll = {
                    sortedTasks.forEach { if (it.id !in selectedIds) selectedIds.add(it.id) }
                },
                onDelete = {
                    state.deleteTasks(selectedIds.toList())
                    exitSelection()
                },
                onCancel = { exitSelection() },
            )
        }
    }
    }
}

// ---------- Habit strip: real vessels that collapse into the title cluster -----

@Composable
private fun HabitStrip(
    habits: List<Habit>,
    listState: androidx.compose.foundation.ScrollState,
    tabAlpha: () -> Float,
    onFilledChange: (Int, Boolean) -> Unit,
    tilt: () -> Float,
    dateKey: String,
    collapseProgress: () -> Float,
    stripLiftPx: () -> Float,
    hScrollOffset: () -> Float,
    hPadPx: Float,
    cellWidthPx: Float,
    cellGapPx: Float,
    clusterStartX: Float,
    targetSpacingPx: Float,
    ringTargetScale: Float,
    visibleMiniCount: Int,
    horizontalOverscrollPx: () -> Float,
    onAddHabit: () -> Unit,
) {
    Row(
        modifier = Modifier
            // zIndex above later Column siblings so the docking cluster stays
            // ABOVE the schedule cards as the strip slides up into the title.
            .zIndex(1f)
            .graphicsLayer {
                translationY = stripLiftPx()
                translationX = horizontalOverscrollPx()
                // Fade out instantly when leaving the Plan tab so the strip's
                // edge circles don't sweep over the incoming tab during a slide.
                alpha = tabAlpha()
            }
            .fillMaxWidth()
            .height(StripHeight)
            // Plain horizontal scroll → EVERY vessel stays composed, so the
            // collapse morph into the cluster always has all of them (no
            // LazyRow recycling = no "vanishing circles" during the collapse).
            .horizontalScroll(listState)
            // padding + spacedBy so each cell's center stays at
            //   hPad + i*(cellW + gap) + cellW/2  → matches the X-morph math.
            .padding(horizontal = ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(StripCellGap),
        verticalAlignment = Alignment.Top,
    ) {
        // Stable key + a per-cell slot-offset animation (inside HabitCell) →
        // when a habit is completed and reorders to the end it GLIDES there
        // smoothly, and the others slide to fill its old place. Done with a
        // relative offset (not animateItem / animatePlacement) so it never
        // fights the horizontal scroll.
        habits.forEachIndexed { idx, h ->
            key(h.id) {
                HabitCell(
                    habit = h,
                    index = idx,
                    tilt = tilt,
                    dateKey = dateKey,
                    onFilledChange = onFilledChange,
                    collapseProgress = collapseProgress,
                    hScrollOffset = hScrollOffset,
                    hPadPx = hPadPx,
                    cellWidthPx = cellWidthPx,
                    cellGapPx = cellGapPx,
                    clusterStartX = clusterStartX,
                    targetSpacingPx = targetSpacingPx,
                    ringTargetScale = ringTargetScale,
                    visibleMiniCount = visibleMiniCount,
                )
            }
        }
        // Trailing "create habit" slot — always the LAST circle in the strip:
        // a dashed-border ring (same size as the vessels) with a "+" inside.
        // Tapping it starts habit creation. It fades out as the strip collapses
        // (it's not part of the docking mini-cluster).
        AddHabitCell(
            collapseProgress = collapseProgress,
            onClick = onAddHabit,
        )
    }
}

// Trailing "+" slot in the habit strip — a dashed-border ring (same size as a
// vessel) with a "+" inside and a «Создать» label below, so it has the EXACT
// same structure/height as a HabitCell (circle + 10dp spacer + 34dp label).
// Matching the height matters: an unequal-height last item makes the LazyRow
// re-measure during the collapse lift, which read as circles "flickering".
// Ring + plus are muted grey (subtle, not garish). The whole cell fades out as
// the cluster collapses (it's not part of the docking mini-cluster).
@Composable
private fun AddHabitCell(
    collapseProgress: () -> Float,
    onClick: () -> Unit,
) {
    // Same subtle disc the empty LiquidVessel draws (text @ 5% alpha), so the
    // "+" cell sits on the same background as the other circles. The dashed ring
    // and "+" are deliberately faint (muted @ low alpha) — barely-there hint.
    val ground = Letify.colors.text
    val hintColor = Letify.colors.muted.copy(alpha = 0.45f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(StripCellWidth),
    ) {
        Box(
            modifier = Modifier
                .size(RingNatural)
                .graphicsLayer {
                    alpha = (1f - collapseProgress() * 1.8f).coerceIn(0f, 1f)
                }
                .drawBehind {
                    val r = size.minDimension / 2f
                    // filled background disc — identical to the other circles
                    drawCircle(color = ground.copy(alpha = 0.05f), radius = r)
                    val w = 2.dp.toPx()
                    drawCircle(
                        color = hintColor,
                        radius = r - w / 2f,
                        style = Stroke(
                            width = w,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(9f, 8f), 0f,
                            ),
                        ),
                    )
                }
                .clip(CircleShape)
                .noFeedbackClick(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = "add-bold", tint = hintColor, size = 26.dp)
        }

        Spacer(Modifier.height(10.dp))

        // «Создать» label, styled like a habit name and faded with the collapse.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .graphicsLayer {
                    alpha = (1f - collapseProgress() * 2.4f).coerceIn(0f, 1f)
                },
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "Создать",
                color = Letify.colors.text,
                style = Letify.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// "+ Создать" inline action shown next to the «Расписание на сегодня» heading.
@Composable
private fun CreateTaskButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .noFeedbackClick(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SolarIcon(name = "add-bold", tint = Letify.colors.accent, size = 16.dp)
        Text(
            text = "Создать",
            color = Letify.colors.accent,
            style = Letify.typography.labelMedium,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitCell(
    habit: Habit,
    index: Int,
    tilt: () -> Float,
    dateKey: String,
    onFilledChange: (Int, Boolean) -> Unit,
    collapseProgress: () -> Float,
    hScrollOffset: () -> Float,
    hPadPx: Float,
    cellWidthPx: Float,
    cellGapPx: Float,
    clusterStartX: Float,
    targetSpacingPx: Float,
    ringTargetScale: Float,
    visibleMiniCount: Int,
    modifier: Modifier = Modifier,
) {
    val state = LocalAppState.current
    val interaction = LocalPlanInteraction.current
    val isPeek = interaction?.peekTarget?.let { it.kind == PeekKind.Habit && it.id == habit.id } == true
    val bgColor = Letify.colors.bg
    val count = habit.progressOn(dateKey)
    val target = habit.effectiveTarget
    val progress = count.toFloat() / target
    val done = habit.isDoneOn(dateKey)
    // Water level (springs up smoothly when progress changes).
    val fill by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
        label = "fill",
    )
    // "Visually done" = marked done AND the liquid has actually risen to the
    // top. Only THEN do we dim + report so the parent reorders it to the end —
    // completing a habit first fills the glass, then it fades/glides away.
    val filled = done && fill >= 0.985f
    LaunchedEffect(filled) { onFilledChange(habit.id, filled) }
    val dim by animateFloatAsState(
        targetValue = if (filled) 0.55f else 1f,
        animationSpec = tween(320),
        label = "habitDim",
    )
    // Reorder glide: a completed habit jumps to the END of the list, shifting
    // everyone. We animate a "visual index" toward the real one and offset the
    // cell by (visual − real) slots (cells are fixed width, one slot =
    // cellWidth + gap). Crucially the visual index still holds the OLD slot on
    // the very first frame after a reorder, so the cell is drawn back at its old
    // place and glides from there — it NEVER flashes for a frame at its new slot
    // first. (That 1-frame flash at the destination was the "circle appears,
    // resharply vanishes, then flies over" glitch from r113.)
    val stepPx = cellWidthPx + cellGapPx
    val visualIndex = remember { Animatable(index.toFloat()) }
    LaunchedEffect(index) {
        // Smooth, unhurried glide (slow-in/slow-out) rather than a springy snap,
        // so the vessel "flows" to its new slot instead of darting there.
        visualIndex.animateTo(
            index.toFloat(),
            animationSpec = tween(
                durationMillis = 560,
                easing = FastOutSlowInEasing,
            ),
        )
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset { IntOffset(((visualIndex.value - index) * stepPx).roundToInt(), 0) }
            .width(StripCellWidth),
    ) {
        // The one and only vessel per habit. As the user scrolls vertically the
        // parent strip lifts (carrying vessel + label), and each vessel
        // independently morphs in X + scale into its mini-cluster slot via this
        // draw-phase layer — same element, no overlay, no crossfade.
        Box(
            modifier = Modifier
                .size(RingNatural)
                .graphicsLayer {
                    val t = collapseProgress()
                    // Short-circuit when fully expanded — by NOT reading
                    // hScrollOffset() here we don't subscribe to it, so dragging
                    // the strip sideways doesn't re-invalidate every vessel layer.
                    if (t <= 0f) {
                        translationX = 0f
                        scaleX = 1f
                        scaleY = 1f
                        // The completion-dim is applied to the VESSEL only (below),
                        // never to this whole layer, so the stories separation
                        // ring/bg-backing stay fully opaque even when the circle dims.
                        alpha = 1f
                        return@graphicsLayer
                    }
                    val slot = index.coerceAtMost(visibleMiniCount - 1)
                    val layoutCenterX =
                        hPadPx + cellWidthPx / 2f + index * (cellWidthPx + cellGapPx)
                    val targetCenterX = clusterStartX + slot * targetSpacingPx
                    translationX = (targetCenterX - layoutCenterX + hScrollOffset()) * t
                    val scale = 1f + (ringTargetScale - 1f) * t
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin.Center
                    // Vessels beyond the mini-count fade out as they collapse.
                    // No `dim` here — dimming is applied to the vessel content only
                    // so the stories ring/separation never fades when a habit is done.
                    alpha = if (index < visibleMiniCount) {
                        1f
                    } else {
                        (1f - t * 1.4f).coerceIn(0f, 1f)
                    }
                }
                // Instagram stories-style bg-color border — taken 1:1 from the
                // reference file. A bg-colour circle slightly LARGER than the
                // vessel, drawn BEHIND it inside the same collapse transform so
                // it scales/translates with the ring. Invisible at full
                // expansion (rings don't overlap); fades in as the rings start
                // to overlap during collapse and creates the stories-like
                // separation stripe in the app bg colour.
                .drawBehind {
                    val t = collapseProgress()
                    if (t > 0.15f && index < visibleMiniCount) {
                        val borderAlpha = ((t - 0.15f) / 0.35f).coerceIn(0f, 1f)
                        // 5dp here scales by ringTargetScale (~0.5) to a ~2.5dp
                        // visible separation when docked — matches the reference
                        // stories ring (was 3dp → looked too thin/cut).
                        val borderPx = 5.dp.toPx()
                        drawCircle(
                            color = bgColor.copy(alpha = borderAlpha),
                            radius = size.width / 2f + borderPx,
                            center = Offset(size.width / 2f, size.height / 2f),
                        )
                    }
                }
                .onGloballyPositioned { interaction?.onHabitBounds(habit.id, it.boundsInWindow()) }
                .clip(CircleShape)
                // Tap = +1 progress; long-press = открыть меню (Сбросить /
                // Изменить / Удалить) с размытием фона + drag-to-select.
                .peekGesture(
                    onTap = { state.tapHabit(habit.id) },
                    onPeekStart = { p -> interaction?.onHabitPeekStart(habit.id, p) },
                    onPeekMove = { p -> interaction?.onPeekMove(p) },
                    onPeekEnd = { interaction?.onPeekEnd() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Opaque app-bg backing behind the vessel, fading in ONLY as it
            // docks. The vessel keeps its full liquid look (fill + icon) — the
            // docked circle stays "the same, just collapsed" — but its otherwise
            // translucent empty area becomes opaque app-bg. Against the top bar
            // that's invisible; where circles overlap it occludes the neighbour
            // so the bg-colour story-ring reads as a clean separation instead of
            // a see-through "обрезанный прозрачный" overlap. Draw-phase read of
            // collapseProgress() → no recomposition.
            if (index < visibleMiniCount) {
                Canvas(Modifier.size(RingNatural)) {
                    val t = collapseProgress()
                    if (t > 0f) drawCircle(color = bgColor.copy(alpha = t))
                }
            }
            // The completion-dim lives HERE — on the vessel (fill + icon) only.
            // The opaque bg-backing disc above and the stories separation ring
            // (drawBehind) stay at full opacity, so a completed/dimmed circle
            // keeps its crisp stories ободок instead of letting it fade away.
            Box(Modifier.graphicsLayer { alpha = if (isPeek) 0f else dim }) {
                LiquidVessel(
                    fill = fill,
                    color = habit.color,
                    icon = habit.icon,
                    size = RingNatural,
                    tiltProvider = tilt,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Label block fades out over the first ~42% of the collapse, AND dims
        // with the same `dim` factor as the vessel when the habit is completed
        // (so the text dims together with its circle).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .graphicsLayer {
                    alpha = dim * (1f - collapseProgress() * 2.4f).coerceIn(0f, 1f)
                },
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = habit.name,
                color = Letify.colors.text,
                style = Letify.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (habit.unit.isEmpty()) "$count/$target" else "$count/$target ${habit.unit}",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


// ---------- «план» create popover (Привычка / Задача) ---------------
//
// A small floating menu anchored under the "+" in the pinned header. It
// replaces the old per-section "+" buttons: one place to create anything on
// the Plan screen.
//
// Rendered as a FULL-SCREEN in-composition overlay (not a platform Popup).
// A transparent scrim catches outside taps to dismiss; the menu column is
// positioned under the header "+" with a top-left scale/fade animation. No
// window allocation → the menu animates in from the first frame (fixes the
// "меню появляется с лагами" jank on open).
@Composable
private fun PlanCreateMenuOverlay(
    visible: Boolean,
    statusBarDp: Dp,
    onDismiss: () -> Unit,
    onHabit: () -> Unit,
    onTask: () -> Unit,
) {
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    val mounted = transition.currentState || transition.targetState
    if (!mounted) return

    Box(
        Modifier
            .fillMaxSize()
            .zIndex(4f),
    ) {
        // Transparent scrim — taps outside the menu dismiss it.
        Box(
            Modifier
                .fillMaxSize()
                .noFeedbackClick(onClick = onDismiss),
        )
        // Menu, anchored just below the header "+" (status bar + title row).
        Box(
            Modifier
                .padding(
                    top = statusBarDp + ScrollTopPadding + TitleHeight - 2.dp,
                    start = ScreenHorizontalPadding,
                ),
        ) {
            AnimatedVisibility(
                visibleState = transition,
                enter = scaleIn(
                    initialScale = 0.86f,
                    animationSpec = tween(durationMillis = 170),
                    transformOrigin = TransformOrigin(0f, 0f),
                ) + fadeIn(tween(130)),
                exit = scaleOut(
                    targetScale = 0.86f,
                    animationSpec = tween(durationMillis = 140),
                    transformOrigin = TransformOrigin(0f, 0f),
                ) + fadeOut(tween(110)),
            ) {
                Column(
                    Modifier
                        .width(216.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Letify.colors.container)
                        .padding(vertical = 6.dp),
                ) {
                    PlanCreateMenuItem("restart-bold-duotone", "Привычка", onHabit)
                    PlanCreateMenuItem("clipboard-list-bold-duotone", "Задача", onTask)
                }
            }
        }
    }
}

@Composable
private fun PlanCreateMenuItem(icon: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .noFeedbackClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SolarIcon(name = icon, tint = Letify.colors.accent, size = 22.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = Letify.colors.text,
            style = Letify.typography.titleMedium,
        )
    }
}

/**
 * Reads the device tilt (left-right lean) from the accelerometer and returns a
 * smoothed angle in degrees. 0° = upright; positive = right side dipped. Used to
 * keep the water surface "level" to gravity inside every [LiquidVessel].
 */
// Returns a STABLE provider lambda — NOT the raw Float. Reading the tilt as a
// State directly in the PlanScreen body made every accelerometer sample
// (SENSOR_DELAY_GAME ≈ 50 Hz) recompose the WHOLE heavy Plan screen (the task
// list and everything). When that 50 Hz recompose storm collided with the
// collapse spring or a tab-switch slide, the main thread starved and the
// docking circles dropped frames — they "abruptly vanished for ~a second then
// snapped back". By handing down a `() -> Float` and only invoking it inside
// the tiny [LiquidVessel] composables, the sensor recomposes just those small
// Canvases — same slosh, no screen-wide recompose. Same lambda pattern the rest
// of this screen already uses for scroll/collapse to avoid recomposition.
@Composable
fun rememberDeviceTilt(): () -> Float {
    // Tilt-driven wave motion DISABLED by request: the liquid surface must stay
    // static and never react to device lean. We return a constant 0° provider
    // (no sensor registered at all → also a small battery win). With tilt fixed
    // at 0 the per-vessel motion gating keeps the surface calm and flat.
    return remember { { 0f } }
}

@Suppress("unused")
@Composable
private fun rememberDeviceTiltSensorDisabled(): () -> Float {
    val context = LocalContext.current
    val tilt = remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val ax = e.values[0]
                val ay = e.values[1]
                val mag = sqrt(ax * ax + ay * ay)
                val target = if (mag < 1.5f) 0f
                    else Math.toDegrees(atan2((-ax).toDouble(), ay.toDouble()))
                        .toFloat().coerceIn(-38f, 38f)
                tilt.floatValue += (target - tilt.floatValue) * 0.10f
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) {
            sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sm?.unregisterListener(listener) }
    }
    // Stable identity so threading it down doesn't itself trigger recomposition.
    return remember { { tilt.floatValue } }
}

/**
 * A round vessel that fills with the habit colour by [progress] (0..1). The
 * fluid surface is an animated sine wave that **tilts with the device lean** (device
 * lean) so the water always stays "level" to gravity and sloshes side to side.
 * Rotating the surface about the vessel centre keeps the filled area — and thus
 * the visible volume — constant while tilting. The icon tint flips to white
 * once the fluid passes it, and a check badge + full fill appears when done.
 */
@Composable
fun LiquidVessel(
    fill: Float,
    color: Color,
    icon: String,
    size: Dp,
    tiltProvider: () -> Float = { 0f },
) {
    // Spring on the tilt → gentle inertia/slosh. Critically-ish damped so the
    // surface glides smoothly to the new angle instead of snapping/jittering.
    val tilt by animateFloatAsState(
        targetValue = tiltProvider(),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tilt",
    )
    // Phase only advances the ripple SHAPE; its amplitude is gated by motion
    // below, so a still phone shows a calm, flat surface (no self-animation).
    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "phase",
    )
    // How fast the water is currently sloshing: the lag between the raw tilt
    // target and the springy value. Smoothed with its own spring so the ripple
    // amplitude rises/falls gradually (no abrupt "snap" of the wave).
    val rawMotion = (kotlin.math.abs(tiltProvider() - tilt) / 9f).coerceIn(0f, 1f)
    val motion by animateFloatAsState(
        targetValue = rawMotion,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
        label = "motion",
    )
    val track = Letify.colors.track
    val ground = Letify.colors.text
    val isDark = Letify.colors.isDark
    // Smooth colour→white cross-fade as the fluid passes the icon (no hard flip).
    val tintFraction = ((fill - 0.40f) / 0.18f).coerceIn(0f, 1f)
    val iconTint = lerp(color, Color.White.copy(alpha = 0.95f), tintFraction)

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val d = this.size.minDimension
            val r = d / 2f
            val center = Offset(r, r)
            // empty-vessel ground tint
            drawCircle(color = ground.copy(alpha = 0.05f), radius = r, center = center)
            if (fill > 0f) {
                clipPath(Path().apply { addOval(Rect(0f, 0f, d, d)) }) {
                    // Surface normal N points "down into the water" (toward gravity).
                    // Tilt rotates N about the vessel centre; volume stays constant.
                    val rad = (tilt * PI.toFloat() / 180f)
                    val nx = sin(rad)
                    val ny = cos(rad)
                    // tangent along the surface
                    val tx = -ny
                    val ty = nx
                    // signed distance from centre to the surface line along N
                    val s = d * (fill - 0.5f)
                    // a point on the surface line
                    val px = center.x - s * nx
                    val py = center.y - s * ny
                    // ripple amplitude: zero when still, grows gently with slosh
                    val amp = if (fill >= 0.999f) 0f else d * 0.032f * motion
                    val halfLen = d * 0.95f
                    val depth = d * 1.9f
                    val seg = 28

                    // Sample the (possibly rippling) surface line once; reuse the
                    // points for both the fluid fill and the surface highlight.
                    val sx = FloatArray(seg + 1)
                    val sy = FloatArray(seg + 1)
                    for (i in 0..seg) {
                        val f = i / seg.toFloat()
                        val along = -halfLen + 2f * halfLen * f
                        val w = amp * sin(phase + f * 2f * PI.toFloat() * 1.6f)
                        sx[i] = px + along * tx + w * nx
                        sy[i] = py + along * ty + w * ny
                    }
                    // single clean fluid body (one surface, no double waves)
                    val fluid = Path().apply {
                        moveTo(sx[0], sy[0])
                        for (i in 1..seg) lineTo(sx[i], sy[i])
                        lineTo(sx[seg] + depth * nx, sy[seg] + depth * ny)
                        lineTo(sx[0] + depth * nx, sy[0] + depth * ny)
                        close()
                    }
                    drawPath(
                        fluid,
                        brush = Brush.linearGradient(
                            colors = listOf(color, color.copy(alpha = 0.85f)),
                            start = Offset(px, py),
                            end = Offset(px + d * nx, py + d * ny),
                        ),
                    )
                    // (removed: white waterline highlight, glass rim & sheen —
                    // per request. The wavy fluid top is kept, so the wave still
                    // reads, just without any glassy/specular treatment.)
                }
            }
        }
        // No completion badge/checkmark: a finished habit dims a little and
        // glides to the end of the row, so the vessel always just shows its icon.
        SolarIcon(name = icon, tint = iconTint, size = size * 0.42f)
    }
}

/** Current daily streak: consecutive completed days ending today. Today not
 *  yet finished does not break the streak (it just isn't counted). */
private fun currentStreak(habit: Habit): Int {
    var streak = 0
    var date = LocalDate.now()
    var guard = 0
    while (guard < 999) {
        guard++
        if (habit.isDoneOn(date.toString())) {
            streak++
            date = date.minusDays(1)
        } else if (date == LocalDate.now()) {
            // today incomplete — look back without breaking the streak
            date = date.minusDays(1)
        } else {
            break
        }
    }
    return streak
}

// ---------- Schedule list -----------------------------------------------------

@Composable
// Short relative-time label ("через N", "до конца N") for the schedule cards.
private fun relMin(total: Int): String = when {
    total <= 0 -> "сейчас"
    total < 60 -> "$total мин"
    total % 60 == 0 -> "${total / 60}ч"
    else -> "${total / 60}ч ${total % 60}м"
}

@Composable
private fun TaskMetaDot() {
    Box(
        Modifier
            .size(3.dp)
            .background(Letify.colors.muted.copy(alpha = 0.6f), CircleShape)
    )
}

// Dimmed-card alpha used both for resting «Прошедшие» cards and as the target
// of the completion dim phase, so the hand-off between sections is seamless.
private const val PastCardAlpha = 0.55f

// How long the completed card keeps its frozen «live» look after the state
// flips — covers the placement-spring flight; once elapsed, the card relaxes
// into the past styling (fill fades, ring shrinks out, meta crossfades).
private const val TaskFlightSettleMs = 560L

// Glides a Column child to its new position when it is RE-SLOTTED — powers
// the schedule card's flight into «Прошедшие» and the neighbours closing the
// one-slot gap. Crucially, it must NOT fight continuous layout shifts (e.g. a
// subtask card expanding above pushes everything down a few px per frame —
// those must be followed 1:1, perfectly in sync, like plain layout does).
// Heuristic: a re-slot changes the position by at least a whole card in a
// single frame, while continuous shifts arrive as many small deltas — so only
// jumps larger than the threshold start a spring; small ones snap instantly
// (unless a flight is already running, then it just retargets smoothly).
@Composable
private fun Modifier.animatePlacement(): Modifier {
    val scope = rememberCoroutineScope()
    val thresholdPx = with(LocalDensity.current) { 52.dp.toPx() }
    var targetOffset by remember { mutableStateOf<IntOffset?>(null) }
    var animatable by remember {
        mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null)
    }
    val airborne = animatable?.isRunning == true
    return this
        .zIndex(if (airborne) 1f else 0f)
        .onPlaced { targetOffset = it.positionInParent().round() }
        .offset {
            val target = targetOffset ?: return@offset IntOffset.Zero
            val anim = animatable
                ?: Animatable(target, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != target) {
                val bigJump =
                    kotlin.math.abs(target.x - anim.targetValue.x) > thresholdPx ||
                        kotlin.math.abs(target.y - anim.targetValue.y) > thresholdPx
                if (bigJump || anim.isRunning) {
                    // Whole-slot re-slot (flight / gap close) → spring there.
                    scope.launch {
                        anim.animateTo(
                            target,
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 260f,
                                visibilityThreshold = IntOffset(1, 1),
                            ),
                        )
                    }
                } else {
                    // Small continuous shift (a sibling expanding/collapsing a
                    // few px per frame) → just keep the anchor in sync, NO
                    // animation, so the card tracks layout 1:1 like before.
                    scope.launch { anim.snapTo(target) }
                }
            }
            anim.value - target
        }
}

// Redesigned schedule card — "Прогресс-заливка" (variant 2):
//   • one tinted icon, no tile/подложка; name; de-emphasised time, no big clock
//   • live      → soft rounded task-colour fill grows start→end, "до конца N мин"
//                 + a plain hollow ring on the right (ONLY here) to mark it done
//   • upcoming  → no right-side control at all
//   • done      → card slowly dims IN PLACE, then the very same card node flies
//                 down into «Прошедшие» (movableContent + animatePlacement in
//                 the caller — no hide/reappear, no check badges anywhere)
@Composable
private fun TaskCard(
    task: TaskItem,
    nowMin: Int,
    dateKey: String,
    completingTasks: MutableMap<Int, Boolean>,
) {
    // A task with a checklist gets its own card: a ring of progress on the left
    // (done/total) that expands the card to reveal the sub-items. Ticking every
    // sub-item completes the task.
    if (task.hasSubtasks) {
        SubtaskTaskCard(task, dateKey)
        return
    }
    val state = LocalAppState.current
    val interaction = LocalPlanInteraction.current
    val selecting = interaction?.selectionMode == true
    val isSelected = interaction?.selectedIds?.contains(task.id) == true
    // When this card is the long-pressed peek target, it is hidden here so the
    // sharp replica in the overlay is the only visible copy (no blur halo, no
    // duplicate).
    val isPeek = interaction?.peekTarget?.let { it.kind == PeekKind.Task && it.id == task.id } == true
    val status = task.statusAt(nowMin, dateKey)
    val isPast = status == TaskStatus.Done
    val inWindow = status == TaskStatus.Live
    val isLive = inWindow

    val color = task.color

    // ── Completion choreography: ONE thing happens at a time ──
    //  1. tap → ring fills, card slowly dims IN PLACE; meanwhile the
    //     «Прошедшие» header (if it wasn't there yet) quietly slides in below,
    //     so the destination is ready before anything moves.
    //  2. state flips → the SAME card flies down; its content is FROZEN
    //     exactly as it looked (progress fill, ring, «до конца…») — the only
    //     thing happening on screen is the flight + neighbours closing the gap
    //     with the identical spring.
    //  3. after landing the card calmly relaxes into its «past» look: fill
    //     fades away, ring shrinks out, meta crossfades to «Выполнено».
    var completing by remember { mutableStateOf(false) }
    val frozen = completing && isPast // airborne/just landed, still wearing the live look
    val dimAnim = remember { Animatable(if (isPast) PastCardAlpha else 1f) }
    LaunchedEffect(completing) {
        if (completing) {
            completingTasks[task.id] = true
            // Phase 1 — плавно тускнеет (card dims in place, header settles in).
            dimAnim.animateTo(PastCardAlpha, tween(durationMillis = 480, easing = FastOutSlowInEasing))
            delay(80)
            // Phase 2 — flip the state; the flight itself is driven outside.
            state.toggleTaskDone(task.id)
            // Phase 3 — wait out the flight, then release the frozen look.
            delay(TaskFlightSettleMs)
            completing = false
            completingTasks.remove(task.id)
        }
    }
    DisposableEffect(Unit) { onDispose { completingTasks.remove(task.id) } }
    // Keep the dim level in sync when status changes WITHOUT the tap flow —
    // e.g. a task whose time window simply ran out fades while it glides down.
    LaunchedEffect(isPast) {
        val target = if (isPast) PastCardAlpha else 1f
        if (!dimAnim.isRunning && dimAnim.value != target) {
            dimAnim.animateTo(target, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
        if (!isPast) completing = false
    }

    val liveLook = isLive || frozen
    val nameColor by animateColorAsState(
        targetValue = if (isPast && !frozen) Letify.colors.muted else Letify.colors.text,
        animationSpec = tween(260),
        label = "taskNameColor",
    )
    val progress = if (liveLook) {
        (task.elapsedMinutes(nowMin).toFloat() / task.durationMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    } else 0f
    // Freeze the last shown fill width so the pill doesn't shrink while it
    // fades away after the landing.
    var fillProgress by remember { mutableStateOf(0f) }
    if (liveLook) fillProgress = progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { interaction?.onTaskBounds(task.id, it.boundsInWindow()) }
            .graphicsLayer { alpha = if (isPeek) 0f else dimAnim.value }
            .clip(RoundedCornerShape(24.dp))
            .background(Letify.colors.container)
            .peekGesture(
                longPressEnabled = !selecting,
                onTap = { if (selecting) interaction?.onToggleSelect(task.id) },
                onPeekStart = { p -> if (!selecting) interaction?.onTaskPeekStart(task.id, p, false) },
                onPeekMove = { p -> interaction?.onPeekMove(p) },
                onPeekEnd = { interaction?.onPeekEnd() },
            ),
    ) {
        // ── Rounded progress-fill pill, inset inside the card ──
        // Shown while the task runs AND kept frozen during the completion
        // flight; it only fades away once the card has landed in «Прошедшие».
        AnimatedVisibility(
            visible = liveLook,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
        ) {
            val fillColor = color.copy(alpha = 0.20f)
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillProgress.coerceAtLeast(0.001f))
                        .background(fillColor)
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Single icon — no coloured tile. Past keeps its own icon (dimmed
            // via the card alpha), just like upcoming/live — no check badge.
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                SolarIcon(name = task.icon, tint = color, size = 26.dp)
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    task.name,
                    color = nameColor,
                    style = Letify.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                // The meta line stays frozen on its live look during the
                // flight and only crossfades to «Выполнено» after landing.
                val metaMode = when {
                    liveLook -> 0
                    isPast -> 1
                    else -> 2
                }
                Crossfade(
                    targetState = metaMode,
                    animationSpec = tween(260),
                    label = "taskMeta",
                ) { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (mode) {
                            0 -> {
                                // "идёт сейчас" removed — the live styling already makes
                                // it obvious; just show the time left.
                                Text(
                                    "до конца ${relMin(task.remainingMinutes(nowMin).coerceAtLeast(1))}",
                                    color = color, style = Letify.typography.bodySmall,
                                )
                            }
                            1 -> {
                                Text(task.startTime, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                                Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                                Text(task.durationLabel, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                                Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                                Text("Выполнено", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                            }
                            else -> {
                                Text(task.startTime, color = Letify.colors.text, style = Letify.typography.bodySmall)
                                Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                                Text(task.durationLabel, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ── Right control ──
            // In selection mode every card shows a selection circle on the
            // right instead of its live completion ring.
            if (selecting) {
                Spacer(Modifier.width(10.dp))
                SelectionCircle(selected = isSelected)
            } else {
                // The completion ring lives ONLY on the task that is running
                // right now — upcoming and past cards show nothing on the right.
                // It stays filled through the flight and shrinks away after
                // landing.
                AnimatedVisibility(
                    visible = liveLook,
                    enter = fadeIn(tween(200)) + expandHorizontally(tween(260, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(220)) + shrinkHorizontally(tween(300, easing = FastOutSlowInEasing)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(10.dp))
                        TaskCheckCircle(
                            filled = completing,
                            color = color,
                        ) { if (!completing && isLive) completing = true }
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Subtask task card — ring-progress button (left) that expands a checklist.
// ───────────────────────────────────────────────────────────────────────────
@Composable
private fun SubtaskTaskCard(task: TaskItem, dateKey: String) {
    val state = LocalAppState.current
    val interaction = LocalPlanInteraction.current
    val selecting = interaction?.selectionMode == true
    val isSelected = interaction?.selectedIds?.contains(task.id) == true
    val isPeek = interaction?.peekTarget?.let { it.kind == PeekKind.Task && it.id == task.id } == true
    val done = task.subtaskProgressOn(dateKey).first
    val total = task.subtaskProgressOn(dateKey).second
    val allDone = task.allSubtasksDoneOn(dateKey)
    val checkedIds = task.completedSubtasksOn(dateKey)
    val color = task.color

    // Tap the head (or chevron) to expand the full checklist. The reveal is a
    // pure top-anchored clip — the items are laid out from frame 1 and just get
    // clipped by the growing/shrinking container, so they appear instantly with
    // the card and vanish WITH it on close (no peek, no separate fade).
    var expanded by remember { mutableStateOf(false) }
    val chevronRot by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(280),
        label = "subchev",
    )

    val nameColor = if (allDone) Letify.colors.muted else Letify.colors.text

    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { interaction?.onTaskBounds(task.id, it.boundsInWindow()) }
            .graphicsLayer { alpha = if (isPeek) 0f else if (allDone) 0.62f else 1f }
            .clip(RoundedCornerShape(24.dp))
            .background(Letify.colors.container),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .peekGesture(
                        longPressEnabled = !selecting,
                        onTap = { if (selecting) interaction?.onToggleSelect(task.id) else expanded = !expanded },
                        onPeekStart = { p -> if (!selecting) interaction?.onTaskPeekStart(task.id, p, expanded) },
                        onPeekMove = { p -> interaction?.onPeekMove(p) },
                        onPeekEnd = { interaction?.onPeekEnd() },
                    )
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubtaskRing(done = done, total = total, color = color)
                Spacer(Modifier.width(13.dp))
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    SolarIcon(name = task.icon, tint = color, size = 26.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        task.name,
                        color = nameColor,
                        style = Letify.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.startTime, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                        Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                        Text(
                            if (allDone) "Выполнено" else "$done из $total",
                            color = if (allDone) color else Letify.colors.muted,
                            style = Letify.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (selecting) {
                    SelectionCircle(selected = isSelected)
                } else {
                    // Same custom down-chevron as the task editor (rotates 180°).
                    Box(
                        Modifier.size(30.dp).graphicsLayer { rotationZ = chevronRot },
                        contentAlignment = Alignment.Center,
                    ) {
                        ExpandChevron(color = Letify.colors.muted)
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(280), expandFrom = Alignment.Top),
                exit = shrinkVertically(animationSpec = tween(240), shrinkTowards = Alignment.Top),
            ) {
                Column(Modifier.padding(start = 15.dp, end = 15.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Letify.colors.muted.copy(alpha = 0.12f)),
                    )
                    task.subtasks.forEach { st ->
                        SubtaskItemRow(
                            title = st.title,
                            checked = st.id in checkedIds,
                            color = color,
                            onToggle = { state.toggleSubtask(task.id, st.id) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// Custom down-chevron — rounded V drawn so it stays crisp at any size; shared
// look between the Plan subtask card and the task editor's hero card.
@Composable
private fun ExpandChevron(color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width; val h = size.height
        val sw = 2.2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.40f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.76f, h * 0.40f)
        }
        drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// 40dp ring button: track + progress arc with a "done/total" count in the hole.
@Composable
private fun SubtaskRing(done: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    val target = if (total > 0) done.toFloat() / total else 0f
    val animated by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(420),
        label = "subring",
    )
    val track = Letify.colors.muted.copy(alpha = 0.18f)
    val cntColor = if (done >= total && total > 0) color else Letify.colors.text
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp)) {
            val sw = 4.dp.toPx()
            val inset = sw / 2f
            val arcSize = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Text(
            "$done/$total",
            color = cntColor,
            style = Letify.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun SubtaskItemRow(
    title: String,
    checked: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .noFeedbackClick(onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (checked) color else Letify.colors.muted.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                SolarIcon(name = "check-bold", tint = Letify.colors.container, size = 13.dp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            color = if (checked) Letify.colors.muted else Letify.colors.text,
            style = Letify.typography.bodyLarge,
            textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

// ○ Completion ring — the right-side control of the task running right now.
// A plain hollow circle, no check mark. Tapping it kicks off the completion
// choreography; while it plays, the ring fills with the task colour as the
// only tap feedback.
@Composable
private fun TaskCheckCircle(
    filled: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onToggle: () -> Unit,
) {
    val fill by animateFloatAsState(
        targetValue = if (filled) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "taskring",
    )
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .noFeedbackClick(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .border(2.dp, Letify.colors.muted.copy(alpha = 0.85f), CircleShape)
        )
        if (fill > 0.01f) {
            Box(
                Modifier
                    .size(24.dp)
                    .graphicsLayer { scaleX = fill; scaleY = fill }
                    .background(color, CircleShape)
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
// Long-press context menu (peek) + multi-select chrome
// ═══════════════════════════════════════════════════════════════════════════

private val PeekDestructiveColor = Color(0xFFFF453A)

private data class PeekMenuItem(
    val label: String,
    val icon: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * iOS-style "peek" overlay shown on long-press: the background is already
 * blurred (driven by the caller), here we add a dimming scrim, a faithful
 * replica of the held item kept at its on-screen position ([rect], window
 * coords), and a rounded action menu placed below or above the item depending
 * on where it sits on screen.
 */
@Composable
private fun PeekOverlay(
    rect: Rect,
    screenHeightPx: Float,
    screenWidthPx: Float,
    progress: () -> Float,
    pointer: () -> Offset?,
    releaseSignal: Int,
    items: List<PeekMenuItem>,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    val gapPx = with(density) { 12.dp.toPx() }
    val sidePadPx = with(density) { 16.dp.toPx() }
    val menuWidthDp = 184.dp
    val menuWidthPx = with(density) { menuWidthDp.toPx() }
    val rowHeightDp = 44.dp
    val rowHeightPx = with(density) { rowHeightDp.toPx() }
    val menuPadTopPx = with(density) { 4.dp.toPx() }
    val menuHeightPx = with(density) { (rowHeightDp * items.size + 8.dp).toPx() }

    val below = rect.center.y < screenHeightPx * 0.5f
    val menuTopPx = if (below) rect.bottom + gapPx else (rect.top - gapPx - menuHeightPx)
    val maxLeft = (screenWidthPx - sidePadPx - menuWidthPx).coerceAtLeast(sidePadPx)
    val menuLeftPx = (rect.center.x - menuWidthPx / 2f).coerceIn(sidePadPx, maxLeft)

    val itemWidthDp = with(density) { rect.width.toDp() }

    // Which menu row the finger is over (drag-to-select). -1 = none.
    val pt = pointer()
    val hovered: Int = run {
        val p = pt ?: return@run -1
        if (p.x < menuLeftPx || p.x > menuLeftPx + menuWidthPx) return@run -1
        val rel = p.y - menuTopPx - menuPadTopPx
        if (rel < 0f) return@run -1
        val idx = (rel / rowHeightPx).toInt()
        if (idx in items.indices) idx else -1
    }
    // Light tick whenever the highlighted row changes (like iOS).
    LaunchedEffect(hovered) {
        if (hovered != -1) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    // On finger lift: fire the highlighted row. If the finger isn't over any
    // row (e.g. a plain long-press with no drag) keep the menu open so the user
    // can still tap an item.
    LaunchedEffect(releaseSignal) {
        // Finger lifted. The peek menu only lives while the finger is held:
        // if the finger was resting on a menu row, fire that row; otherwise
        // the press is simply over, so dismiss the whole peek and let
        // everything settle back to normal.
        if (releaseSignal > 0) {
            if (hovered != -1) items[hovered].onClick() else onDismiss()
        }
    }

    Box(Modifier.fillMaxSize().zIndex(10f)) {
        // No blur — just a gentle dim tinted to the theme background colour, so
        // the screen reads as "stepped back" without any frosted-glass effect.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress() }
                .background(Letify.colors.bg.copy(alpha = 0.6f))
                .noFeedbackClick { onDismiss() },
        )

        // The held item, redrawn at its exact original position so it "stays"
        // crisply in place while everything around it blurs. It grows slightly
        // while held and shrinks back on release (driven by progress).
        Box(
            Modifier
                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .width(itemWidthDp)
                .graphicsLayer {
                    // Barely-there lift — just enough to read as "picked up".
                    val s = 1f + 0.015f * progress()
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                },
        ) {
            content()
        }

        // Rounded action menu.
        Box(
            Modifier
                .offset { IntOffset(menuLeftPx.roundToInt(), menuTopPx.roundToInt()) }
                .width(menuWidthDp)
                .graphicsLayer {
                    alpha = progress()
                    val s = 0.9f + 0.1f * progress()
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, if (below) 0f else 1f)
                }
                .clip(RoundedCornerShape(18.dp))
                .background(Letify.colors.container),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                items.forEachIndexed { i, item ->
                    // Icon LEFT, label RIGHT. Icons are grey (muted); destructive
                    // rows go red. No dividers — compact, clean list. The row
                    // under the finger shrinks slightly (no grey bg) to show it
                    // will be picked on release.
                    val iconTint = if (item.destructive) PeekDestructiveColor else Letify.colors.muted
                    val textTint = if (item.destructive) PeekDestructiveColor else Letify.colors.text
                    // Only a whisper of a press dip — a soft, subtle cue, not a
                    // deep "bend". Short tween so dragging across rows reads
                    // calm rather than springy/exaggerated.
                    val rowScale by animateFloatAsState(
                        targetValue = if (i == hovered) 0.975f else 1f,
                        animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing),
                        label = "peekRowScale",
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(rowHeightDp)
                            .graphicsLayer { scaleX = rowScale; scaleY = rowScale }
                            .noFeedbackClick { item.onClick() }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SolarIcon(name = item.icon, tint = iconTint, size = 20.dp)
                        Spacer(Modifier.width(13.dp))
                        Text(
                            item.label,
                            color = textTint,
                            style = Letify.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Static, non-interactive replica of a task card's CURRENT resting look — must
 * read 1:1 with the live card it stands in for, so the long-press peek looks
 * like the same card was simply lifted out (no second/different card appears).
 * Branches on subtasks and carries the exact resting dim (past/done) so a
 * dimmed card never "pops" brighter the instant it's held.
 */
@Composable
private fun PeekTaskCard(task: TaskItem, nowMin: Int, dateKey: String, expanded: Boolean = false) {
    val color = task.color

    // ── Subtask card (ring + checklist head) — collapsed resting look ──
    if (task.hasSubtasks) {
        val (done, total) = task.subtaskProgressOn(dateKey)
        val allDone = task.allSubtasksDoneOn(dateKey)
        val checkedIds = task.completedSubtasksOn(dateKey)
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (allDone) 0.62f else 1f }
                .clip(RoundedCornerShape(24.dp))
                .background(Letify.colors.container),
        ) {
          Column {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubtaskRing(done = done, total = total, color = color)
                Spacer(Modifier.width(13.dp))
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    SolarIcon(name = task.icon, tint = color, size = 26.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        task.name,
                        color = if (allDone) Letify.colors.muted else Letify.colors.text,
                        style = Letify.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(task.startTime, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                        Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                        Text(
                            if (allDone) "Выполнено" else "$done из $total",
                            color = if (allDone) color else Letify.colors.muted,
                            style = Letify.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(30.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                    contentAlignment = Alignment.Center,
                ) {
                    ExpandChevron(color = Letify.colors.muted)
                }
            }
            if (expanded) {
                Column(Modifier.padding(start = 15.dp, end = 15.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Letify.colors.muted.copy(alpha = 0.12f)),
                    )
                    task.subtasks.forEach { st ->
                        SubtaskItemRow(
                            title = st.title,
                            checked = st.id in checkedIds,
                            color = color,
                            onToggle = {},
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
          }
        }
        return
    }

    // ── Simple task card — mirrors TaskCard's live / upcoming / past look ──
    val status = task.statusAt(nowMin, dateKey)
    val isPast = status == TaskStatus.Done
    val isLive = status == TaskStatus.Live
    val progress = if (isLive) {
        (task.elapsedMinutes(nowMin).toFloat() / task.durationMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    } else 0f

    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isPast) PastCardAlpha else 1f }
            .clip(RoundedCornerShape(24.dp))
            .background(Letify.colors.container),
    ) {
        if (isLive) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(24.dp))) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceAtLeast(0.001f))
                        .background(color.copy(alpha = 0.20f))
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                SolarIcon(name = task.icon, tint = color, size = 26.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.name,
                    color = if (isPast) Letify.colors.muted else Letify.colors.text,
                    style = Letify.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        isLive -> Text(
                            "до конца ${relMin(task.remainingMinutes(nowMin).coerceAtLeast(1))}",
                            color = color, style = Letify.typography.bodySmall,
                        )
                        isPast -> {
                            Text(task.startTime, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                            Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                            Text(task.durationLabel, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                            Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                            Text("Выполнено", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                        }
                        else -> {
                            Text(task.startTime, color = Letify.colors.text, style = Letify.typography.bodySmall)
                            Spacer(Modifier.width(7.dp)); TaskMetaDot(); Spacer(Modifier.width(7.dp))
                            Text(task.durationLabel, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                        }
                    }
                }
            }
            if (isLive) {
                Spacer(Modifier.width(10.dp))
                // Resting (unfilled) completion ring — same as the live card.
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .border(2.dp, Letify.colors.muted.copy(alpha = 0.85f), CircleShape)
                    )
                }
            }
        }
    }
}

/** Static replica of a habit vessel for the long-press peek. */
@Composable
private fun PeekHabitVessel(habit: Habit, dateKey: String) {
    val count = habit.progressOn(dateKey)
    val target = habit.effectiveTarget.coerceAtLeast(1)
    // Animate the fill with the SAME smooth spring as the live vessel so that
    // tapping «Сбросить» makes the wave glide down gracefully instead of
    // snapping to empty. The held replica is what's on screen at that moment
    // (and stays so through the close-fade), and the real vessel underneath
    // uses an identical spring from the same start value, so the recede reads
    // as one continuous, smooth drain across the hand-off.
    val targetFill = (count.toFloat() / target).coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
        label = "peekVesselFill",
    )
    // Carry the SAME completion dim the live vessel uses (a done habit renders
    // at 0.55 alpha). Without this the replica popped to full brightness on
    // long-press — the "фон резко светлеет" bug.
    val dim = if (habit.isDoneOn(dateKey)) 0.55f else 1f
    Box(Modifier.size(RingNatural), contentAlignment = Alignment.Center) {
        Box(Modifier.graphicsLayer { alpha = dim }) {
            LiquidVessel(fill = fill, color = habit.color, icon = habit.icon, size = RingNatural)
        }
    }
}

/** Selection circle shown on the right of each task card in selection mode. */
@Composable
private fun SelectionCircle(selected: Boolean) {
    if (selected) {
        SolarIcon(name = "check-circle-bold", tint = Letify.colors.accent, size = 26.dp)
    } else {
        Box(
            Modifier
                .size(24.dp)
                .border(2.dp, Letify.colors.muted.copy(alpha = 0.55f), CircleShape),
        )
    }
}

/** Rounded top bar shown while multi-selecting tasks. */
@Composable
private fun SelectionTopBar(
    count: Int,
    statusBarDp: Dp,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(9f)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = ScrollTopPadding, start = 16.dp, end = 16.dp)
            .height(TitleHeight),
    ) {
        Row(
            // The selection bar is its own rounded «island» (opaque container
            // fill) floating over the page — this is also what cleanly hides the
            // pinned «план» title and docked habit cluster underneath it.
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Letify.colors.container)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.noFeedbackClick { onCancel() }) {
                Text("Отмена", color = Letify.colors.accent, style = Letify.typography.titleSmall)
            }
            Text(
                if (count > 0) "Выбрано $count" else "Выберите задачи",
                color = Letify.colors.text,
                style = Letify.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
            )
            Box(Modifier.noFeedbackClick { onSelectAll() }) {
                SolarIcon(name = "checklist-minimalistic-bold", tint = Letify.colors.accent, size = 24.dp)
            }
            Spacer(Modifier.width(16.dp))
            Box(
                Modifier.noFeedbackClick(enabled = count > 0) { onDelete() }
                    .graphicsLayer { alpha = if (count > 0) 1f else 0.35f },
            ) {
                SolarIcon(name = "trash-bin-trash-bold", tint = PeekDestructiveColor, size = 24.dp)
            }
        }
    }
}
