package com.letify.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.letify.app.ui.components.AccentSwitch
import com.letify.app.ui.components.ColorPickerGrid
import com.letify.app.ui.components.HeaderCheckButton
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
import com.letify.app.ui.components.ScreenHorizontalPadding
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.Subtask
import com.letify.app.ui.state.TaskItem
import com.letify.app.ui.state.scheduleTextFor
import com.letify.app.ui.theme.AccentPalette
import com.letify.app.ui.theme.Manrope
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

// ───────────────────────────────────────────────────────────────────────────
// "Create task" — redesigned to mirror AddHabitScreen's settings-list flow.
//
// Instead of the old 3-step wizard, the screen is now a single scrollable
// list of parameters with a LIVE TASK-CARD preview at the top (the exact
// card the task shows on the Plan screen), inline floating popovers for the
// icon / colour pickers, and slide-in sub-screens for:
//   • Время              — start + end via iOS-style wheel drums
//   • Напоминание и дни   — day grid + push toggle + minutes-before chips
// ───────────────────────────────────────────────────────────────────────────

private enum class TaskRoute { Root, Time, Reminder }

private data class TaskDraft(
    val name: String = "",
    val icon: String = "dumbbell-bold-duotone",
    val color: Color = AccentPalette[2],
    val startH: Int = 8,
    val startM: Int = 0,
    val endH: Int = 9,
    val endM: Int = 0,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val remind: Boolean = true,
    val remindMinutesBefore: Int = 10,
    val subtasks: List<Subtask> = emptyList(),
) {
    val startMin: Int get() = startH * 60 + startM
    val endMin: Int get() = endH * 60 + endM
    // A task may run past midnight (e.g. 23:00 → 05:00). The only invalid case
    // is start == end (zero-length). end < start simply means it wraps to the
    // next day.
    val timeValid: Boolean get() = endMin != startMin

    fun startLabel(): String = "%02d:%02d".format(startH, startM)
    fun endLabel(): String = "%02d:%02d".format(endH, endM)
    fun timeRangeLabel(): String = "${startLabel()} – ${endLabel()}"
    fun durationLabel(): String =
        formatTaskDuration(if (endMin > startMin) endMin - startMin else endMin - startMin + 24 * 60)

    fun reminderLabel(): String {
        val sched = scheduleTextFor(days)
        val tail = when {
            !remind -> "без пуша"
            remindMinutesBefore == 0 -> "вовремя"
            else -> "за $remindMinutesBefore мин"
        }
        return "$sched · $tail"
    }
}

private fun formatTaskDuration(total: Int): String = when {
    total <= 0 -> "—"
    total < 60 -> "${total}м"
    total % 60 == 0 -> "${total / 60}ч"
    else -> "${total / 60}ч ${total % 60}м"
}

@Composable
fun AddTaskScreen(onBack: () -> Unit, editId: Int? = null) {
    val state = LocalAppState.current
    // When editing, seed the draft from the existing task so every field is
    // pre-filled; otherwise start from the blank create draft.
    val editing = remember(editId) { editId?.let { id -> state.tasks.firstOrNull { it.id == id } } }
    var route by remember { mutableStateOf(TaskRoute.Root) }
    var draft by remember {
        mutableStateOf(
            editing?.let { t ->
                TaskDraft(
                    name = t.name,
                    icon = t.icon,
                    color = t.color,
                    startH = t.startMinutes / 60,
                    startM = t.startMinutes % 60,
                    endH = t.endMinutes / 60,
                    endM = t.endMinutes % 60,
                    days = t.days,
                    remind = t.remind,
                    remindMinutesBefore = t.remindMinutesBefore,
                    subtasks = t.subtasks,
                )
            } ?: TaskDraft()
        )
    }

    val parallax = rememberParallaxProgress()
    Box(Modifier.fillMaxSize()) {
        OverlayHost(parallaxProgress = parallax) {
            TaskRootScreen(
                draft = draft,
                onDraft = { draft = it },
                onBack = onBack,
                isEditing = editing != null,
                onTime = { route = TaskRoute.Time },
                onReminder = { route = TaskRoute.Reminder },
                onCreate = {
                    val built = TaskItem(
                        id = editing?.id ?: 0,
                        name = draft.name.trim(),
                        icon = draft.icon,
                        color = draft.color,
                        startMinutes = draft.startMin,
                        endMinutes = draft.endMin,
                        days = draft.days,
                        remind = draft.remind,
                        remindMinutesBefore = draft.remindMinutesBefore,
                        subtasks = draft.subtasks,
                    )
                    if (editing != null) state.updateTask(built) else state.addTask(built)
                    onBack()
                },
            )
        }
        if (route != TaskRoute.Root) {
            key(route) {
                RoundedSlideOverlay(
                    parallaxProgress = parallax,
                    onDismissed = { route = TaskRoute.Root },
                ) { animatedBack ->
                    when (route) {
                        TaskRoute.Time -> TaskTimeSubScreen(draft, { draft = it }, animatedBack)
                        TaskRoute.Reminder -> TaskReminderSubScreen(draft, { draft = it }, animatedBack)
                        TaskRoute.Root -> Unit
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// ROOT
// ───────────────────────────────────────────────────────────────────────────

private enum class TaskInlinePanel { None, Icon, Color }

@Composable
private fun TaskRootScreen(
    draft: TaskDraft,
    onDraft: (TaskDraft) -> Unit,
    onBack: () -> Unit,
    isEditing: Boolean = false,
    onTime: () -> Unit,
    onReminder: () -> Unit,
    onCreate: () -> Unit,
) {
    val scroll = rememberScrollState()
    var panel by remember { mutableStateOf(TaskInlinePanel.None) }
    var rowWidthPx by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        snapshotFlow { scroll.value }.collect {
            if (panel != TaskInlinePanel.None) panel = TaskInlinePanel.None
        }
    }

    val canCreate = draft.name.trim().isNotEmpty() && draft.timeValid

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
                title = if (isEditing) "Изменить задачу" else "Новая задача",
                onBack = onBack,
                trailing = { HeaderCheckButton(enabled = canCreate, onClick = onCreate) },
            )

            // Hero — the live task card, identical to the one shown on the
            // Plan schedule, so the user previews the exact artwork they're
            // configuring (icon bullet, time, name, duration, schedule).
            // It also OWNS subtask editing now: an expand chevron reveals an
            // inline checklist editor and the card morphs into the «с
            // подпунктами» variant (icon → progress ring) as items appear.
            TaskHeroCard(draft, onDraft)

            // Name input — slim Telegram-style container.
            Box(Modifier.screenHPad().padding(top = 14.dp)) {
                TaskNameField(
                    name = draft.name,
                    onChange = { onDraft(draft.copy(name = it)) },
                )
            }

            TaskSectionLabel("ПАРАМЕТРЫ", topPad = 22.dp)
            SettingsCard(
                modifier = Modifier
                    .screenHPad()
                    .onGloballyPositioned { rowWidthPx = it.size.width },
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "clock-circle-bold-duotone",
                    iconTile = LetifyColors.TileBlue,
                    title = "Время",
                    value = draft.timeRangeLabel(),
                    onClick = onTime,
                )
                SettingsRowDivider()

                // ── Иконка: floating popover ────────────────────────────
                Box {
                    SettingsRow(
                        icon = "stars-bold-duotone",
                        iconTile = LetifyColors.TilePink,
                        title = "Иконка",
                        showChevron = false,
                        trailing = { TaskMiniIconPreview(icon = draft.icon, color = draft.color) },
                        onClick = {
                            panel = if (panel == TaskInlinePanel.Icon) TaskInlinePanel.None else TaskInlinePanel.Icon
                        },
                    )
                    TaskAnchoredPopover(
                        visible = panel == TaskInlinePanel.Icon,
                        widthPx = rowWidthPx,
                        onDismiss = { panel = TaskInlinePanel.None },
                    ) {
                        TaskIconGrid(
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
                            panel = if (panel == TaskInlinePanel.Color) TaskInlinePanel.None else TaskInlinePanel.Color
                        },
                    )
                    TaskAnchoredPopover(
                        visible = panel == TaskInlinePanel.Color,
                        widthPx = rowWidthPx,
                        onDismiss = { panel = TaskInlinePanel.None },
                    ) {
                        ColorPickerGrid(
                            colors = AccentPalette,
                            selected = draft.color,
                            onSelect = { onDraft(draft.copy(color = it)) },
                        )
                    }
                }
                SettingsRowDivider()

                // Days + reminder live together on their own sub-screen.
                SettingsRow(
                    icon = "bell-bold-duotone",
                    iconTile = LetifyColors.TileOrange,
                    title = "Напоминания",
                    value = draft.reminderLabel(),
                    onClick = onReminder,
                )
                // NB: «Подзадачи» is no longer a row here — subtasks are edited
                // inline inside the hero card above (expand chevron).
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// HERO — live task card preview (mirrors PlanScreen.TaskCard "upcoming")
// ───────────────────────────────────────────────────────────────────────────

@Composable
private fun TaskHeroCard(draft: TaskDraft, onDraft: (TaskDraft) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val hasSub = draft.subtasks.isNotEmpty()
    val total = draft.subtasks.size
    val chevRot by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(280),
        label = "heroChevron",
    )

    Box(Modifier.screenHPad().padding(top = 16.dp, bottom = 2.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Letify.colors.container),
        ) {
            // ── HEAD — tapping anywhere (or the chevron) expands the editor ──
            Row(
                Modifier
                    .fillMaxWidth()
                    .noFeedbackClick { expanded = !expanded }
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left bullet morphs: tinted icon → progress ring once the task
                // carries subtasks (mirrors the «с подпунктами» card on the Plan).
                if (hasSub) {
                    HeroSubtaskRing(done = 0, total = total, color = draft.color)
                    Spacer(Modifier.width(11.dp))
                } else {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        SolarIcon(name = draft.icon, tint = draft.color, size = 26.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        draft.name.ifBlank { "Название задачи" },
                        color = if (draft.name.isBlank()) Letify.colors.muted else Letify.colors.text,
                        style = Letify.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            draft.startLabel(),
                            color = Letify.colors.text,
                            style = Letify.typography.bodySmall,
                        )
                        Spacer(Modifier.width(7.dp))
                        Box(Modifier.size(3.dp).background(Letify.colors.muted.copy(alpha = 0.6f), CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            draft.durationLabel(),
                            color = Letify.colors.muted,
                            style = Letify.typography.bodySmall,
                        )
                        Spacer(Modifier.width(7.dp))
                        Box(Modifier.size(3.dp).background(Letify.colors.muted.copy(alpha = 0.6f), CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (hasSub) "0 из $total" else scheduleTextFor(draft.days),
                            color = Letify.colors.muted,
                            style = Letify.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Custom-drawn down chevron — rotates 180° when open.
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .noFeedbackClick { expanded = !expanded },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.graphicsLayer { rotationZ = chevRot }) {
                        HeroChevron(color = Letify.colors.muted)
                    }
                }
            }

            // ── REVEAL — inline subtask checklist editor ──
            // Pure top-anchored clip reveal: the content is fully laid out from
            // frame 1 and just gets clipped by the growing/shrinking container —
            // so the text appears instantly with the container and vanishes WITH
            // it on close (no separate fade / slide-in-after).
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(280), expandFrom = Alignment.Top),
                exit = shrinkVertically(animationSpec = tween(240), shrinkTowards = Alignment.Top),
            ) {
                Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 3.dp)
                            .height(1.dp)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f)),
                    )
                    Spacer(Modifier.height(4.dp))
                    InlineSubtaskEditor(draft, onDraft)
                }
            }
        }
    }
    Text(
        "Так задача встанет в твоём расписании",
        color = Letify.colors.muted,
        style = Letify.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 24.dp, end = 24.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// Custom down-chevron — drawn as a rounded V so it stays crisp at any size.
@Composable
private fun HeroChevron(color: androidx.compose.ui.graphics.Color) {
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

// 6-dot drag grip (2×3) — a real grip glyph, not a “⋯” menu.
@Composable
private fun HeroDragGrip(color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(18.dp)) {
        val r = 1.45.dp.toPx()
        val cols = listOf(size.width * 0.34f, size.width * 0.66f)
        val rows = listOf(size.height * 0.26f, size.height * 0.50f, size.height * 0.74f)
        for (x in cols) for (y in rows) drawCircle(color, r, Offset(x, y))
    }
}

// 34dp progress ring used inside the hero card (mirrors PlanScreen.SubtaskRing).
@Composable
private fun HeroSubtaskRing(done: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    val target = if (total > 0) done.toFloat() / total else 0f
    val animated by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(420),
        label = "heroSubRing",
    )
    val track = Letify.colors.muted.copy(alpha = 0.18f)
    val cntColor = if (done >= total && total > 0) color else Letify.colors.text
    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(34.dp)) {
            val sw = 3.5.dp.toPx()
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
        Text("$done/$total", color = cntColor, style = Letify.typography.labelSmall, maxLines = 1)
    }
}

// ───────────────────────────────────────────────────────────────────────────
// INLINE SUBTASK EDITOR — Notes-style checklist living inside the hero card.
// One source of truth (local list, may hold blank rows for typing); only the
// non-blank rows are pushed up to the draft so the card count stays clean.
// ───────────────────────────────────────────────────────────────────────────
@Composable
private fun InlineSubtaskEditor(draft: TaskDraft, onDraft: (TaskDraft) -> Unit) {
    val density = LocalDensity.current

    fun nextIdFor(list: List<Subtask>): Int = (list.maxOfOrNull { it.id } ?: 0) + 1
    // Keep EXACTLY one trailing empty input row: collapse any extra trailing
    // empties to one, and append one if the last row has content. So a fresh
    // empty pункт only appears once the previous one is filled (no stacking).
    fun withTrailingEmpty(list: List<Subtask>): List<Subtask> {
        val l = list.toMutableList()
        while (l.size > 1 && l.last().title.isBlank() && l[l.size - 2].title.isBlank()) {
            l.removeAt(l.size - 1)
        }
        if (l.isEmpty() || l.last().title.isNotBlank()) l.add(Subtask(nextIdFor(l), ""))
        return l
    }

    val items = remember { mutableStateOf(withTrailingEmpty(draft.subtasks)) }

    LaunchedEffect(items.value) {
        val clean = items.value.filter { it.title.isNotBlank() }
        if (clean != draft.subtasks) onDraft(draft.copy(subtasks = clean))
    }

    val focusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    fun frFor(id: Int): FocusRequester = focusRequesters.getOrPut(id) { FocusRequester() }
    var pendingFocusId by remember { mutableStateOf<Int?>(null) }

    val rowHeights = remember { mutableStateMapOf<Int, Int>() }
    val defaultRowH = with(density) { 48.dp.toPx() }

    fun setTitle(id: Int, title: String) {
        val updated = items.value.map { if (it.id == id) it.copy(title = title) else it }
        items.value = withTrailingEmpty(updated)
    }
    fun addRowAfter(id: Int, initial: String = "") {
        val idx = items.value.indexOfFirst { it.id == id }
        val nid = nextIdFor(items.value)
        val list = items.value.toMutableList()
        list.add(idx + 1, Subtask(nid, initial))
        items.value = withTrailingEmpty(list)
        pendingFocusId = nid
    }
    fun deleteRow(id: Int) {
        val idx = items.value.indexOfFirst { it.id == id }
        val list = items.value.filter { it.id != id }
        items.value = withTrailingEmpty(list)
        pendingFocusId = items.value.getOrNull((idx - 1).coerceAtLeast(0))?.id
    }
    fun move(from: Int, to: Int) {
        if (from == to) return
        val list = items.value.toMutableList()
        val it = list.removeAt(from)
        list.add(to, it)
        items.value = list
    }

    // Drag-reorder: the list order stays FIXED during the gesture; the dragged
    // row follows the finger (slightly translucent, no shadow), and the rows it
    // passes slide aside to open a gap. The reorder is committed only on drop.
    var dragId by remember { mutableStateOf<Int?>(null) }
    var dragFrom by remember { mutableStateOf(-1) }
    var dragTo by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(pendingFocusId, items.value.size) {
        val fid = pendingFocusId ?: return@LaunchedEffect
        focusRequesters[fid]?.let { runCatching { it.requestFocus() } }
        pendingFocusId = null
    }

    // animateContentSize → the card grows/shrinks SMOOTHLY as you type (text
    // wraps), add a пункт, or delete one — same feel as the open/close reveal.
    // BUT it clips to its bounds, which would cut off a row dragged past the
    // list. No resize happens mid-drag, so disable it while dragging → the
    // lifted row can overflow freely past the container edge again.
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (dragId == null) Modifier.animateContentSize(tween(220)) else Modifier),
    ) {
        val list = items.value
        // Fixed lane height = one text line + the field's vertical padding, so the
        // numbered square and the grip/✕ buttons center on the FIRST text line.
        val laneH = 42.dp
        val draggedH = (dragId?.let { rowHeights[it] } ?: defaultRowH.toInt()).toFloat()
        list.forEachIndexed { index, st ->
            val isDragging = st.id == dragId
            val shiftTarget = when {
                dragId == null || isDragging -> 0f
                dragFrom < dragTo && index in (dragFrom + 1)..dragTo -> -draggedH
                dragFrom > dragTo && index in dragTo until dragFrom -> draggedH
                else -> 0f
            }
            val shift by animateFloatAsState(shiftTarget, tween(180), label = "shift$index")
            Row(
                Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else shift
                        if (isDragging) alpha = 0.6f
                    }
                    .onGloballyPositioned { rowHeights[st.id] = it.size.height }
                    .clip(RoundedCornerShape(14.dp))
                    .padding(start = 4.dp, end = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // Numbered square — centered on the first text line via the lane.
                Box(Modifier.height(laneH), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(27.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Letify.colors.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            color = Letify.colors.accent,
                            style = Letify.typography.labelSmall,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))

                BasicTextField(
                    value = st.title,
                    onValueChange = { new ->
                        if (new.contains('\n')) {
                            val parts = new.split('\n', limit = 2)
                            val head = parts[0]
                            // Enter on an empty row does nothing (no stray empties);
                            // on a filled row it splits into a new focused row.
                            if (head.isNotBlank()) {
                                setTitle(st.id, head)
                                addRowAfter(st.id, parts.getOrElse(1) { "" })
                            } else {
                                setTitle(st.id, head)
                            }
                        } else {
                            setTitle(st.id, new)
                        }
                    },
                    singleLine = false,
                    textStyle = TextStyle(
                        fontFamily = Manrope,
                        fontSize = 16.sp,
                        color = Letify.colors.text,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(Letify.colors.accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .focusRequester(frFor(st.id))
                        .onPreviewKeyEvent { ev ->
                            if (ev.type == KeyEventType.KeyDown &&
                                ev.key == Key.Backspace &&
                                st.title.isEmpty() &&
                                items.value.size > 1
                            ) {
                                deleteRow(st.id); true
                            } else false
                        },
                    decorationBox = { inner ->
                        if (st.title.isEmpty()) {
                            Text(
                                "Пункт списка…",
                                color = Letify.colors.muted,
                                style = TextStyle(
                                    fontFamily = Manrope,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                ),
                            )
                        }
                        inner()
                    },
                )

                // Drag handle (⠿) — long-press then drag to reorder.
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(30.dp)
                        .pointerInput(st.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragId = st.id
                                    dragFrom = items.value.indexOfFirst { it.id == st.id }
                                    dragTo = dragFrom
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    if (dragFrom in items.value.indices &&
                                        dragTo in items.value.indices && dragFrom != dragTo
                                    ) move(dragFrom, dragTo)
                                    dragId = null; dragFrom = -1; dragTo = -1; dragOffset = 0f
                                },
                                onDragCancel = {
                                    dragId = null; dragFrom = -1; dragTo = -1; dragOffset = 0f
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    dragOffset += delta.y
                                    val h = (dragId?.let { rowHeights[it] } ?: defaultRowH.toInt())
                                        .toFloat().coerceAtLeast(1f)
                                    dragTo = (dragFrom + Math.round(dragOffset / h))
                                        .coerceIn(0, items.value.lastIndex)
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    HeroDragGrip(color = Letify.colors.muted.copy(alpha = 0.7f))
                }

                // Delete (X)
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .noFeedbackClick { deleteRow(st.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(
                        name = "close-circle-bold-duotone",
                        tint = Letify.colors.muted,
                        size = 21.dp,
                    )
                }
            }
        }

        // Bottom inset lives INSIDE the animated/clipped area, so the smooth
        // resize reveals at the CARD edge (like open/close) — never at an inner
        // отступ that visually cuts the пункты.
        Spacer(Modifier.height(8.dp))
    }
}

// ───────────────────────────────────────────────────────────────────────────
// TIME SUB-SCREEN — start + end wheel drums
// ───────────────────────────────────────────────────────────────────────────

private val TaskHourValues: List<Int> = (0..23).toList()
private val TaskMinuteValues: List<Int> = (0..59).toList()

@Composable
private fun TaskTimeSubScreen(draft: TaskDraft, onDraft: (TaskDraft) -> Unit, onBack: () -> Unit) {
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
                title = "Время",
                onBack = onBack,
                trailing = { HeaderCheckButton(enabled = draft.timeValid, onClick = onBack) },
            )

            TaskSectionLabel("НАЧАЛО", topPad = 12.dp)
            TaskTimeWheelGroup(
                hour = draft.startH,
                minute = draft.startM,
                onHour = { onDraft(draft.copy(startH = it)) },
                onMinute = { onDraft(draft.copy(startM = it)) },
            )

            TaskSectionLabel("ОКОНЧАНИЕ", topPad = 20.dp)
            TaskTimeWheelGroup(
                hour = draft.endH,
                minute = draft.endM,
                onHour = { onDraft(draft.copy(endH = it)) },
                onMinute = { onDraft(draft.copy(endM = it)) },
            )

            if (!draft.timeValid) {
                Text(
                    "Начало и окончание не должны совпадать",
                    color = LetifyColors.TileRed,
                    style = Letify.typography.bodySmall,
                    modifier = Modifier.padding(start = 24.dp, top = 14.dp, end = 24.dp),
                )
            } else if (draft.endMin < draft.startMin) {
                Text(
                    "Задача переходит на следующий день",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                    modifier = Modifier.padding(start = 24.dp, top = 14.dp, end = 24.dp),
                )
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun TaskTimeWheelGroup(
    hour: Int,
    minute: Int,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
) {
    val wheelItemHeight = 38.dp
    Box(
        Modifier
            .screenHPad()
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Accent-tinted centre-row pill that unifies the HH:MM pair, the
        // same visual the habit goal wheels use.
        Box(
            Modifier
                .height(wheelItemHeight + 4.dp)
                .fillMaxWidth(0.86f)
                .background(Letify.colors.accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            WheelPicker(
                values = TaskHourValues,
                initialIndex = hour.coerceIn(0, 23),
                modifier = Modifier.weight(1f),
                visibleItems = 5,
                itemHeight = wheelItemHeight,
                onSelected = { _, v -> onHour(v) },
                label = { "%02d".format(it) },
            )
            Text(
                ":",
                color = Letify.colors.text,
                style = Letify.typography.displayMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            WheelPicker(
                values = TaskMinuteValues,
                initialIndex = minute.coerceIn(0, 59),
                modifier = Modifier.weight(1f),
                visibleItems = 5,
                itemHeight = wheelItemHeight,
                onSelected = { _, v -> onMinute(v) },
                label = { "%02d".format(it) },
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// REMINDER + DAYS SUB-SCREEN
// ───────────────────────────────────────────────────────────────────────────

private val TaskRemindPresets = listOf(0, 5, 10, 15, 30, 60)

@Composable
private fun TaskReminderSubScreen(draft: TaskDraft, onDraft: (TaskDraft) -> Unit, onBack: () -> Unit) {
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
                title = "Напоминание и дни",
                onBack = onBack,
                trailing = { HeaderCheckButton(enabled = draft.days.isNotEmpty(), onClick = onBack) },
            )

            TaskSectionLabel("ДНИ")
            TaskDayRow(
                selected = draft.days,
                onChange = { onDraft(draft.copy(days = it)) },
            )
            Row(
                Modifier.screenHPad().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskDayPreset("Каждый день", draft.days.size == 7) {
                    onDraft(draft.copy(days = setOf(1, 2, 3, 4, 5, 6, 7)))
                }
                TaskDayPreset("Будни", draft.days == setOf(1, 2, 3, 4, 5)) {
                    onDraft(draft.copy(days = setOf(1, 2, 3, 4, 5)))
                }
                TaskDayPreset("Выходные", draft.days == setOf(6, 7)) {
                    onDraft(draft.copy(days = setOf(6, 7)))
                }
            }

            TaskSectionLabel("УВЕДОМЛЕНИЕ", topPad = 22.dp)
            SettingsCard(
                modifier = Modifier.screenHPad(),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                SettingsRow(
                    icon = "bell-bing-bold-duotone",
                    iconTile = LetifyColors.TileOrange,
                    title = "Напомнить заранее",
                    showChevron = false,
                    trailing = {
                        AccentSwitch(
                            checked = draft.remind,
                            onCheckedChange = { onDraft(draft.copy(remind = it)) },
                        )
                    },
                )
            }

            if (draft.remind) {
                TaskSectionLabel("ЗА СКОЛЬКО ДО НАЧАЛА", topPad = 22.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = ScreenHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TaskRemindPresets.forEach { m ->
                        TaskMinutesChip(
                            label = if (m == 0) "вовремя" else "${m}м",
                            active = m == draft.remindMinutesBefore,
                            onClick = { onDraft(draft.copy(remindMinutesBefore = m)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// SHARED PIECES (file-private — mirror the habit screen's helpers)
// ───────────────────────────────────────────────────────────────────────────

@Composable
private fun TaskSectionLabel(text: String, topPad: Dp = 16.dp) {
    Text(
        text,
        color = Letify.colors.muted,
        style = Letify.typography.labelSmall,
        modifier = Modifier.padding(start = 28.dp, top = topPad, bottom = 8.dp),
    )
}

@Composable
private fun TaskNameField(
    name: String,
    onChange: (String) -> Unit,
    placeholder: String = "Название задачи",
) {
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
            cursorBrush = SolidColor(Letify.colors.accent),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            decorationBox = { inner ->
                if (name.isEmpty()) {
                    Text(
                        placeholder,
                        color = Letify.colors.muted,
                        style = TextStyle(fontFamily = Manrope, fontSize = 17.sp),
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun TaskMiniIconPreview(icon: String, color: Color) {
    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        SolarIcon(name = icon, tint = color, size = 28.dp)
    }
}

@Composable
private fun TaskMinutesChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(40.dp)
            .background(
                if (active) Letify.colors.accentSoft else Letify.colors.track,
                RoundedCornerShape(999.dp),
            )
            .noFeedbackClick { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (active) Letify.colors.accent else Letify.colors.text,
            style = Letify.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun TaskDayRow(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Row(
        Modifier.screenHPad().fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val iso = i + 1
            val active = iso in selected
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
private fun RowScope.TaskDayPreset(label: String, active: Boolean, onClick: () -> Unit) {
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

// ── Floating popover anchored under a row (icon / colour pickers) ───────────
@Composable
private fun TaskAnchoredPopover(
    visible: Boolean,
    widthPx: Int,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (widthPx <= 0) return
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }

    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    val mounted = transition.currentState || transition.targetState
    if (!mounted) return

    val gapPx = with(density) { 6.dp.roundToPx() }
    val provider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = IntOffset(anchorBounds.left, anchorBounds.bottom + gapPx)
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
                    .background(Letify.colors.container, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                content()
            }
        }
    }
}

// ── 6×6 task icon grid (drag-to-select, like the habit picker) ─────────────
@Composable
private fun TaskIconGrid(selected: String, tint: Color, onSelect: (String) -> Unit) {
    val icons = TaskIconCatalog

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
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
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
                            Modifier.weight(1f).height(cellHeight),
                            contentAlignment = Alignment.Center,
                        ) {
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

// 36 task-oriented icons → 6 rows × 6 columns. Sport / fitness lead, then
// mind-body, food, work/study, misc — all present in assets/icons.
private val TaskIconCatalog: List<String> = listOf(
    // row 1 — fitness / movement
    "dumbbell-bold-duotone", "running-bold-duotone", "walking-bold-duotone",
    "bicycling-bold-duotone", "swimming-bold-duotone", "stretching-bold-duotone",
    // row 2 — sport
    "heart-pulse-bold-duotone", "basketball-bold-duotone", "volleyball-bold-duotone",
    "tennis-2-bold-duotone", "skateboard-bold-duotone", "running-round-bold-duotone",
    // row 3 — mind / body / sleep
    "meditation-bold-duotone", "heart-bold-duotone", "bed-bold-duotone",
    "moon-stars-bold-duotone", "sun-bold-duotone", "alarm-bold-duotone",
    // row 4 — food / drink / care
    "cup-hot-bold-duotone", "plate-bold-duotone", "chef-hat-bold-duotone",
    "wineglass-bold-duotone", "leaf-bold-duotone", "bath-bold-duotone",
    // row 5 — work / study
    "book-bookmark-bold-duotone", "notebook-bold-duotone", "pen-bold-duotone",
    "clipboard-check-bold-duotone", "case-bold-duotone", "smartphone-bold-duotone",
    // row 6 — misc / leisure
    "gamepad-bold-duotone", "music-note-2-bold-duotone", "stopwatch-bold-duotone",
    "bonfire-bold-duotone", "star-shine-bold-duotone", "widget-bold-duotone",
)
