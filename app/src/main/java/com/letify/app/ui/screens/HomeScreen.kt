package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.ProgressRing
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Dates
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.TaskItem
import com.letify.app.ui.state.TaskStatus
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onAddWeight: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onAddSleep: () -> Unit = {},
    onWaterHistory: () -> Unit = {},
) {
    val state = LocalAppState.current
    var nowMin by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay() / 60) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = LocalTime.now().toSecondOfDay() / 60
        }
    }
    val dateKey = Dates.todayKey()
    val tasksToday = state.tasksToday()
    val overall = state.overallProgress()
    val hour = LocalTime.now().hour
    val greet = when {
        hour < 6 -> "Доброй ночи"
        hour < 12 -> "Доброе утро"
        hour < 18 -> "Добрый день"
        else -> "Добрый вечер"
    }
    val today = LocalDate.now()
    val dateLabel = buildString {
        append(today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("ru")))
        append(", ")
        append(today.dayOfMonth)
        append(" ")
        append(today.month.getDisplayName(TextStyle.FULL, Locale("ru")))
    }

    ScreenScaffold(
        pinnedHeader = {
            Text(
                "летифай",
                color = Letify.colors.text,
                style = Letify.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .screenHPad()
                    .padding(top = 12.dp, bottom = 8.dp),
            )
        },
    ) {
        // Greeting
        Column(Modifier.screenHPad().padding(top = 8.dp, bottom = 28.dp)) {
            Text(
                dateLabel.replaceFirstChar { it.titlecase(Locale("ru")) },
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$greet,\n${state.userName.ifBlank { "друг" }}",
                color = Letify.colors.text,
                style = Letify.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 34.sp,
            )
        }

        // Day progress — single large percent
        Column(Modifier.screenHPad().padding(bottom = 28.dp)) {
            Text(
                text = "${(overall * 100).toInt()}%",
                color = Letify.colors.text,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.5).sp,
                lineHeight = 68.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "прогресс за день · план, вода, питание",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }

        // Divider
        Box(
            Modifier
                .screenHPad()
                .fillMaxWidth()
                .height(1.dp)
                .background(Letify.colors.track),
        )
        Spacer(Modifier.height(28.dp))

        // Now
        SectionLabel("Сейчас")
        NowBlock(tasksToday = tasksToday, nowMin = nowMin, dateKey = dateKey)
        Spacer(Modifier.height(28.dp))

        // Water
        Row(
            Modifier.screenHPad().fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Вода", color = Letify.colors.muted, style = Letify.typography.bodySmall)
            NoFeedbackButton(onClick = onWaterHistory) {
                Text("история", color = Letify.colors.accent, style = Letify.typography.bodySmall)
            }
        }
        WaterBlock()
        Spacer(Modifier.height(28.dp))

        // Nutrition
        SectionLabel("Питание")
        NutritionBlock(onOpen = onOpenNutrition)
        Spacer(Modifier.height(28.dp))

        // Sleep / Weight
        Row(Modifier.screenHPad().fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Сон", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                val sleep = state.sleepLog.maxByOrNull { it.dateKey }
                val sleepText = sleep?.let {
                    val m = it.durationMinutes
                    "${m / 60} ч ${m % 60} м"
                } ?: "—"
                NoFeedbackButton(onClick = onAddSleep) {
                    Text(
                        sleepText,
                        color = Letify.colors.text,
                        style = Letify.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Вес", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                NoFeedbackButton(onClick = onAddWeight) {
                    Text(
                        String.format("%.1f кг", state.weight).replace('.', ','),
                        color = Letify.colors.text,
                        style = Letify.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Letify.colors.muted,
        style = Letify.typography.bodySmall,
        modifier = Modifier.screenHPad().padding(bottom = 12.dp),
    )
}

@Composable
private fun NowBlock(tasksToday: List<TaskItem>, nowMin: Int, dateKey: String) {
    val live = tasksToday.firstOrNull { it.statusAt(nowMin, dateKey) == TaskStatus.Live }
    val next = live ?: tasksToday
        .filter { it.statusAt(nowMin, dateKey) == TaskStatus.Upcoming }
        .minByOrNull { it.startMinutes }

    Column(Modifier.screenHPad()) {
        if (next == null) {
            Text(
                "ничего не запланировано",
                color = Letify.colors.muted,
                style = Letify.typography.bodyMedium,
            )
        } else {
            val status = next.statusAt(nowMin, dateKey)
            val tag = when (status) {
                TaskStatus.Live -> {
                    val left = (next.endMinutes - nowMin).coerceAtLeast(0)
                    "идёт · ещё $left мин"
                }
                TaskStatus.Upcoming -> {
                    val inMin = (next.startMinutes - nowMin).coerceAtLeast(0)
                    val h = inMin / 60
                    val m = inMin % 60
                    if (h > 0) "через ${h}ч ${m}м" else "через $m мин"
                }
                else -> "готово"
            }
            Text(
                tag,
                color = Letify.colors.accent,
                style = Letify.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                next.name,
                color = Letify.colors.text,
                style = Letify.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatRange(next.startMinutes, next.endMinutes),
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
    }
}

private fun formatRange(start: Int, end: Int): String {
    fun fmt(m: Int) = "%d:%02d".format(m / 60, m % 60)
    return "${fmt(start)} – ${fmt(end)}"
}

@Composable
private fun WaterBlock() {
    val state = LocalAppState.current
    val ml = state.waterMl
    val goal = state.waterTarget.coerceAtLeast(1)
    val liters = ml / 1000f
    val goalLiters = goal / 1000f
    val progress = (ml.toFloat() / goal).coerceIn(0f, 1f)

    Column(
        Modifier
            .screenHPad()
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Letify.colors.container)
            .padding(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    String.format("%.2f", liters).replace('.', ','),
                    color = Letify.colors.text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "л",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Text(
                "из ${String.format("%.1f", goalLiters).replace('.', ',')} л",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Letify.colors.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .background(LetifyColors.Water, RoundedCornerShape(99.dp)),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WaterChip("+100", false, Modifier.weight(1f)) {
                state.addWater(100, "Вода", "water")
            }
            WaterChip("+250", false, Modifier.weight(1f)) {
                state.addWater(250, "Вода", "water")
            }
            WaterChip("+500", true, Modifier.weight(1f)) {
                state.addWater(500, "Вода", "water")
            }
        }
    }
}

@Composable
private fun WaterChip(
    label: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NoFeedbackButton(onClick = onClick, modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .then(
                    if (primary) {
                        Modifier.background(LetifyColors.Water, RoundedCornerShape(12.dp))
                    } else {
                        Modifier.border(1.dp, Letify.colors.track, RoundedCornerShape(12.dp))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = if (primary) Color.White else Letify.colors.text,
                style = Letify.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun NutritionBlock(onOpen: () -> Unit) {
    val state = LocalAppState.current
    val progress = (state.kcal.toFloat() / state.kcalTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
    val left = (state.kcalTarget - state.kcal).coerceAtLeast(0)

    NoFeedbackButton(onClick = onOpen, modifier = Modifier.screenHPad().fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    ProgressRing(
                        progress = progress,
                        color = LetifyColors.Cal,
                        size = 48.dp,
                        strokeWidth = 3.dp,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${state.kcal} ккал",
                        color = Letify.colors.text,
                        style = Letify.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "цель ${state.kcalTarget} · осталось $left",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodySmall,
                    )
                }
                SolarIcon(
                    name = "alt-arrow-right-outline",
                    tint = Letify.colors.muted.copy(alpha = 0.45f),
                    size = 18.dp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Letify.colors.track))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MacroStat("${state.protein} г", "белки")
                MacroStat("${state.fat} г", "жиры")
                MacroStat("${state.carb} г", "углеводы")
            }
        }
    }
}

@Composable
private fun MacroStat(value: String, label: String) {
    Column {
        Text(
            value,
            color = Letify.colors.text,
            style = Letify.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
    }
}
