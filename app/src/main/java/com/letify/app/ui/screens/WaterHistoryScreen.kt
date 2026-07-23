package com.letify.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.SectionTitle
import com.letify.app.ui.components.SegItem
import com.letify.app.ui.components.SegmentedTabs
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.WCard
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Dates
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.WaterEntry
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SHORT_DOW = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
private val DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM")

private fun parseDate(dateKey: String): LocalDate? =
    runCatching { LocalDate.parse(dateKey) }.getOrNull()

private fun shortDow(dateKey: String): String =
    parseDate(dateKey)?.let { SHORT_DOW[it.dayOfWeek.value - 1] } ?: ""

private fun dayMonth(dateKey: String): String =
    parseDate(dateKey)?.format(DAY_MONTH) ?: dateKey

/**
 * "История воды" — day-by-day log + week/month chart.
 *
 * Layout mirrors Appearance / Bindings / Notifications exactly so the
 * transparent status-bar contract is identical (no strip / gap / opaque bar
 * at the top).
 */
@Composable
fun WaterHistoryScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var period by remember { mutableStateOf("week") }
    val days = if (period == "week") 7 else 30
    val totals = remember(state.waterHistory.size, period) { state.waterDailyTotals(days) }
    val weekTotals = remember(state.waterHistory.size) { state.waterDailyTotals(7) }
    val todayKey = Dates.todayKey()

    val nonZeroTotals = weekTotals.map { it.second }.filter { it > 0 }
    val avg = if (nonZeroTotals.isEmpty()) 0 else nonZeroTotals.sum() / nonZeroTotals.size
    val best = totals.maxByOrNull { it.second }

    var expandedDay by remember { mutableStateOf<String?>(Dates.todayKey()) }
    val scroll = rememberScrollState()

    // Same scaffold as AppearanceScreen / BindingsScreen / NotificationsScreen:
    // full-bleed opaque bg, then scroll column with statusBars inset + top pad.
    // Previous ordering (insets before scroll, no top pad) left a visible
    // artifact under the system status icons on this screen only.
    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 24.dp),
        ) {
            SettingsHeader(title = "История воды", onBack = onBack)

            // ── Quick stats ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().screenHPad(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(Modifier.weight(1f), "Сегодня", "${state.waterMl} мл")
                StatTile(Modifier.weight(1f), "Среднее / 7 дн.", "$avg мл")
                StatTile(
                    Modifier.weight(1f),
                    "Лучший день",
                    if (best != null && best.second > 0) "${best.second} мл" else "—",
                )
            }

            Box(Modifier.height(18.dp))

            // ── Chart ──────────────────────────────────────────────────
            Box(Modifier.screenHPad()) {
                SegmentedTabs(
                    items = listOf(
                        SegItem("week", "Неделя"),
                        SegItem("month", "Месяц"),
                    ),
                    selected = period,
                    onSelect = { period = it },
                )
            }
            Box(Modifier.height(14.dp))
            WCard(modifier = Modifier.screenHPad()) {
                WaterBars(
                    totals = totals,
                    goalMl = state.waterTarget,
                    compact = period == "month",
                    selectedDateKey = expandedDay ?: totals.lastOrNull()?.first,
                    onSelectDay = { key -> expandedDay = key },
                )
            }

            Box(Modifier.height(20.dp))

            // ── Day-by-day list ───────────────────────────────────────
            SectionTitle("По дням")
            if (state.waterHistory.isEmpty()) {
                WCard(modifier = Modifier.screenHPad()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Записей пока нет — добавь первую воду на экране «Вода»",
                            color = Letify.colors.muted,
                            style = Letify.typography.bodySmall,
                        )
                    }
                }
            } else {
                val dayKeys = state.waterHistory.map { it.dateKey }.distinct()
                    .sortedDescending()
                WCard(
                    modifier = Modifier.screenHPad(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Column {
                        dayKeys.forEachIndexed { i, dateKey ->
                            DayRow(
                                dateKey = dateKey,
                                isToday = dateKey == todayKey,
                                total = state.waterEntriesOn(dateKey).sumOf { it.ml },
                                goal = state.waterTarget,
                                expanded = expandedDay == dateKey,
                                onToggle = {
                                    expandedDay = if (expandedDay == dateKey) null else dateKey
                                },
                                entries = state.waterEntriesOn(dateKey),
                            )
                            if (i != dayKeys.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .height(1.dp)
                                        .background(Letify.colors.muted.copy(alpha = 0.12f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier = Modifier, label: String, value: String) {
    // Fixed height so all three tiles align regardless of label line-count.
    Box(
        modifier
            .height(76.dp)
            .background(Letify.colors.container, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(
                value,
                color = Letify.colors.text,
                style = Letify.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Box(Modifier.height(2.dp))
            Text(
                label,
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun WaterBars(
    totals: List<Pair<String, Int>>,
    goalMl: Int,
    compact: Boolean,
    selectedDateKey: String?,
    onSelectDay: (String) -> Unit,
) {
    val maxMl = (totals.maxOfOrNull { it.second } ?: goalMl).coerceAtLeast(goalMl).coerceAtLeast(1)
    val selected = totals.indexOfFirst { it.first == selectedDateKey }.let { if (it >= 0) it else totals.lastIndex }

    Column(Modifier.fillMaxWidth()) {
        if (selected in totals.indices) {
            val (dateKey, ml) = totals[selected]
            Text(
                "${shortDow(dateKey).ifEmpty { dayMonth(dateKey) }} · $ml мл",
                color = Letify.colors.text,
                style = Letify.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.height(10.dp))
        }
        Row(
            Modifier.fillMaxWidth().height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            totals.forEachIndexed { i, (dateKey, ml) ->
                val fraction = (ml.toFloat() / maxMl).coerceIn(0f, 1f)
                val metGoal = ml >= goalMl && goalMl > 0
                val on = i == selected
                NoFeedbackButton(onClick = { onSelectDay(dateKey) }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(if (compact) 6.dp else 24.dp)
                                .height((6 + fraction * 104).dp)
                                .background(
                                    when {
                                        on -> LetifyColors.Water
                                        metGoal -> LetifyColors.Water.copy(alpha = 0.6f)
                                        else -> LetifyColors.Water.copy(alpha = 0.3f)
                                    },
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                        if (!compact) {
                            Box(Modifier.height(6.dp))
                            Text(
                                shortDow(dateKey),
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
}

@Composable
private fun DayRow(
    dateKey: String,
    isToday: Boolean,
    total: Int,
    goal: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    entries: List<WaterEntry>,
) {
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .noFeedbackClick(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).background(LetifyColors.Water.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                SolarIcon(name = "bottle-bold-duotone", tint = LetifyColors.Water, size = 20.dp)
            }
            Box(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isToday) "Сегодня" else "${shortDow(dateKey)}, ${dayMonth(dateKey)}",
                    color = Letify.colors.text,
                    style = Letify.typography.titleSmall,
                )
                Text(
                    "$total из $goal мл · ${entries.size} " + entriesWord(entries.size),
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
            SolarIcon(
                name = if (expanded) "alt-arrow-up-outline" else "alt-arrow-down-outline",
                tint = Letify.colors.muted,
                size = 18.dp,
            )
        }
        if (expanded) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                entries.sortedByDescending { it.time }.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SolarIcon(name = e.icon, tint = LetifyColors.Water, size = 16.dp)
                        Box(Modifier.width(10.dp))
                        Text(e.label, color = Letify.colors.text, style = Letify.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(e.time, color = Letify.colors.muted, style = Letify.typography.bodySmall)
                        Box(Modifier.width(10.dp))
                        Text("+${e.ml} мл", color = Letify.colors.text, style = Letify.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun entriesWord(n: Int): String {
    val mod100 = n % 100
    val mod10 = n % 10
    return when {
        mod100 in 11..14 -> "записей"
        mod10 == 1 -> "запись"
        mod10 in 2..4 -> "записи"
        else -> "записей"
    }
}
