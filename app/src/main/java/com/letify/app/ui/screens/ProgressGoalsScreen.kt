package com.letify.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import com.letify.app.ui.components.ElasticOverscroll
import com.letify.app.ui.components.IconButtonRound
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.OverlayHost
import com.letify.app.ui.components.ProgressRing
import com.letify.app.ui.components.RoundedSlideOverlay
import com.letify.app.ui.components.rememberParallaxProgress
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.rememberElasticOverscroll
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.GoalBreakdown
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.SleepEntry
import com.letify.app.ui.state.WeightEntry
import com.letify.app.ui.state.calculateGoalProgress
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Shared helpers ─────────────────────────────────────────────────────

private val SHORT_DOW = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM")

private fun parseDate(dateKey: String): LocalDate? =
    runCatching { LocalDate.parse(dateKey) }.getOrNull()

private fun shortDow(dateKey: String): String =
    parseDate(dateKey)?.let { SHORT_DOW[it.dayOfWeek.value - 1] } ?: ""

private fun formatHm(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}ч" else "${h}ч ${m}м"
}

private fun sleepQualityLabel(q: Int): String =
    listOf("плохо", "так себе", "норм", "отлично").getOrElse(q) { "норм" }

private fun oneDecimalComma(v: Float): String = "%.1f".format(v).replace('.', ',')

// Detail routes. The calm overview is shown when no metric is open (null);
// tapping a metric row opens one of these full detail screens.
private const val WEIGHT = 0
private const val SLEEP = 1
private const val STEPS = 2

// Date windows for the period control, in days.
private enum class Period(val title: String, val days: Long) {
    WEEK("Неделя", 7),
    MONTH("Месяц", 31),
    YEAR("Год", 366),
}

/**
 * "Прогресс целей" — r52, вариант «Спокойный».
 *
 * One calm vertical screen. No swipe, no top tabs. The top of the screen has a
 * single focus — the overall progress ring with a short status line. Below it a
 * quiet list of metrics (Вес / Сон / Шаги): each row shows an icon, the metric
 * name, its current value, a small sparkline and a trend pill. Tapping a row
 * opens that metric's full detail screen (big value + period control + chart +
 * "Записать…" button) — charts are revealed on tap, so the overview itself
 * stays light and easy to read.
 *
 * Weight + Sleep entry are root-level bottom sheets driven by [onAddWeight] /
 * [onAddSleep] — they do NOT push onto the overlay stack, so this screen never
 * re-mounts and the open detail / scroll position / period never snap back.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProgressGoalsScreen(
    onBack: () -> Unit,
    onAddWeight: () -> Unit = {},
    onAddSleep: () -> Unit = {},
) {
    val breakdown = calculateGoalProgress(LocalAppState.current)
    val metrics = rememberMetrics()
    val scope = rememberCoroutineScope()

    // ── Detail screen state — which metric is open (null = none) ──
    var active by remember { mutableStateOf<Int?>(null) }
    // Drives the base list sliding out to the left in lockstep with the detail
    // sliding in from the right — one continuous canvas, like every other screen.
    val detailParallax = rememberParallaxProgress()

    val scroll = remember { ScrollState(0) }
    val elastic = rememberElasticOverscroll(maxVertical = 56.dp, maxHorizontal = 0.dp)
    val topInset =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 6.dp

    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        // ── Base: calm overview list ──
        OverlayHost(parallaxProgress = detailParallax) {
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                Box(Modifier.fillMaxSize().nestedScroll(elastic.connection)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = elastic.verticalOverscroll.floatValue }
                            .verticalScroll(scroll)
                            .padding(top = topInset, bottom = 160.dp),
                    ) {
                        SettingsHeader(title = "Прогресс целей", onBack = onBack)
                        OverviewCalm(
                            breakdown = breakdown,
                            metrics = metrics,
                            onOpen = { if (active == null) active = it },
                        )
                        Box(Modifier.height(24.dp))
                    }
                }
            }
        }

        // ── Metric detail: a real screen that slides in from the right with
        //    the same canvas slide + swipe-back as the rest of the app ──
        val act = active
        if (act != null) {
            val vm = metrics.firstOrNull { it.id == act }
            key(act) {
                RoundedSlideOverlay(
                    parallaxProgress = detailParallax,
                    onDismissed = { active = null },
                ) { animatedBack ->
                    if (vm != null) {
                        MetricDetailScreen(
                            act = act,
                            vm = vm,
                            topInset = topInset,
                            onBack = animatedBack,
                            onAddWeight = onAddWeight,
                            onAddSleep = onAddSleep,
                        )
                    }
                }
            }
        }
    }
}

// ── Metric view-model (shared by the list row and the lifted overlay) ──

private data class MetricVM(
    val id: Int,
    val icon: String,
    val color: Color,
    val name: String,
    val value: String,
    val spark: List<Float>?,
    val trend: Triple<String, Color, Boolean>?,
)

/** Builds the Вес / Сон / Шаги view-models from app state, in fixed order. */
@Composable
private fun rememberMetrics(): List<MetricVM> {
    val state = LocalAppState.current

    val weightSorted = state.weightLog.sortedBy { it.dateKey }
    val curWeight = weightSorted.lastOrNull()?.kg
    val weeklyDelta: Float? = run {
        val last = weightSorted.lastOrNull()
        if (last == null || weightSorted.size < 2) null
        else {
            val weekAgo = parseDate(last.dateKey)?.minusDays(7)
            val ref = weekAgo?.let { wa ->
                weightSorted.lastOrNull { e -> parseDate(e.dateKey)?.let { it <= wa } == true }
            } ?: weightSorted.first()
            last.kg - ref.kg
        }
    }
    val weightVM = MetricVM(
        id = WEIGHT,
        icon = "scale-bold-duotone",
        color = Letify.colors.accent,
        name = "Вес",
        value = curWeight?.let { "${oneDecimalComma(it)} кг" } ?: "—",
        spark = weightSorted.takeLast(7).map { it.kg }.takeIf { it.size >= 2 },
        trend = weeklyDelta?.takeIf { abs(it) >= 0.05f }?.let {
            val loss = it < 0f
            Triple(
                "${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(it))} кг",
                if (loss) LetifyColors.Mint else LetifyColors.Pink,
                true,
            )
        },
    )

    val sleepSorted = state.sleepLog.sortedBy { it.dateKey }
    val lastSleep = sleepSorted.lastOrNull()
    val sleepVM = MetricVM(
        id = SLEEP,
        icon = "moon-sleep-bold-duotone",
        color = LetifyColors.Water,
        name = "Сон",
        value = lastSleep?.let { formatHm(it.durationMinutes) } ?: "—",
        spark = sleepSorted.takeLast(7).map { it.durationMinutes.toFloat() }
            .takeIf { it.size >= 2 && it.any { v -> v > 0f } },
        trend = lastSleep?.let {
            Triple(sleepQualityLabel(it.quality), LetifyColors.Water, false)
        },
    )

    val stepsVM = MetricVM(
        id = STEPS,
        icon = "walking-bold-duotone",
        color = LetifyColors.Mint,
        name = "Шаги",
        value = "—",
        spark = null,
        trend = Triple("скоро", Letify.colors.muted, false),
    )

    return listOf(weightVM, sleepVM, stepsVM)
}

// ── Calm overview ──────────────────────────────────────────────────────

@Composable
private fun OverviewCalm(
    breakdown: GoalBreakdown,
    metrics: List<MetricVM>,
    onOpen: (Int) -> Unit,
) {
    val state = LocalAppState.current
    val percent = (breakdown.overall * 100f).toInt()

    val weightSorted = state.weightLog.sortedBy { it.dateKey }
    val weeklyDelta: Float? = run {
        val last = weightSorted.lastOrNull()
        if (last == null || weightSorted.size < 2) null
        else {
            val weekAgo = parseDate(last.dateKey)?.minusDays(7)
            val ref = weekAgo?.let { wa ->
                weightSorted.lastOrNull { e -> parseDate(e.dateKey)?.let { it <= wa } == true }
            } ?: weightSorted.first()
            last.kg - ref.kg
        }
    }

    // ── Single focus: overall ring + short status line ──
    Row(
        Modifier.fillMaxWidth().screenHPad().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = breakdown.overall,
                color = Letify.colors.accent,
                size = 92.dp,
                strokeWidth = 9.dp,
                discFillAlpha = 0.10f,
            )
            Text(
                "$percent%",
                color = Letify.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }
        Box(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (percent >= 50) "Идёшь по плану" else "Только начало",
                color = Letify.colors.text,
                style = Letify.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.height(3.dp))
            Text(
                "Общий прогресс целей",
                color = Letify.colors.muted,
                style = Letify.typography.bodyMedium,
            )
            if (weeklyDelta != null && abs(weeklyDelta) >= 0.05f) {
                val loss = weeklyDelta < 0f
                Text(
                    "За неделю ${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(weeklyDelta))} кг",
                    color = if (loss) LetifyColors.Mint else LetifyColors.Pink,
                    style = Letify.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    Box(Modifier.height(20.dp))
    CalmSectionLabel("Мои показатели")
    Box(Modifier.height(12.dp))

    Column(Modifier.screenHPad(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.forEach { vm ->
            MetricRow(
                vm = vm,
                onClick = { onOpen(vm.id) },
            )
        }
    }
}

@Composable
private fun CalmSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Letify.colors.muted,
        style = Letify.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.screenHPad().padding(start = 6.dp),
    )
}

/** One quiet metric row, tappable to open its detail screen. */
@Composable
private fun MetricRow(
    vm: MetricVM,
    onClick: () -> Unit,
) {
    NoFeedbackButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        MetricRowContent(vm)
    }
}

/** The visual content of a metric card — shared by the list row and the overlay. */
@Composable
private fun MetricRowContent(
    vm: MetricVM,
    hideText: Boolean = false,
    onNameBounds: (Rect) -> Unit = {},
    onValueBounds: (Rect) -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Letify.colors.container, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(vm.color.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) { SolarIcon(name = vm.icon, tint = vm.color, size = 24.dp) }
        Box(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                vm.name,
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
                modifier = Modifier
                    .onGloballyPositioned { onNameBounds(it.boundsInRoot()) }
                    .graphicsLayer { alpha = if (hideText) 0f else 1f },
            )
            Box(Modifier.height(2.dp))
            Text(
                vm.value,
                color = Letify.colors.text,
                style = Letify.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .onGloballyPositioned { onValueBounds(it.boundsInRoot()) }
                    .graphicsLayer { alpha = if (hideText) 0f else 1f },
            )
        }
        Box(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (vm.spark != null) {
                Sparkline(data = vm.spark, color = vm.color, width = 82.dp, height = 30.dp)
                Box(Modifier.height(8.dp))
            }
            if (vm.trend != null) {
                CalmTrendPill(text = vm.trend.first, color = vm.trend.second, withArrow = vm.trend.third)
            }
        }
    }
}

/**
 * The metric detail as a normal full screen (Вес / Сон / Шаги).
 *
 * It is hosted by a [RoundedSlideOverlay] in the screen above, so it slides in
 * from the right as one continuous canvas with the list and supports swipe-back
 * — exactly like every other screen in the app. No custom animation here.
 */
@Composable
private fun MetricDetailScreen(
    act: Int,
    vm: MetricVM,
    topInset: Dp,
    onBack: () -> Unit,
    onAddWeight: () -> Unit,
    onAddSleep: () -> Unit,
) {
    val scroll = remember(act) { ScrollState(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Letify.colors.bg),
    ) {
        Box(Modifier.height(topInset))
        SettingsHeader(title = vm.name, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll),
        ) {
            Box(Modifier.height(8.dp))
            when (act) {
                WEIGHT -> WeightSection(onAddWeight = onAddWeight)
                SLEEP -> SleepSection(onAddSleep = onAddSleep)
                else -> StepsSection()
            }
            Box(Modifier.height(140.dp))
        }
    }
}

@Composable
private fun CalmTrendPill(text: String, color: Color, withArrow: Boolean) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = if (withArrow) 9.dp else 10.dp, vertical = 5.dp),
    ) {
        Text(
            text,
            color = color,
            style = Letify.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Tiny smooth sparkline (no axes) ending in a filled dot — mirrors the web
 * prototype's Spark. Draws via the shared [smoothPath] cubic builder.
 */
@Composable
private fun Sparkline(data: List<Float>, color: Color, width: Dp, height: Dp) {
    Canvas(Modifier.size(width, height)) {
        val pad = 4.dp.toPx()
        val min = data.min()
        val max = data.max()
        val range = (max - min).takeIf { it > 0.0001f } ?: 1f
        val w = size.width
        val h = size.height
        val pts = data.mapIndexed { i, v ->
            val x = if (data.size > 1) pad + i * (w - pad * 2) / (data.size - 1) else w / 2f
            val y = pad + (1f - (v - min) / range) * (h - pad * 2)
            Offset(x, y)
        }
        if (pts.size >= 2) {
            drawPath(
                smoothPath(pts),
                color = color,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        val last = pts.last()
        drawCircle(color = color, radius = 3.2.dp.toPx(), center = last)
    }
}


// ── Section: Вес ───────────────────────────────────────────────────────

@Composable
private fun WeightSection(
    onAddWeight: () -> Unit,
    onValueBounds: (Rect) -> Unit = {},
    valueModifier: Modifier = Modifier,
) {
    val state = LocalAppState.current
    var period by remember { mutableStateOf(Period.WEEK) }
    val goal = state.weightGoal

    val all = state.weightLog.sortedBy { it.dateKey }
    val cutoff = LocalDate.now().minusDays(period.days)
    val prevCutoff = cutoff.minusDays(period.days)
    val windowed = all.filter { parseDate(it.dateKey)?.let { d -> !d.isBefore(cutoff) } ?: false }
    val prevWindowed = all.filter {
        parseDate(it.dateKey)?.let { d -> !d.isBefore(prevCutoff) && d.isBefore(cutoff) } ?: false
    }
    val entries = windowed.ifEmpty { all.takeLast(1) }
    val points = entries.map { it.kg }
    val hasData = points.isNotEmpty()
    val current = points.lastOrNull() ?: state.weight
    val delta = if (points.size >= 2) points.last() - points.first() else 0f
    val avg = if (hasData) points.average().toFloat() else 0f
    val remaining = (current - goal).coerceAtLeast(0f)
    // Comparison vs previous period (for the ghost line + pill).
    val compareDelta = if (points.isNotEmpty() && prevWindowed.isNotEmpty()) {
        avg - prevWindowed.map { it.kg }.average().toFloat()
    } else null

    Column(Modifier.screenHPad().padding(start = 4.dp)) {
        Text("Текущий вес", color = Letify.colors.muted, style = Letify.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (hasData) oneDecimalComma(current) else "—",
                color = Letify.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                modifier = valueModifier.onGloballyPositioned { onValueBounds(it.boundsInRoot()) },
            )
            Text(" кг", color = Letify.colors.muted, style = Letify.typography.titleMedium)
            if (points.size >= 2) {
                Box(Modifier.width(10.dp))
                val loss = delta < 0f
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            (if (loss) LetifyColors.Mint else LetifyColors.Pink).copy(alpha = 0.16f),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(delta))} кг",
                        color = if (loss) LetifyColors.Mint else LetifyColors.Pink,
                        style = Letify.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    Box(Modifier.height(16.dp))
    PeriodControl(period = period, onSelect = { period = it })
    Box(Modifier.height(16.dp))

    if (hasData) {
        SwipeLineChart(
            entries = entries,
            prevEntries = prevWindowed,
            goalKg = goal,
            compareDelta = compareDelta,
            period = period,
        )
    } else {
        EmptyState("Запишите вес, чтобы увидеть динамику")
    }

    Box(Modifier.height(18.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Среднее", if (hasData) "${oneDecimalComma(avg)} кг" else "—")
        StatTile(Modifier.weight(1f), "Цель", "${oneDecimalComma(goal)} кг")
        StatTile(Modifier.weight(1f), "Осталось", "${oneDecimalComma(remaining)} кг")
    }

    Box(Modifier.height(16.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Letify.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Последние записи",
                color = Letify.colors.text,
                style = Letify.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddWeight)
        }
        if (all.isEmpty()) {
            Box(Modifier.height(8.dp))
            Text("Пока нет записей", color = Letify.colors.muted, style = Letify.typography.bodySmall)
        } else {
            val recent = all.reversed().take(6)
            recent.forEachIndexed { i, e ->
                if (i > 0) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Letify.colors.track))
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        parseDate(e.dateKey)?.format(DAY_MONTH) ?: e.dateKey,
                        color = Letify.colors.muted,
                        style = Letify.typography.bodyMedium,
                    )
                    Text(
                        "${oneDecimalComma(e.kg)} кг",
                        color = Letify.colors.text,
                        style = Letify.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ── Section: Сон ───────────────────────────────────────────────────────

@Composable
private fun SleepSection(
    onAddSleep: () -> Unit,
    onValueBounds: (Rect) -> Unit = {},
    valueModifier: Modifier = Modifier,
) {
    val state = LocalAppState.current
    var period by remember { mutableStateOf(Period.WEEK) }

    val all = state.sleepLog.sortedBy { it.dateKey }
    val cutoff = LocalDate.now().minusDays(period.days)
    val windowed = all.filter { parseDate(it.dateKey)?.let { d -> !d.isBefore(cutoff) } ?: false }
    val entries = windowed.ifEmpty { all.takeLast(1) }
    val hasData = entries.isNotEmpty() && entries.any { it.durationMinutes > 0 }
    val last = all.lastOrNull()
    val avgMin = if (hasData) entries.map { it.durationMinutes }.average().toInt() else 0
    val bestMin = entries.maxOfOrNull { it.durationMinutes } ?: 0
    val goalMin = state.sleepGoalMinutes
    val goalText = if (goalMin % 60 == 0) "${goalMin / 60} ч" else formatHm(goalMin)

    Column(Modifier.screenHPad().padding(start = 4.dp)) {
        Text("Прошлой ночью", color = Letify.colors.muted, style = Letify.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                last?.let { formatHm(it.durationMinutes) } ?: "—",
                color = Letify.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                modifier = valueModifier.onGloballyPositioned { onValueBounds(it.boundsInRoot()) },
            )
            if (last != null) {
                Box(Modifier.width(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LetifyColors.Water.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        sleepQualityLabel(last.quality),
                        color = LetifyColors.Water,
                        style = Letify.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    Box(Modifier.height(16.dp))
    PeriodControl(period = period, onSelect = { period = it })
    Box(Modifier.height(16.dp))

    if (hasData) {
        Row(
            Modifier.screenHPad().fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Длительность сна", color = Letify.colors.muted, style = Letify.typography.bodySmall)
            Text("цель $goalText", color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
        Box(Modifier.height(10.dp))
        SleepBars(entries = entries, goalMin = goalMin)
    } else {
        EmptyState("Запишите сон, чтобы увидеть статистику")
    }

    Box(Modifier.height(18.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Среднее", if (hasData) oneDecimalComma(avgMin / 60f) + " ч" else "—")
        StatTile(Modifier.weight(1f), "Лучшая", if (bestMin > 0) oneDecimalComma(bestMin / 60f) + " ч" else "—")
        StatTile(Modifier.weight(1f), "Цель", goalText)
    }

    Box(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(Letify.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Записать сон",
            color = Letify.colors.text,
            style = Letify.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButtonRound(icon = "add-circle-bold-duotone", accent = true, onClick = onAddSleep)
    }
}

// ── Section: Шаги (coming soon) ────────────────────────────────────────

@Composable
private fun StepsSection() {
    Box(Modifier.height(8.dp))
    Column(
        Modifier.fillMaxWidth().screenHPad(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = 0f,
                color = LetifyColors.Mint,
                size = 132.dp,
                strokeWidth = 12.dp,
                discFillAlpha = 0.10f,
            )
            SolarIcon(name = "walking-bold-duotone", tint = LetifyColors.Mint, size = 44.dp)
        }
        Box(Modifier.height(18.dp))
        Text(
            "Шаги скоро здесь",
            color = Letify.colors.text,
            style = Letify.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Box(Modifier.height(6.dp))
        Text(
            "Здесь будут ваши шаги, дистанция и активность за день и за неделю.",
            color = Letify.colors.muted,
            style = Letify.typography.bodyMedium,
        )
    }

    Box(Modifier.height(20.dp))
    Row(Modifier.screenHPad(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(Modifier.weight(1f), "Дистанция", "—")
        StatTile(Modifier.weight(1f), "Калории", "—")
    }

    Box(Modifier.height(16.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(22.dp))
            .background(LetifyColors.Mint.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(LetifyColors.Mint.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { SolarIcon(name = "walking-bold-duotone", tint = LetifyColors.Mint, size = 22.dp) }
        Box(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Скоро: встроенный шагомер",
                color = Letify.colors.text,
                style = Letify.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Box(Modifier.height(2.dp))
            Text(
                "Шаги будут считаться автоматически с датчиков телефона.",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
    }
}

// ── Reusable bits ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Letify.colors.text,
        style = Letify.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.screenHPad().padding(start = 4.dp),
    )
}

/**
 * Segmented period control (Неделя / Месяц / Год) with a single pill that
 * slides smoothly between segments via [animateDpAsState] when the selection
 * changes.
 */
@Composable
private fun PeriodControl(period: Period, onSelect: (Period) -> Unit) {
    val items = Period.entries
    val selected = items.indexOf(period).coerceAtLeast(0)
    Box(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .clip(RoundedCornerShape(14.dp))
            .background(Letify.colors.track, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(36.dp)) {
            val n = items.size
            val cell = maxWidth / n
            val x by animateDpAsState(
                targetValue = cell * selected,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                label = "periodPill",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .width(cell)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Letify.colors.container, RoundedCornerShape(10.dp)),
            )
            Row(Modifier.fillMaxSize()) {
                items.forEach { p ->
                    val active = p == period
                    NoFeedbackButton(
                        onClick = { onSelect(p) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                p.title,
                                color = if (active) Letify.colors.text else Letify.colors.muted,
                                style = Letify.typography.bodyMedium,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, label: String, value: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Letify.colors.container, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
        Box(Modifier.height(4.dp))
        Text(value, color = Letify.colors.text, style = Letify.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        Modifier.fillMaxWidth().screenHPad().height(140.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Letify.colors.muted, style = Letify.typography.bodyMedium)
    }
}

// ── Charts ─────────────────────────────────────────────────────────────

/**
 * Build a smooth (rounded) path through [pts] using a cubic bezier per
 * segment with horizontal control tangents. Gives the gentle S-curve look of
 * iOS-style line charts instead of hard polyline corners.
 */
private fun smoothPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) {
        val p0 = pts[i - 1]
        val p1 = pts[i]
        val cx = (p0.x + p1.x) / 2f
        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
    }
    return path
}

/** Evaluate the y of the smoothed cubic curve at an arbitrary x (x is monotonic). */
private fun yOnCurveAt(pts: List<Offset>, x: Float): Float {
    if (pts.isEmpty()) return 0f
    if (pts.size == 1) return pts[0].y
    if (x <= pts.first().x) return pts.first().y
    if (x >= pts.last().x) return pts.last().y
    var i = 0
    while (i < pts.size - 1 && !(x >= pts[i].x && x <= pts[i + 1].x)) i++
    val p0 = pts[i]
    val p1 = pts[i + 1]
    val cx = (p0.x + p1.x) / 2f // both bezier control points share this x (matches smoothPath)
    var lo = 0f
    var hi = 1f
    repeat(24) {
        val t = (lo + hi) / 2f
        val mt = 1 - t
        val bx = mt * mt * mt * p0.x + 3 * mt * mt * t * cx + 3 * mt * t * t * cx + t * t * t * p1.x
        if (bx < x) lo = t else hi = t
    }
    val t = (lo + hi) / 2f
    val mt = 1 - t
    return mt * mt * mt * p0.y + 3 * mt * mt * t * cx + 3 * mt * t * t * cx + t * t * t * p1.y
}

/**
 * Full-bleed weight line chart — variant «Контекст / Ghost compare».
 * No grey grid/bars. A faint dashed "ghost" of the previous period sits behind
 * the current accent line + gradient fill, with a dotted goal reference and a
 * compare pill in the corner. Drag a finger across it and a marker glides along
 * the curve while a tooltip with the date + weight follows smoothly (spring),
 * fading in/out. No card background — it sits straight on the page.
 */
@Composable
private fun SwipeLineChart(
    entries: List<WeightEntry>,
    prevEntries: List<WeightEntry>,
    goalKg: Float,
    compareDelta: Float?,
    period: Period,
) {
    val points = entries.map { it.kg }
    val prevPoints = prevEntries.map { it.kg }
    val color = Letify.colors.accent
    val bg = Letify.colors.bg
    val muted = Letify.colors.muted
    val container = Letify.colors.container
    val n = points.size
    val density = LocalDensity.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Raw finger x while a touch is active, null otherwise. Continuous → smooth follow.
    var scrubX by remember(entries, period) { mutableStateOf<Float?>(null) }
    val pressed = scrubX != null
    val scrubAlpha by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 170),
        label = "scrubAlpha",
    )

    val rangeV = points + prevPoints + listOf(goalKg)
    val rawMin = rangeV.min() - 0.6f
    val rawMax = (rangeV.max() + 0.6f).coerceAtLeast(rawMin + 0.1f)
    val hPadPx = with(density) { 16.dp.toPx() }

    fun xAt(i: Int, count: Int, w: Float): Float {
        val usable = w - hPadPx * 2
        return if (count > 1) hPadPx + i * (usable / (count - 1)) else w / 2f
    }

    Box(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(196.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(entries, period) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        scrubX = down.position.x
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                scrubX = ch.position.x
                                ch.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        scrubX = null
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val vPad = 26.dp.toPx()
            val plotTop = vPad
            val plotBottom = h - vPad
            fun yAt(v: Float): Float = vPad + (1f - (v - rawMin) / (rawMax - rawMin)) * (h - vPad * 2)

            // Dotted goal reference (accent, low opacity) — no grey grid lines.
            val goalY = yAt(goalKg)
            drawLine(
                color = color.copy(alpha = 0.32f),
                start = Offset(hPadPx, goalY),
                end = Offset(w - hPadPx, goalY),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 7.dp.toPx()), 0f),
            )

            // Ghost line of the previous period (faint dashed grey).
            if (prevPoints.size > 1) {
                val ppts = prevPoints.mapIndexed { i, v -> Offset(xAt(i, prevPoints.size, w), yAt(v)) }
                drawPath(
                    smoothPath(ppts),
                    color = muted.copy(alpha = 0.45f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()), 0f),
                    ),
                )
            }

            // Current period line + gradient fill.
            val pts = points.mapIndexed { i, v -> Offset(xAt(i, n, w), yAt(v)) }
            if (n > 1) {
                val line = smoothPath(pts)
                val area = Path().apply {
                    addPath(line)
                    lineTo(pts.last().x, h)
                    lineTo(pts.first().x, h)
                    close()
                }
                drawPath(
                    area,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.0f)),
                        startY = plotTop,
                        endY = h,
                    ),
                )
                drawPath(line, color = color.copy(alpha = 0.16f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
                drawPath(line, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }

            // Resting end-point marker (hidden while scrubbing).
            if (pts.isNotEmpty()) {
                val end = pts.last()
                val restAlpha = 1f - scrubAlpha
                if (restAlpha > 0.01f) {
                    drawCircle(color = color.copy(alpha = 0.16f * restAlpha), radius = 11.dp.toPx(), center = end)
                    drawCircle(color = color.copy(alpha = restAlpha), radius = 5.5.dp.toPx(), center = end)
                    drawCircle(color = bg.copy(alpha = restAlpha), radius = 2.4.dp.toPx(), center = end)
                }
            }

            // Scrubber: glide a marker along the curve at the finger's x.
            val sx = scrubX
            if (sx != null && scrubAlpha > 0.01f && n > 0) {
                val cx = sx.coerceIn(pts.first().x, pts.last().x)
                val cy = if (n > 1) yOnCurveAt(pts, cx) else pts.first().y
                // Vertical guide.
                drawLine(
                    color = color.copy(alpha = 0.30f * scrubAlpha),
                    start = Offset(cx, plotTop - 4.dp.toPx()),
                    end = Offset(cx, plotBottom),
                    strokeWidth = 1.2.dp.toPx(),
                )
                drawCircle(color = color.copy(alpha = 0.18f * scrubAlpha), radius = 13.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = color.copy(alpha = scrubAlpha), radius = 6.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = bg.copy(alpha = scrubAlpha), radius = 2.6.dp.toPx(), center = Offset(cx, cy))
            }
        }

        // "прошлая неделя" hint for the ghost line.
        if (prevPoints.size > 1) {
            Text(
                "прошлая неделя",
                color = muted.copy(alpha = 0.7f),
                style = Letify.typography.bodySmall,
                modifier = Modifier.offset(x = 18.dp, y = 4.dp),
            )
        }

        // Compare pill (top-right): difference vs previous period.
        if (compareDelta != null && abs(compareDelta) >= 0.05f) {
            val loss = compareDelta < 0f
            val pillColor = if (loss) LetifyColors.Mint else LetifyColors.Pink
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = 2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(pillColor.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    "${if (loss) "↓" else "↑"} ${oneDecimalComma(abs(compareDelta))} кг к пр.",
                    color = pillColor,
                    style = Letify.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Floating tooltip — follows the finger smoothly (spring), shows date + weight.
        val sx = scrubX
        if (sx != null && canvasSize.width > 0 && n > 0) {
            val w = canvasSize.width.toFloat()
            val cx = sx.coerceIn(xAt(0, n, w), xAt(n - 1, n, w))
            val idx = (((cx - hPadPx) / ((w - hPadPx * 2).coerceAtLeast(1f))) * (n - 1))
                .roundToInt().coerceIn(0, n - 1)
            val tipWidthDp = 122.dp
            val tipHalfPx = with(density) { (tipWidthDp / 2).toPx() }
            val clampedX = cx.coerceIn(tipHalfPx + hPadPx, w - tipHalfPx - hPadPx)
            // Spring the displayed x so the tooltip glides rather than teleports.
            val animX by animateFloatAsState(
                targetValue = clampedX,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.85f,
                    stiffness = 900f,
                ),
                label = "tipX",
            )
            val xDp = with(density) { (animX - tipHalfPx).toDp() }
            Box(
                Modifier
                    .offset(x = xDp, y = 0.dp)
                    .width(tipWidthDp)
                    .graphicsLayer { alpha = scrubAlpha },
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(container, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        "${oneDecimalComma(entries[idx].kg)} кг",
                        color = color,
                        style = Letify.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(Modifier.height(1.dp))
                    Text(
                        parseDate(entries[idx].dateKey)?.format(DAY_MONTH) ?: "",
                        color = muted,
                        style = Letify.typography.bodySmall,
                    )
                }
            }
        }
    }

    // X-axis labels
    Box(Modifier.height(6.dp))
    if (period == Period.WEEK && n <= 7) {
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            entries.forEach {
                Text(shortDow(it.dateKey), color = Letify.colors.muted, style = Letify.typography.bodySmall)
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                parseDate(entries.first().dateKey)?.format(DAY_MONTH) ?: "",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
            Text(
                parseDate(entries.last().dateKey)?.format(DAY_MONTH) ?: "",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
    }
}


/**
 * Full-bleed sleep bars. Tap a bar to highlight it and show its duration.
 */
@Composable
private fun SleepBars(entries: List<SleepEntry>, goalMin: Int) {
    val maxMin = (entries.maxOfOrNull { it.durationMinutes } ?: goalMin)
        .coerceAtLeast(goalMin)
        .coerceAtLeast(1)
    var selected by remember(entries) { mutableStateOf(entries.lastIndex) }

    Column(Modifier.fillMaxWidth().screenHPad()) {
        if (selected in entries.indices) {
            val e = entries[selected]
            Text(
                "${shortDow(e.dateKey).ifEmpty { parseDate(e.dateKey)?.format(DAY_MONTH) ?: "" }} · ${formatHm(e.durationMinutes)}",
                color = Letify.colors.text,
                style = Letify.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.height(10.dp))
        }
        Row(
            Modifier.fillMaxWidth().height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            entries.forEachIndexed { i, e ->
                val fraction = (e.durationMinutes.toFloat() / maxMin).coerceIn(0f, 1f)
                val on = i == selected
                NoFeedbackButton(onClick = { selected = i }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(24.dp)
                                .height((14 + fraction * 96).dp)
                                .background(
                                    if (on) LetifyColors.Water else LetifyColors.Water.copy(alpha = 0.35f),
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                        Box(Modifier.height(6.dp))
                        Text(
                            shortDow(e.dateKey),
                            color = if (on) Letify.colors.text else Letify.colors.muted,
                            style = Letify.typography.bodySmall,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
