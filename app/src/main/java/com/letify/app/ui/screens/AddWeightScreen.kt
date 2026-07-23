package com.letify.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.LetifyBottomSheet
import com.letify.app.ui.components.WheelPicker
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify

private const val MIN_WEIGHT_KG = 20
private const val MAX_WEIGHT_KG = 300

/**
 * Bottom-sheet weight entry. Replaces the old stepper layout with a pair
 * of wheel pickers (whole kilograms + decimal digit). The save action
 * now lives as a small check glyph in the sheet header (top-right) —
 * one tap commits and dismisses with a smooth animation.
 */
@Composable
fun AddWeightScreen(onBack: () -> Unit) {
    val state = LocalAppState.current

    // Seed the wheels from the current weight value. Stored as separate
    // integers so each wheel can be controlled independently — combined
    // back into a single Float on save.
    val initialKg = state.weight.toInt().coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG)
    val initialDecimal = ((state.weight - state.weight.toInt()) * 10f).toInt().coerceIn(0, 9)

    var kg by remember { mutableIntStateOf(initialKg) }
    var decimal by remember { mutableIntStateOf(initialDecimal) }

    val kgValues = remember { (MIN_WEIGHT_KG..MAX_WEIGHT_KG).toList() }
    val decimalValues = remember { (0..9).toList() }

    LetifyBottomSheet(
        title = "Записать вес",
        onDismiss = onBack,
        trailingIcon = "check-bold",
        onTrailing = {
            // Record a real weigh-in (appends to history + updates current).
            state.logWeight(kg + decimal / 10f)
        },
    ) {
        // Headline numeric readout above the wheels. Tracks the live
        // values as the user spins so it always reflects what's about
        // to be committed.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$kg,$decimal",
                    color = Letify.colors.text,
                    style = Letify.typography.displayLarge,
                )
                Text(
                    text = " кг",
                    color = Letify.colors.muted,
                    style = Letify.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        // The wheel pair. The kg wheel takes ~2/3 of the width, decimal
        // is narrower (single digit, smaller value space).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelPicker(
                values = kgValues,
                initialIndex = kgValues.indexOf(initialKg).coerceAtLeast(0),
                modifier = Modifier.weight(2f),
                visibleItems = 5,
                onSelected = { _, v -> kg = v },
                label = { it.toString() },
            )
            Box(Modifier.width(6.dp))
            // Decimal separator that visually anchors the two wheels
            // together. Sits centred on the selected-row line.
            Text(
                text = ",",
                color = Letify.colors.text,
                style = Letify.typography.displayMedium,
            )
            Box(Modifier.width(6.dp))
            WheelPicker(
                values = decimalValues,
                initialIndex = decimalValues.indexOf(initialDecimal).coerceAtLeast(0),
                modifier = Modifier.weight(1f),
                visibleItems = 5,
                onSelected = { _, v -> decimal = v },
                label = { it.toString() },
            )
        }
    }
}
