package com.letify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify

/**
 * Curated catalog of Solar icons shown in the wizard step 2 picker.
 *
 * Roughly grouped into rows: hydration & food, fitness, mind & sleep,
 * learning & focus, time & calendars, mood/reward, and miscellaneous
 * lifestyle. The grid is rendered 5 columns wide; total cells ≈ 40.
 */
val WizardIconCatalog: List<String> = listOf(
    // hydration / food
    "bottle-bold-duotone", "cup-paper-bold-duotone", "cup-hot-bold-duotone", "tea-cup-bold-duotone", "wineglass-bold-duotone",
    "chef-hat-bold-duotone", "whisk-bold-duotone", "donut-bold-duotone", "donut-bitten-bold-duotone", "waterdrop-bold-duotone",
    // fitness
    "dumbbell-bold-duotone", "dumbbells-bold-duotone", "running-bold-duotone", "bicycling-bold-duotone", "swimming-bold-duotone",
    "stretching-bold-duotone", "hiking-bold-duotone", "treadmill-round-bold-duotone", "body-shape-bold-duotone", "meditation-bold-duotone",
    // mind / sleep / health
    "moon-stars-bold-duotone", "moon-sleep-bold-duotone", "bed-bold-duotone", "heart-bold-duotone", "heart-pulse-bold-duotone",
    "pill-bold-duotone", "stethoscope-bold-duotone", "leaf-bold-duotone", "flame-bold-duotone", "sunrise-bold-duotone",
    // learning / focus / work
    "book-bookmark-bold-duotone", "book-2-bold-duotone", "notebook-bold-duotone", "square-academic-cap-bold-duotone", "calculator-bold-duotone",
    "clipboard-check-bold-duotone", "alarm-bold-duotone", "stopwatch-bold-duotone", "star-bold-duotone", "medal-star-bold-duotone",
)

/**
 * Big square preview tile (top of step 2). Shows the selected icon over a
 * soft tinted disc, with the habit/task title + a one-line meta caption.
 */
@Composable
fun WizardPreviewTile(icon: String, color: Color, title: String, meta: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Letify.colors.container, RoundedCornerShape(22.dp))
            .padding(vertical = 18.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(color.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = color, size = 34.dp)
        }
        Text(
            text = title.ifBlank { "Без названия" },
            color = if (title.isBlank()) Letify.colors.muted else Letify.colors.text,
            style = Letify.typography.titleMedium,
        )
        if (meta.isNotBlank()) {
            Text(meta, color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
    }
}

/**
 * 5-column grid icon picker. No internal scrolling — meant to flow as part
 * of a larger scrollable form. Cells are square (aspectRatio 1:1) so they
 * stay perfectly even regardless of screen width.
 */
@Composable
fun WizardIconGrid(
    icons: List<String> = WizardIconCatalog,
    selected: String,
    tint: Color,
    onSelect: (String) -> Unit,
    columns: Int = 5,
) {
    val rows = (icons.size + columns - 1) / columns
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(rows) { r ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(columns) { c ->
                    val idx = r * columns + c
                    if (idx < icons.size) {
                        val ic = icons[idx]
                        val active = ic == selected
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (active) tint.copy(alpha = 0.18f) else Letify.colors.track,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { onSelect(ic) },
                            contentAlignment = Alignment.Center,
                        ) {
                            SolarIcon(
                                name = ic,
                                tint = if (active) tint else Letify.colors.text,
                                size = 24.dp,
                            )
                        }
                    } else {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 7-day row picker (Пн … Вс). Renders the days as equal-weight pill cells;
 * tap toggles. Uses ISO weekday numbers (1=Mon … 7=Sun).
 */
@Composable
fun WeekdaysPicker(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                        RoundedCornerShape(12.dp),
                    )
                    .clickable {
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

/**
 * Horizontal row of small selection "chips". Single-select. Used for quick
 * presets (units, durations, day patterns, …).
 */
@Composable
fun ChipRow(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    // Horizontally scrollable strip so longer catalogs (units, day presets)
    // never wrap awkwardly. Padding mirrors the form's own horizontal pad
    // so chips slide off-screen rather than getting clipped inside a card.
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            val active = opt == selected
            Box(
                Modifier
                    .background(
                        if (active) Letify.colors.accentSoft else Letify.colors.track,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
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

/**
 * Plain "+ / − value" stepper used for habit targets. Centred number, two
 * round buttons. Range is clamped; out-of-range presses are silent.
 */
@Composable
fun NumberStepper(value: Int, min: Int = 1, max: Int = 999, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Letify.colors.track, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepBtn(icon = "minus-circle-outline") {
            if (value - 1 >= min) onChange(value - 1)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                value.toString(),
                color = Letify.colors.text,
                style = Letify.typography.titleLarge,
            )
        }
        StepBtn(icon = "add-circle-outline") {
            if (value + 1 <= max) onChange(value + 1)
        }
    }
}

@Composable
private fun StepBtn(icon: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(Letify.colors.container, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        SolarIcon(name = icon, tint = Letify.colors.text, size = 22.dp)
    }
}

/**
 * Small toggle row: title + sub + on/off pill switch on the right. Used for
 * the "Напомнить" toggle on step 3.
 */
@Composable
fun ToggleRow(title: String, subtitle: String?, on: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Letify.colors.track, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Letify.colors.text, style = Letify.typography.titleSmall)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = Letify.colors.muted, style = Letify.typography.bodySmall)
            }
        }
        SwitchPill(on = on, onToggle = onToggle)
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
            .clickable { onToggle(!on) },
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
