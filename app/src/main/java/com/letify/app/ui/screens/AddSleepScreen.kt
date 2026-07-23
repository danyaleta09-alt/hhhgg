package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.FieldLabel
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.LetifyBottomSheet
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify

/**
 * Sleep entry as a modal bottom sheet (same surface as the weight sheet). The
 * "Сохранить" button persists a real sleep night and dismisses with the
 * sheet's shared enter/exit animation.
 */
@Composable
fun AddSleepScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var fromH by remember { mutableStateOf(23) }
    var fromM by remember { mutableStateOf(30) }
    var toH by remember { mutableStateOf(7) }
    var toM by remember { mutableStateOf(0) }
    var quality by remember { mutableStateOf(2) }

    LetifyBottomSheet(
        title = "Записать сон",
        onDismiss = onBack,
        primaryLabel = "Сохранить",
        onPrimary = {
            // Persist a real sleep night. LetifyBottomSheet dismisses for us.
            state.logSleep(fromH * 60 + fromM, toH * 60 + toM, quality)
        },
    ) {
        FieldLabel("Лёг спать")
        TimePicker(fromH, fromM) { h, m -> fromH = h; fromM = m }
        FieldLabel("Проснулся")
        TimePicker(toH, toM) { h, m -> toH = h; toM = m }
        FieldLabel("Качество сна")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Плохо", "Так себе", "Норм", "Отлично").forEachIndexed { idx, q ->
                val active = idx == quality
                NoFeedbackButton(onClick = { quality = idx }, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .background(if (active) Letify.colors.accentSoft else Letify.colors.track, RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(q, color = if (active) Letify.colors.accent else Letify.colors.text, style = Letify.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePicker(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Letify.colors.track, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stepper(hour, 0, 23) { onChange(it, minute) }
        Text(":", color = Letify.colors.text, style = Letify.typography.displayMedium, modifier = Modifier.padding(horizontal = 8.dp))
        Stepper(minute, 0, 59, step = 5) { onChange(hour, it) }
    }
}

@Composable
private fun Stepper(value: Int, min: Int, max: Int, step: Int = 1, onChange: (Int) -> Unit) {
    fun wrap(next: Int): Int {
        val span = max - min + 1
        return ((next - min) % span + span) % span + min
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        NoFeedbackButton(onClick = { onChange(wrap(value - step)) }, modifier = Modifier.size(36.dp)) {
            Box(
                Modifier.size(36.dp).background(Letify.colors.container, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = "alt-arrow-down-outline", tint = Letify.colors.text, size = 18.dp) }
        }
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Text("%02d".format(value), color = Letify.colors.text, style = Letify.typography.displayMedium)
        }
        NoFeedbackButton(onClick = { onChange(wrap(value + step)) }, modifier = Modifier.size(36.dp)) {
            Box(
                Modifier.size(36.dp).background(Letify.colors.container, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SolarIcon(name = "alt-arrow-up-outline", tint = Letify.colors.text, size = 18.dp) }
        }
    }
}
