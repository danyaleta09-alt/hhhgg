package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.FieldLabel
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.OverlayScreen
import com.letify.app.ui.components.TextInput
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

private data class MealKind(val key: String, val title: String, val icon: String, val color: Color)
private val mealKinds = listOf(
    MealKind("breakfast", "Завтрак", "sun-outline", LetifyColors.Orange),
    MealKind("lunch", "Обед", "plate-bold-duotone", LetifyColors.Cal),
    MealKind("dinner", "Ужин", "moon-outline", LetifyColors.Purple),
    MealKind("snack", "Перекус", "donut-bitten-outline", LetifyColors.Pink),
)

@Composable
fun AddNutritionScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("breakfast") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carb by remember { mutableStateOf("") }

    OverlayScreen(
        title = "Добавить приём пищи",
        onBack = onBack,
        primaryLabel = "Добавить",
        primaryEnabled = name.isNotBlank() && kcal.isNotBlank(),
        onPrimary = {
            state.kcal += kcal.toIntOrNull() ?: 0
            state.protein += protein.toIntOrNull() ?: 0
            state.fat += fat.toIntOrNull() ?: 0
            state.carb += carb.toIntOrNull() ?: 0
            onBack()
        },
    ) {
        FieldLabel("Название")
        TextInput(value = name, placeholder = "Овсянка с ягодами", onValueChange = { name = it })

        FieldLabel("Тип приёма")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            mealKinds.forEach { mk ->
                val active = mk.key == kind
                NoFeedbackButton(onClick = { kind = mk.key }, modifier = Modifier.weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp)
                            .background(
                                if (active) mk.color.copy(alpha = 0.16f) else Letify.colors.track,
                                RoundedCornerShape(14.dp),
                            )
                            .padding(horizontal = 4.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SolarIcon(name = mk.icon, tint = mk.color, size = 22.dp)
                        Text(
                            text = mk.title,
                            color = if (active) mk.color else Letify.colors.text,
                            style = Letify.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        FieldLabel("Калории")
        TextInput(
            value = kcal,
            placeholder = "0 ккал",
            onValueChange = { kcal = it.filter { c -> c.isDigit() } },
            keyboardType = KeyboardType.Number,
        )

        FieldLabel("Б · Ж · У (грамм)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(value = protein, placeholder = "Белки", onValueChange = { protein = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            TextInput(value = fat, placeholder = "Жиры", onValueChange = { fat = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            TextInput(value = carb, placeholder = "Углеводы", onValueChange = { carb = it.filter { c -> c.isDigit() } }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
    }
}
