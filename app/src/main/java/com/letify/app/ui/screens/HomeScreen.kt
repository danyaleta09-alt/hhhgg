package com.letify.app.ui.screens

import androidx.compose.foundation.background
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

/**
 * Home — variant A (list):
 *  hero ring + greeting, then plain rows for now / water / nutrition / sleep / weight.
 *  No habits, no notification bell, no nutrition tab.
 */
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
    val dateShort = buildString {
        append(today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru")))
        append(", ")
        append(today.dayOfMonth)
        append(" ")
        append(today.month.getDisplayName(TextStyle.FULL, Locale("ru")))
    }

    val planDone = tasksToday.count { it.statusAt(nowMin, dateKey) == TaskStatus.Done }
    val planTotal = tasksToday.size
    val waterL = state.waterMl / 1000f
    val waterGoalL = state.waterTarget / 1000f

    ScreenScaffold(
        pinnedHeader = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .screenHPad()
                    .padding(top = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "летифай",
                    color = Letify.colors.text,
                    style = Letify.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    dateShort.replaceFirstChar { it.titlecase(Locale("ru")) },
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) {
        // ── Hero ──
        Row(
            Modifier
                .screenHPad()
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Letify.colors.container)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = overall,
                    color = Letify.colors.accent,
                    size = 88.dp,
                    strokeWidth = 7.5.dp,
                )
                Text(
                    "${(overall * 100).toInt()}%",
                    color = Letify.colors.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    greet,
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    state.userName.ifBlank { "друг" },
                    color = Letify.colors.text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HeroStat(
                        if (planTotal == 0) "—" else "$planDone/$planTotal",
                        "план",
                    )
                    HeroStat(
                        String.format("%.2f", waterL).trimEnd('0').trimEnd(',', '.').replace('.', ','),
                        "л воды",
                    )
                    HeroStat("${state.kcal}", "ккал")
                }
            }
        }

        // ── Сейчас ──
        SectionLabel("Сейчас")
        NowRow(tasksToday = tasksToday, nowMin = nowMin, dateKey = dateKey)

        // ── Вода ──
        SectionLabel("Вода")
        WaterSection(onHistory = onWaterHistory)

        // ── Питание ──
        SectionLabel("Питание")
        ListRow(
            dot = LetifyColors.Cal,
            title = "${state.kcal} ккал",
            subtitle = "цель ${state.kcalTarget} · осталось ${(state.kcalTarget - state.kcal).coerceAtLeast(0)}",
            onClick = onOpenNutrition,
            trailing = {
                SolarIcon(
                    name = "alt-arrow-right-outline",
                    tint = Letify.colors.muted.copy(alpha = 0.4f),
                    size = 16.dp,
                )
            },
        )

        // ── Ещё ──
        SectionLabel("Ещё")
        val sleep = state.sleepLog.maxByOrNull { it.dateKey }
        val sleepText = sleep?.let {
            val m = it.durationMinutes
            "${m / 60}ч ${m % 60}м"
        } ?: "—"
        ListRow(
            dot = LetifyColors.Mint,
            title = "Сон",
            subtitle = "прошлая ночь",
            onClick = onAddSleep,
            trailing = {
                Text(
                    sleepText,
                    color = Letify.colors.text,
                    style = Letify.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
        )
        ListRow(
            dot = LetifyColors.Yellow,
            title = "Вес",
            subtitle = "последний замер",
            onClick = onAddWeight,
            trailing = {
                Text(
                    String.format("%.1f кг", state.weight).replace('.', ','),
                    color = Letify.colors.text,
                    style = Letify.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            showDivider = false,
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column {
        Text(
            value,
            color = Letify.colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
        )
        Text(
            label,
            color = Letify.colors.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = Letify.colors.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .screenHPad()
            .padding(top = 22.dp, bottom = 4.dp),
    )
}

@Composable
private fun NowRow(tasksToday: List<TaskItem>, nowMin: Int, dateKey: String) {
    val live = tasksToday.firstOrNull { it.statusAt(nowMin, dateKey) == TaskStatus.Live }
    val next = live ?: tasksToday
        .filter { it.statusAt(nowMin, dateKey) == TaskStatus.Upcoming }
        .minByOrNull { it.startMinutes }

    Row(
        Modifier
            .screenHPad()
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(9.dp)
                .background(
                    if (live != null) Letify.colors.accent else Letify.colors.muted.copy(alpha = 0.45f),
                    CircleShape,
                ),
        )
        Spacer(Modifier.width(14.dp))
        if (next == null) {
            Text(
                "Свободно · нет задач",
                color = Letify.colors.muted,
                style = Letify.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        } else {
            val status = next.statusAt(nowMin, dateKey)
            val tag = when (status) {
                TaskStatus.Live -> {
                    val left = (next.endMinutes - nowMin).coerceAtLeast(0)
                    "ещё $left мин"
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
                buildString {
                    append(next.name)
                    append(" · ")
                    append(tag)
                },
                color = Letify.colors.text,
                style = Letify.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    Box(
        Modifier
            .screenHPad()
            .fillMaxWidth()
            .height(1.dp)
            .background(Letify.colors.track),
    )
}

@Composable
private fun WaterSection(onHistory: () -> Unit) {
    val state = LocalAppState.current
    val ml = state.waterMl
    val goal = state.waterTarget.coerceAtLeast(1)
    val liters = ml / 1000f
    val progress = (ml.toFloat() / goal).coerceIn(0f, 1f)

    Column(Modifier.screenHPad().fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .background(LetifyColors.Water, CircleShape),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    String.format("%.2f л", liters).replace('.', ','),
                    color = Letify.colors.text,
                    style = Letify.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "цель ${String.format("%.1f", goal / 1000f).replace('.', ',')} л · ${(progress * 100).toInt()}%",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            NoFeedbackButton(onClick = onHistory) {
                Text(
                    "история",
                    color = Letify.colors.accent,
                    style = Letify.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Letify.colors.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(LetifyColors.Water, RoundedCornerShape(99.dp)),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Letify.colors.track),
        )
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
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (primary) LetifyColors.Water
                    else Letify.colors.text.copy(alpha = 0.06f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = if (primary) Color.White else Letify.colors.text,
                style = Letify.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ListRow(
    dot: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    showDivider: Boolean = true,
) {
    NoFeedbackButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.screenHPad().fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(dot, CircleShape),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Letify.colors.text,
                        style = Letify.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle,
                        color = Letify.colors.muted,
                        style = Letify.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                trailing()
            }
            if (showDivider) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Letify.colors.track),
                )
            }
        }
    }
}
