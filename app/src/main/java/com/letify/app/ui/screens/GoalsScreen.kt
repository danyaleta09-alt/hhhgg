package com.letify.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.letify.app.ui.components.FieldLabel
import com.letify.app.ui.components.OverlayScreen
import com.letify.app.ui.components.TextInput
import com.letify.app.ui.state.LocalAppState

@Composable
fun GoalsScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var water by remember { mutableStateOf(state.waterTarget.toString()) }
    var kcal by remember { mutableStateOf(state.kcalTarget.toString()) }
    var weight by remember { mutableStateOf("%.1f".format(state.weightGoal).replace(',', '.')) }

    OverlayScreen(
        title = "Цели",
        onBack = onBack,
        primaryLabel = "Сохранить",
        primaryEnabled = true,
        onPrimary = {
            state.waterTarget = water.toIntOrNull()?.coerceAtLeast(100) ?: state.waterTarget
            state.kcalTarget = kcal.toIntOrNull()?.coerceAtLeast(500) ?: state.kcalTarget
            state.weightGoal = weight.replace(',', '.').toFloatOrNull()?.coerceIn(20f, 400f) ?: state.weightGoal
            onBack()
        },
    ) {
        FieldLabel("Вода в день, мл")
        TextInput(
            value = water,
            placeholder = "2500",
            onValueChange = { water = it.filter { c -> c.isDigit() }.take(5) },
            keyboardType = KeyboardType.Number,
        )

        FieldLabel("Калории в день, ккал")
        TextInput(
            value = kcal,
            placeholder = "2300",
            onValueChange = { kcal = it.filter { c -> c.isDigit() }.take(5) },
            keyboardType = KeyboardType.Number,
        )

        FieldLabel("Целевой вес, кг")
        TextInput(
            value = weight,
            placeholder = "75.0",
            onValueChange = { input ->
                weight = input.filter { c -> c.isDigit() || c == '.' || c == ',' }
            },
            keyboardType = KeyboardType.Decimal,
        )
    }
}
