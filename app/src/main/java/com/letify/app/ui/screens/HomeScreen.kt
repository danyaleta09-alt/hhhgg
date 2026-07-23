package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.noFeedbackCombinedClick
import com.letify.app.ui.components.ProgressRing
import com.letify.app.ui.components.ScreenHeader
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.SectionTitle
import com.letify.app.ui.components.WCard
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Dates
import com.letify.app.ui.state.Habit
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.TaskItem
import com.letify.app.ui.state.TaskStatus
import com.letify.app.ui.theme.Letify
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun HomeScreen(
    @Suppress("UNUSED_PARAMETER") onAddWeight: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAddMeal: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAddSleep: () -> Unit = {},
) {
    val state = LocalAppState.current

    // Tick once a minute so "Идёт сейчас" / "Через 1ч 35м" stays current
    // without forcing the user to navigate away and back.
    var nowMin by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay() / 60) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = LocalTime.now().toSecondOfDay() / 60
        }
    }
    val dateKey = Dates.todayKey()
    val today = state.habitsToday()
    val tasksToday = state.tasksToday()

    ScreenScaffold(
        pinnedHeader = {
            ScreenHeader(
                title = "летифай",
                trailingIcon = "bell-outline",
                trailingGhost = true,
                onTrailingClick = {},
            )
        }
    ) {
        HeroCard()
        Box(Modifier.height(12.dp))

        SectionTitle("Сейчас")
        NowTaskCard(tasksToday = tasksToday, nowMin = nowMin, dateKey = dateKey)
        Box(Modifier.height(8.dp))

        val done = today.count { it.isDoneOn(dateKey) }
        SectionTitle("Привычки") {
            Text(
                if (today.isEmpty()) "—" else "$done из ${today.size}",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall
            )
        }
        HabitsList(today = today, dateKey = dateKey)
    }
}

@Composable
private fun HeroCard() {
    val state = LocalAppState.current
    // Hoisted once per recomposition — overallProgress() iterates the
    // habits list, so calling it twice (ring fill + percent label) made
    // the hero block do double work on every recomposition (which
    // happens on the 30 s nowMin tick / any habit toggle).
    val overall = state.overallProgress()
    Row(
        Modifier
            .fillMaxWidth()
            .screenHPad()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = overall,
                color = Letify.colors.accent,
                size = 140.dp,
                strokeWidth = 12.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(overall * 100).toInt()}%",
                    color = Letify.colors.text,
                    style = Letify.typography.displayLarge,
                )
                Text("цель дня", color = Letify.colors.muted, style = Letify.typography.bodySmall)
            }
        }
        Box(Modifier.width(14.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Stat(
                icon = "bottle-bold-duotone",
                color = com.letify.app.ui.theme.LetifyColors.Water,
                value = state.waterMl.toString(),
                suffix = " / ${state.waterTarget} мл",
                label = "Вода",
                progress = state.waterMl.toFloat() / state.waterTarget,
            )
            Stat(
                icon = "donut-bitten-outline",
                color = com.letify.app.ui.theme.LetifyColors.Cal,
                value = state.kcal.toString(),
                suffix = " / ${state.kcalTarget} ккал",
                label = "Питание",
                progress = state.kcal.toFloat() / state.kcalTarget,
            )
        }
    }
}

@Composable
private fun Stat(icon: String, color: Color, value: String, suffix: String, label: String, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = progress,
                color = color,
                size = 44.dp,
                strokeWidth = 5.dp,
            )
            SolarIcon(name = icon, tint = color, size = 18.dp)
        }
        Column(Modifier.padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Letify.colors.text, style = Letify.typography.titleMedium)
                Text(suffix, color = Letify.colors.muted, style = Letify.typography.bodySmall)
            }
            Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
    }
}

/**
 * The "right now" hero card on the home screen. Prefer a live task, then
 * the next upcoming task, then fall back to a friendly "ничего не
 * запланировано" placeholder when the day is fully clear. The ring shows
 * progress through the current task's time window (0% at start → 100% at
 * end) so the user can glance at it and know how much of the slot is left.
 */
@Composable
private fun NowTaskCard(tasksToday: List<TaskItem>, nowMin: Int, dateKey: String) {
    val live = tasksToday.firstOrNull { it.statusAt(nowMin, dateKey) == TaskStatus.Live }
    val next = live ?: tasksToday
        .filter { it.statusAt(nowMin, dateKey) == TaskStatus.Upcoming }
        .minByOrNull { it.startMinutes }

    WCard(
        modifier = Modifier.screenHPad(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (next == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(Letify.colors.track, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "moon-stars-bold-duotone", tint = Letify.colors.muted, size = 22.dp)
                }
                Box(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Свободно", color = Letify.colors.text, style = Letify.typography.titleMedium)
                    Text("На сегодня задач нет", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                }
            }
            return@WCard
        }

        val isLive = next === live
        val progress = if (isLive) {
            ((nowMin - next.startMinutes).toFloat() /
                (next.endMinutes - next.startMinutes).coerceAtLeast(1)).coerceIn(0f, 1f)
        } else 0f
        val ringLabel = if (isLive) "${(next.endMinutes - nowMin).coerceAtLeast(0)}м"
                        else next.startTime

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (isLive) Letify.colors.accentSoft else Letify.colors.track,
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    if (isLive) {
                        LiveDot()
                        Box(Modifier.width(6.dp))
                    }
                    Text(
                        if (isLive) "Идёт сейчас" else "Следующее",
                        color = if (isLive) Letify.colors.accent else Letify.colors.muted,
                        style = Letify.typography.labelMedium,
                    )
                }
                Box(Modifier.height(6.dp))
                Text(next.name, color = Letify.colors.text, style = Letify.typography.titleLarge)
                Box(Modifier.height(2.dp))
                Text(
                    next.statusTextAt(nowMin, dateKey),
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
            Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = progress,
                    color = if (isLive) Letify.colors.accent else next.color,
                    size = 54.dp,
                    strokeWidth = 5.dp,
                )
                Text(ringLabel, color = Letify.colors.text, style = Letify.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LiveDot() {
    Box(
        Modifier
            .size(6.dp)
            .background(Letify.colors.accent, RoundedCornerShape(999.dp))
    )
}

@Composable
private fun HabitsList(today: List<Habit>, dateKey: String) {
    val state = LocalAppState.current
    WCard(
        modifier = Modifier.screenHPad(),
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
    ) {
        if (today.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Сегодня свободно — добавь первую привычку в Плане",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
            return@WCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            today.forEach { h ->
                HabitRow(
                    h, dateKey,
                    onTap = { state.tapHabit(h.id) },
                    onLongTap = { state.untapHabit(h.id) },
                )
            }
        }
    }
}

@Composable
private fun HabitRow(h: Habit, dateKey: String, onTap: () -> Unit, onLongTap: () -> Unit = {}) {
    val progress = h.progressOn(dateKey)
    val done = h.isDoneOn(dateKey)
    val target = h.effectiveTarget
    val ringFraction = (progress.toFloat() / target).coerceIn(0f, 1f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tap = step up (wraps to 0 at target); long-press = step down by one
        // (a short haptic tick fires) so a multi-count habit can be corrected
        // without tapping all the way around.
        Box(
            modifier = Modifier
                .size(28.dp)
                .noFeedbackCombinedClick(onLongClick = onLongTap, onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            // Solid coloured disc, no transparency. While not started it's
            // an empty disc; once done it carries a white check; partial
            // progress is shown as a thin white inner-arc on top of the
            // solid disc — keeps the row reading as a clean coloured
            // checkbox rather than a washed-out donut.
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(h.color, RoundedCornerShape(999.dp)),
                )
                if (done) {
                    SolarIcon(name = "check-read-bold", tint = Color.White, size = 16.dp)
                } else if (ringFraction > 0f && target > 1) {
                    ProgressRing(
                        progress = ringFraction,
                        color = Color.White,
                        size = 22.dp,
                        strokeWidth = 2.5.dp,
                        discFillAlpha = 0f,
                    )
                }
            }
        }
        Box(Modifier.width(12.dp))
        // Telegram-style solid coloured tile with a white glyph — same
        // visual language as the settings rows on AddHabit so the home
        // list and the editor stay coherent.
        Box(
            Modifier
                .size(34.dp)
                .background(h.color, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = h.icon, tint = Color.White, size = 20.dp)
        }
        Box(Modifier.width(12.dp))
        Text(
            h.name,
            color = if (done) Letify.colors.muted else Letify.colors.text,
            style = Letify.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            ),
            modifier = Modifier.weight(1f),
        )
        if (target > 1) {
            Text(
                "$progress/$target",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
    }
}
