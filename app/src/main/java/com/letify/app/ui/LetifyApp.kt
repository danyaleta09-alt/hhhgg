package com.letify.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.letify.app.ui.components.Navbar
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.OverlayHost
import com.letify.app.ui.components.overlayHostShiftFraction
import com.letify.app.ui.components.RoundedSlideOverlay
import com.letify.app.ui.components.rememberParallaxProgress
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.screens.AddHabitScreen
import com.letify.app.ui.screens.AddNutritionScreen
import com.letify.app.ui.screens.AddSleepScreen
import com.letify.app.ui.screens.AddTaskScreen
import com.letify.app.ui.screens.AddWeightScreen
import com.letify.app.ui.screens.AppearanceScreen
import com.letify.app.ui.screens.BindingsScreen
import com.letify.app.ui.screens.EditProfileScreen
import com.letify.app.ui.screens.GoalsScreen
import com.letify.app.ui.screens.HomeScreen
import com.letify.app.ui.screens.LogsScreen
import com.letify.app.ui.screens.NotificationsScreen
import com.letify.app.ui.screens.OtherScreen
import com.letify.app.ui.screens.NutritionScreen
import com.letify.app.ui.screens.PlanScreen
import com.letify.app.ui.screens.ProfileScreen
import com.letify.app.ui.screens.ProgressGoalsScreen
import com.letify.app.ui.screens.WaterHistoryScreen
import com.letify.app.ui.screens.MediaScreen
import com.letify.app.ui.screens.CameraCaptureScreen
import com.letify.app.ui.screens.CameraPrewarm
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.Tab
import com.letify.app.ui.state.TransitionStyle
import com.letify.app.ui.theme.Letify
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Identifies which secondary screen is currently active on top of the tabs.
 * Most are full-screen "slide in from the right" overlays; weight is rendered
 * as a real BottomSheet because it's a single-value picker.
 */
// Tab-switch push: same gentle ease-out curve + duration the navbar pill uses,
// so the bottom-tab swap and the indicator glide on one consistent motion.
private val TabPushEasing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)
private const val TabPushMs = 320

// Smooth decelerate for the camera slide-up from bottom.
private val CameraSlideEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
private const val CameraSlideInMs = 380
private const val CameraSlideOutMs = 300

sealed interface AddOverlay {
    // editId != null → open the create screen pre-filled to EDIT that item
    // (from the long-press «Изменить» menu) instead of creating a new one.
    data class Habit(val editId: Int? = null) : AddOverlay
    data class Task(val editId: Int? = null) : AddOverlay
    data object Nutrition : AddOverlay
    data object Weight : AddOverlay
    data object Sleep : AddOverlay
    data object EditProfile : AddOverlay
    data object Goals : AddOverlay
    data object Appearance : AddOverlay
    data object Notifications : AddOverlay
    data object Bindings : AddOverlay
    data object Tiwi : AddOverlay
    data object Other : AddOverlay
    data object Logs : AddOverlay
    data object ProgressGoals : AddOverlay
    data object WaterHistory : AddOverlay
    data object Media : AddOverlay
}

/**
 * Stable key for the SaveableStateHolder so a screen keeps its saveable state
 * (scroll offset, expanded sections…) when it moves between the active-top slot
 * and the static underlay slot. Data-class overlays include their id so two
 * different detail/schedule screens never collide.
 */
private fun AddOverlay.stateKey(): String = this::class.java.simpleName

@Composable
fun LetifyApp() {
    val state = LocalAppState.current

    // Snap to whatever the user picked as their default landing tab once on
    // first composition of the app shell.
    //
    // Keying this on `state.defaultTab` (the previous behaviour) was a
    // landmine: opening Appearance → Навбар and picking a new default tab
    // would mutate `currentTab` while the user was still inside the nested
    // overlay, kicking off a tab-content transition behind the overlay
    // stack. Swiping back from Навбар then races against that transition,
    // which crashed the app on some devices. The setting is a "next launch"
    // preference, not a "switch right now" action, so we only honour it on
    // first entry. From then on `currentTab` is owned solely by direct nav
    // (tab bar tap / overlay nav handlers).
    LaunchedEffect(Unit) {
        state.currentTab = state.defaultTab
    }

    // Stack of overlays so a sub-screen can pop back to its parent
    // (e.g. Logs opened from Other slides back into Other, not all the
    // way to the home screen).
    //
    // Rendering model: the *top* of the stack is the active overlay —
    // it gets a RoundedSlideOverlay that drives the host parallax (so
    // the home tab content slides/zooms behind it), is interactive, and
    // can be dismissed by swipe-back. If the stack has a level below
    // (e.g. [Other, Logs]) we render that **second-from-top** overlay
    // statically as an "underlay" behind the active one, with no swipe
    // gestures and a no-op back. That guarantees:
    //   - When the user taps Логи from Другое, the slide-in of Logs
    //     reveals Other behind it — not the home tab (Profile). The
    //     previous code unmounted Other on the key change and the user
    //     briefly saw Profile through the gap.
    //   - When the user swipes Logs back, Logs slides out to the right
    //     revealing the still-rendered Other below. After onDismissed
    //     fires we re-mount Other as the new active top, but it was
    //     already at its final on-screen position so there's no second
    //     slide-in animation (see `animateIn` below).
    //
    // `lastAction` tells the active RoundedSlideOverlay whether the
    // user got here via PUSH (animate in from the right) or POP (it
    // was already on screen as an underlay — no entry animation).
    var overlayStack by remember { mutableStateOf<List<AddOverlay>>(emptyList()) }
    var lastAction by remember { mutableStateOf("init") }

    // Camera opens as a simple full-screen slide-up from the bottom.
    // Progress 0 = off-screen below, 1 = settled.
    //
    // Live-preview contract (Telegram-style — picture stays during motion):
    //  1. ONE cancellable Job owns the slide (mid-open close cancels cleanly).
    //  2. `cameraReady` is true for the WHOLE visible lifetime — including the
    //     slide-out. PreviewView uses TextureView (COMPATIBLE), which correctly
    //     follows parent translationY, so the live feed rides the sheet instead
    //     of being torn down → black flash → then animating.
    //  3. Surface is released only AFTER the sheet is off-screen (cameraVisible
    //     flips false → composition disposed → unbind). User never sees the drop.
    //  4. ProcessCameraProvider is prewarmed at app start so bind is cheap.
    val cameraScope = rememberCoroutineScope()
    val appContext = LocalContext.current
    var cameraVisible by remember { mutableStateOf(false) }
    val cameraProgress = remember { Animatable(0f) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraAnimJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(Unit) { CameraPrewarm.warm(appContext) }
    val openCamera: () -> Unit = {
        CameraPrewarm.warm(appContext)
        cameraAnimJob?.cancel()
        cameraVisible = true
        // Allow bind immediately — TextureView rides the slide-up with the feed.
        cameraReady = true
        cameraAnimJob = cameraScope.launch {
            cameraProgress.animateTo(
                1f,
                animationSpec = tween(CameraSlideInMs, easing = CameraSlideEasing),
            )
        }
    }
    val closeCamera: () -> Unit = {
        cameraAnimJob?.cancel()
        // Keep cameraReady=true so the last live frame stays on screen while
        // the sheet slides down. Release only after it's gone.
        cameraAnimJob = cameraScope.launch {
            cameraProgress.animateTo(
                0f,
                animationSpec = tween(CameraSlideOutMs, easing = CameraSlideEasing),
            )
            cameraReady = false
            cameraVisible = false
        }
    }
    val overlay: AddOverlay? = overlayStack.lastOrNull()
    val underlay: AddOverlay? = if (overlayStack.size >= 2) overlayStack[overlayStack.size - 2] else null
    val push: (AddOverlay) -> Unit = { o -> overlayStack = overlayStack + o; lastAction = "push" }
    val pop: () -> Unit = { overlayStack = overlayStack.dropLast(1); lastAction = "pop" }
    // Root-level bottom sheet (weight / sleep) opened from inside the
    // Progress-Goals screen. Rendered as a sibling ABOVE the overlay stack
    // rather than pushed onto it — so Progress-Goals stays the active overlay
    // and never re-mounts. That's what keeps its active tab, per-section
    // scroll position and period from snapping back when the user taps
    // "+ вес" / "+ сон" (the jump the user reported). null = no sheet open.
    var rootSheet by remember { mutableStateOf<AddOverlay?>(null) }
    val parallax = rememberParallaxProgress()
    // Sink for nested overlays. When the stack is >= 2 levels deep the
    // active top RoundedSlideOverlay must NOT drive the host parallax,
    // because the underlay (Other under Logs) is rendered as a fully
    // opaque sibling above the home tab. If the new Logs RSO mirrored
    // its dismissProgress=1 starting value into the real parallax, the
    // navbar (whose alpha = parallax) would flash to 1 for one frame
    // every time the user opens a nested screen — that's the wrong
    // entry animation the user reported for Logs from Другое. The
    // dummy sink absorbs the mirror writes so nested entry/exit reads
    // exactly like a normal slide-in over the parent.
    // The top overlay at depth >= 2 drives THIS state instead of the
    // host parallax. We also read it from the underlay Box so the
    // screen underneath (e.g. Other under Logs) slides slightly to
    // the left as the top overlay slides in from the right — the
    // iOS-style parallax the rest of the app already does for the
    // home pager via OverlayHost. Initial value is 1f (no shift) so
    // there's no flicker before the first RSO write.
    // Driver for nested (depth >= 2) push/pop transitions. Re-created on every
    // stack change STARTING AT 1f, so a freshly-pushed nested screen begins
    // fully off-screen to the right instead of momentarily rendering at rest
    // (progress = 0) for one frame before the slide-in kicks off — that single
    // wrong frame was the «мигание при Запланировать» the user kept seeing
    // (the shared sink used to sit at 0 from the previous level). Both the
    // incoming top overlay AND the static parent underneath read THIS instance,
    // so they move in lockstep: the parent now slides too, which makes nested
    // transitions finally obey the chosen «Сдвиг / Наплыв» style instead of the
    // parent always staying frozen (the «переход не тот, что в настройках» bug).
    // Init depends on the action that produced this stack:
    //  - PUSH → start at 1f so the incoming top begins fully off the right edge
    //    and slides in (a freshly-created Animatable rendered at 0 for one frame
    //    was the old «мигание при Запланировать» on OPEN).
    //  - POP  → start at 0f so the screen we popped BACK to (which becomes the new
    //    active top) is centered on its very first frame. Otherwise the recreated
    //    Animatable sat at 1f, the incoming top's graphicsLayer read progress=1
    //    and drew it off-screen-right for one frame BEFORE its LaunchedEffect
    //    snapTo(0) ran — exposing the underlay (the витрина) underneath = the
    //    «мигание витрины при закрытии Запланировать» the user still saw.
    val nestedParallax = remember(overlayStack) {
        Animatable(if (lastAction == "pop") 0f else 1f)
    }

    // Keeps each overlay's *saveable* UI state (e.g. the витрина's vertical
    // scroll offset) alive when the SAME screen moves between the active-top
    // slot and the static underlay slot. Without it, tapping a list row pushed
    // a child, which re-mounted the витрина in the underlay slot with a fresh
    // scroll state → it snapped back to the top («резко перематывается вверх»,
    // the bug previously fixed for Progress-Goals).
    val overlayStateHolder = rememberSaveableStateHolder()

    // Frosted-glass source — captures the tab content drawn under
    // the navbar so the bar can render a real RenderEffect-blurred
    // snapshot through it. The source is on a SIBLING wrapper, not
    // on the root that also holds the navbar, so the navbar's own
    // pixels don't end up in the source (avoids the empty-navbar
    // feedback we saw in r33).
    val hazeState = remember { HazeState() }

    // SaveableStateHolder for the tab pager (see SaveableStateProvider below).
    val tabStateHolder = rememberSaveableStateHolder()

    // True while a tab-switch slide is in flight. The navbar uses this to drop
    // its (expensive, per-frame) frosted-glass blur DURING the slide and restore
    // it once the screen has settled. Rationale: on a tab switch the content
    // slides UNDER the stationary navbar, so haze re-samples the blur every frame;
    // during the slow ease-out tail (sub-pixel/frame) that re-sampling reads as a
    // shimmer = the "подрагивание при остановке" the user felt. While moving we
    // show a flat tint of the SAME colour (no blur, no re-sampling, no shimmer),
    // then crossfade the real blur back in ~1 frame AFTER motion fully stops — so
    // the swap itself never happens while the eye is tracking motion. We add a
    // small buffer past TabPushMs so the blur returns strictly after the last
    // moving frame, never one frame early (which would re-introduce a hitch).
    // Driven by CachedTabPager.onSettledChange: false the instant a tab-switch
    // slide starts, true exactly when it settles (no fixed delay guesswork).
    var tabSettled by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        OverlayHost(parallaxProgress = parallax) {
            // Haze SOURCE = tab content only. Both this source AND the navbar
            // below now live INSIDE OverlayHost, so they translate together as
            // ONE canvas during a push: the navbar's blurred backdrop no longer
            // "dances" under content sliding independently (that was the jank),
            // and the bar slides away with the screen like one polotno. Overlays
            // are still drawn AFTER OverlayHost (below) so they cover it.
            Box(Modifier.fillMaxSize().haze(hazeState)) {
                CachedTabPager(
                    current = state.currentTab,
                    order = state.navbarOrder,
                    onSettledChange = { tabSettled = it },
                    modifier = Modifier.fillMaxSize(),
                ) { tab ->
                    // Wrap each tab in a SaveableStateProvider keyed by the tab.
                    // CachedTabPager keeps every visited tab COMPOSED (parked
                    // off-screen), so there's no dispose+rebuild on switch; this
                    // provider just adds rememberSaveable survival across process
                    // death / config changes (scroll offsets, expanded sections…).
                    tabStateHolder.SaveableStateProvider(tab.name) {
                        when (tab) {
                            Tab.Home -> HomeScreen(
                                onAddWeight = { push(AddOverlay.Weight) },
                                onAddMeal = { push(AddOverlay.Nutrition) },
                                onAddSleep = { push(AddOverlay.Sleep) },
                            )
                            Tab.Nutrition -> NutritionScreen(
                                onAddMeal = { push(AddOverlay.Nutrition) },
                                onWaterHistory = { push(AddOverlay.WaterHistory) },
                            )
                            Tab.Plan -> PlanScreen(
                                onAddHabit = { push(AddOverlay.Habit()) },
                                onAddTask = { push(AddOverlay.Task()) },
                                onEditHabit = { id -> push(AddOverlay.Habit(id)) },
                                onEditTask = { id -> push(AddOverlay.Task(id)) },
                            )
                            Tab.Profile -> ProfileScreen(
                                onEditProfile = { push(AddOverlay.EditProfile) },
                                onGoals = { push(AddOverlay.Goals) },
                                onAppearance = { push(AddOverlay.Appearance) },
                                onNotifications = { push(AddOverlay.Notifications) },
                                onOther = { push(AddOverlay.Other) },
                                onProgressDetail = { push(AddOverlay.ProgressGoals) },
                                onMedia = { push(AddOverlay.Media) },
                                onQuickCamera = { openCamera() },
                                onQuickScan = { push(AddOverlay.Tiwi) },
                                onQuickWeight = { push(AddOverlay.Weight) },
                            )
                        }
                    }
                }
            }

            // Navbar — INSIDE OverlayHost so it rides the same canvas. Drawn
            // after the tab content (so it sits above it) but before the
            // overlays (added below as later siblings) so any open screen
            // covers it. It now slides away WITH the tab content during a
            // push, and its blur source moves in lockstep with it so the
            // frosted backdrop stays calm (no per-frame "dancing" jank).
            // When a long-press "peek" menu is open on the Plan screen, the
            // bottom navbar must NOT carry its own blur pass (its frosted-glass
            // haze read as a distinct, self-blurred panel) and must NOT vanish
            // (its icons would just disappear). Instead the bar stays fully in
            // place with its icons, drops its frosted blur, and is darkened by
            // the SAME theme-coloured dim as the rest of the screen — one single
            // uniform dim that also covers the navbar.
            Navbar(
                current = state.currentTab,
                onSelect = { state.currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
                // Lambda (not a plain Bool) so reading the flag doesn't
                // recompose the whole navbar subtree — only the backdrop
                // Crossfade observes it. Also drop the frosted blur while a
                // peek is open so the bar can't "blur within itself".
                tabAnimating = { !tabSettled || state.peekActive },
                // The SAME dim driver as the background scrim, read at draw
                // time: the Plan screen publishes its peek-transition value to
                // state.peekDim every frame, so the bar and the background dim
                // and undim in perfect lockstep (no lag, especially on close).
                peekDim = { state.peekDim },
            )
        }

        // Underlay — only the second-from-top overlay, rendered
        // statically full-screen behind the active one (no slide, no
        // swipe-back, no-op onBack). Keeps the parent visible during
        // child push/pop so the home tab never flashes through.
        underlay?.let { u ->
            if (u != AddOverlay.Weight) {
                // Nested push now behaves EXACTLY like a top-level push: the parent
                // (underlay) slides left in lockstep with the incoming child, driven
                // by the SAME `nestedParallax` Animatable the active top reads —
                // identical to how OverlayHost shifts the home content at depth 1.
                // This restores the shared-axis «Сдвиг»/«Наплыв» look the user wanted
                // («переход должен быть как на всех других экранах»). The one-frame
                // витрина flash that previously came with a sliding underlay was NOT
                // caused by the slide itself but by `nestedParallax` re-initialising
                // at 1f on a POP (see above) — fixed there, so we can slide safely.
                val style = state.transitionStyle
                val shiftFraction = overlayHostShiftFraction(style)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = nestedParallax.value.coerceIn(0f, 1f)
                            translationX = -(1f - p) * size.width * shiftFraction
                        }
                        .background(Letify.colors.bg)
                ) {
                    overlayStateHolder.SaveableStateProvider(u.stateKey()) {
                        OverlayContent(
                            current = u,
                            animatedBack = {},
                            onPushLogs = {},
                        )
                    }
                    // Cover style («Наплыв») dims the receding parent, matching the
                    // top-level OverlayHost dim so nested transitions look the same.
                    if (style == TransitionStyle.Cover) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = (1f - nestedParallax.value.coerceIn(0f, 1f)) * 0.16f
                                }
                                .background(Color.Black)
                        )
                    }
                }
            }
        }

        // Multi-field forms get the full-screen "slide in from the right"
        // overlay. Weight is a BottomSheet (rendered outside this branch).
        overlay?.let { current ->
            if (current == AddOverlay.Weight) {
                AddWeightScreen(onBack = { pop() })
            } else {
                // Only animate-in for PUSH actions. If we got here via
                // POP (the user dismissed a child overlay), this view
                // was already on screen as an underlay — re-mounting it
                // and re-running the slide-in would look like the new
                // top "appears" from the right one extra time.
                val animateInTop = lastAction != "pop"
                val topParallax = if (overlayStack.size >= 2) nestedParallax else parallax
                key(current, animateInTop) {
                    RoundedSlideOverlay(
                        parallaxProgress = topParallax,
                        onDismissed = { pop() },
                        animateIn = animateInTop,
                    ) { animatedBack ->
                        overlayStateHolder.SaveableStateProvider(current.stateKey()) {
                            OverlayContent(
                                current = current,
                                animatedBack = animatedBack,
                                onPushLogs = { push(AddOverlay.Logs) },
                                onPushBindings = { push(AddOverlay.Bindings) },
                                // Weight / sleep adders open as a ROOT bottom sheet
                                // (rootSheet) instead of being pushed — keeps the
                                // Progress-Goals screen mounted underneath.
                                onPushWeight = { rootSheet = AddOverlay.Weight },
                                onPushSleep = { rootSheet = AddOverlay.Sleep },
                                onOpenCameraExpand = { openCamera() },
                            )
                        }
                    }
                }
            }
        }
        // Root-level bottom sheets (weight / sleep). Rendered LAST so they
        // sit on top of every overlay and the navbar. Driven by `rootSheet`
        // rather than the overlay stack, so the screen that opened them
        // (Progress-Goals) stays mounted and keeps its scroll/tab/period.
        when (rootSheet) {
            AddOverlay.Weight -> AddWeightScreen(onBack = { rootSheet = null })
            AddOverlay.Sleep -> AddSleepScreen(onBack = { rootSheet = null })
            else -> {}
        }

        // Camera full-screen slide-up. Keyed ONLY on `cameraVisible` so the
        // Animatable progress is read inside graphicsLayer (draw phase) and
        // does NOT recompose CameraCaptureScreen / AndroidView every frame —
        // that recomposition was what made the live TextureView flicker and
        // the picture "snap" away during open/close.
        // Close keeps cameraVisible=true until the slide finishes, so the
        // feed rides translationY the whole way down.
        if (cameraVisible) {
            Box(Modifier.fillMaxSize().zIndex(50f)) {
                // Soft scrim that fades with the slide (draw-time read).
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = cameraProgress.value.coerceIn(0f, 1f) * 0.45f
                        }
                        .background(Color.Black),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = cameraProgress.value.coerceIn(0f, 1f)
                            translationY = (1f - p) * size.height
                        }
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                ) {
                    CameraCaptureScreen(
                        onBack = closeCamera,
                        onCaptured = {},
                        readyToBind = cameraReady,
                    )
                }
            }
        }

        // Crash reports are written to disk by CrashReporter on uncaught
        // exception and surfaced passively via Profile → Другое → Логи.
        // The previous launch-time dialog interrupted the cold-start flow
        // after every crash and looked alarming — the new screen lets the
        // user copy logs on demand without blocking the UI.
    }
}

/**
 * Renders the body of a single overlay level. Extracted so both the
 * static underlay (no animation, no back) and the interactive top
 * (slide+swipe via RoundedSlideOverlay) share one switch and stay in
 * sync when new overlay types are added.
 */
@Composable
private fun OverlayContent(
    current: AddOverlay,
    animatedBack: () -> Unit,
    onPushLogs: () -> Unit,
    onPushWeight: () -> Unit = {},
    onPushSleep: () -> Unit = {},
    onPushBindings: () -> Unit = {},
    onOpenCameraExpand: () -> Unit = {},
) {
    when (current) {
        is AddOverlay.Habit -> AddHabitScreen(onBack = animatedBack, editId = current.editId)
        is AddOverlay.Task -> AddTaskScreen(onBack = animatedBack, editId = current.editId)
        AddOverlay.Nutrition -> AddNutritionScreen(onBack = animatedBack)
        AddOverlay.Sleep -> AddSleepScreen(onBack = animatedBack)
        AddOverlay.Weight -> {} // weight is a bottom-sheet, handled elsewhere
        AddOverlay.EditProfile -> EditProfileScreen(onBack = animatedBack)
        AddOverlay.Goals -> GoalsScreen(onBack = animatedBack)
        AddOverlay.Appearance -> AppearanceScreen(onBack = animatedBack)
        AddOverlay.Notifications -> NotificationsScreen(onBack = animatedBack)
        AddOverlay.Bindings -> BindingsScreen(onBack = animatedBack)
        AddOverlay.Tiwi -> TiwiPlaceholder(onBack = animatedBack)
        AddOverlay.Other -> OtherScreen(
            onBack = animatedBack,
            onLogs = onPushLogs,
            onBindings = onPushBindings,
        )
        AddOverlay.Logs -> LogsScreen(onBack = animatedBack)
        AddOverlay.WaterHistory -> WaterHistoryScreen(onBack = animatedBack)
        AddOverlay.ProgressGoals -> ProgressGoalsScreen(
            onBack = animatedBack,
            onAddWeight = onPushWeight,
            onAddSleep = onPushSleep,
        )
        AddOverlay.Media -> MediaScreen(
            onBack = animatedBack,
            onOpenCamera = onOpenCameraExpand,
        )
    }
}

@Composable
private fun TiwiPlaceholder(onBack: () -> Unit) {
    // The user explicitly asked that "Тифи" not open any real content for
    // now — just a polite stub with a back arrow so the entry still feels
    // wired up.
    Box(
        Modifier
            .fillMaxSize()
            .background(Letify.colors.bg),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            com.letify.app.ui.components.SettingsHeader(title = "Letify", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SolarIcon(
                        name = "smile-circle-outline",
                        tint = Letify.colors.muted.copy(alpha = 0.6f),
                        size = 64.dp,
                    )
                    Box(Modifier.size(14.dp))
                    Text(
                        "Скоро",
                        color = Letify.colors.text,
                        style = Letify.typography.headlineMedium,
                    )
                    Box(Modifier.size(6.dp))
                    Text(
                        "Этот раздел пока в работе",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/**
 * Tab pager that CACHES screens instead of disposing them.
 *
 * The old `AnimatedContent` tore the outgoing tab's whole composition down and
 * rebuilt the incoming one from scratch on every switch. For the heavy Plan
 * screen (a non-lazy task list) that rebuild dropped frames every time you
 * tapped it in the navbar — the "очень сильно лагает открытие экрана задач" lag,
 * and the janky tab-slide (the incoming screen was being composed mid-animation).
 *
 * Here every tab that has ever been shown stays in the composition, parked
 * off-screen with `alpha = 0` (so it draws nothing while idle). Switching just
 * slides the two involved tabs across — no compose work, no rebuild — so
 * re-entry is instant and the slide runs at full frame-rate. Only a direct
 * two-screen push is animated (from → to), exactly like before, regardless of
 * how far apart the tabs sit in the navbar.
 */
@Composable
private fun CachedTabPager(
    current: Tab,
    order: List<Tab>,
    onSettledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Tab) -> Unit,
) {
    val visited = remember { mutableStateListOf<Tab>() }
    if (current !in visited) visited.add(current)

    var fromTab by remember { mutableStateOf(current) }
    var toTab by remember { mutableStateOf(current) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(current) {
        if (current != toTab) {
            fromTab = toTab
            toTab = current
            onSettledChange(false)
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(TabPushMs, easing = TabPushEasing))
            onSettledChange(true)
            // Collapse: the screen we slid away from is now just another parked
            // (alpha-0) cached tab.
            fromTab = current
        }
    }

    val forward =
        order.indexOf(toTab).coerceAtLeast(0) >= order.indexOf(fromTab).coerceAtLeast(0)
    val dir = if (forward) 1f else -1f

    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val p = progress.value
        visited.forEach { tab ->
            val parked = tab != toTab && tab != fromTab
            val dx = when (tab) {
                toTab -> (1f - p) * dir * w
                fromTab -> -p * dir * w
                else -> w
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(if (tab == toTab) 1f else 0f)
                    .graphicsLayer {
                        translationX = dx
                        // Parked tabs draw nothing (alpha 0 => layer composite is
                        // skipped) yet stay composed, so returning to them is free.
                        alpha = if (parked) 0f else 1f
                        // Clip each page to its own bounds so overflowing content
                        // (e.g. a Plan circle's label scrolled half off-screen)
                        // can't bleed over the neighbouring tab during the slide.
                        clip = true
                    },
            ) {
                content(tab)
            }
        }
    }
}
