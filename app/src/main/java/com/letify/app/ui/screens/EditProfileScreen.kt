package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.FieldLabel
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.OverlayScreen
import com.letify.app.ui.components.TextInput
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Gender
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    var name by remember { mutableStateOf(state.userName) }
    var ageText by remember { mutableStateOf(state.age.toString()) }
    var weightText by remember { mutableStateOf("%.1f".format(state.weight).replace(',', '.')) }
    var gender by remember { mutableStateOf(state.gender) }

    OverlayScreen(
        title = "Изменить профиль",
        onBack = onBack,
        primaryLabel = "Сохранить",
        primaryEnabled = name.isNotBlank(),
        onPrimary = {
            state.userName = name.trim()
            state.age = ageText.toIntOrNull()?.coerceIn(0, 120) ?: state.age
            state.weight = weightText.replace(',', '.').toFloatOrNull()?.coerceIn(20f, 400f) ?: state.weight
            state.gender = gender
            onBack()
        },
    ) {
        Box(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(108.dp)
                    .background(Letify.colors.accentSoft, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) {
                SolarIcon(name = "user-outline", tint = Letify.colors.accent, size = 56.dp)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .background(Letify.colors.accent, RoundedCornerShape(999.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "pen-outline", tint = androidx.compose.ui.graphics.Color.White, size = 18.dp)
                }
            }
        }

        FieldLabel("Имя")
        TextInput(value = name, placeholder = "Как вас зовут", onValueChange = { name = it })

        FieldLabel("Возраст")
        TextInput(
            value = ageText,
            placeholder = "22",
            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
            keyboardType = KeyboardType.Number,
        )

        FieldLabel("Вес, кг")
        TextInput(
            value = weightText,
            placeholder = "78.4",
            onValueChange = { input ->
                weightText = input.filter { c -> c.isDigit() || c == '.' || c == ',' }
            },
            keyboardType = KeyboardType.Decimal,
        )

        FieldLabel("Пол")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Gender.values().forEach { g ->
                val active = g == gender
                NoFeedbackButton(
                    onClick = { gender = g },
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                if (active) Letify.colors.accentSoft else Letify.colors.track,
                                RoundedCornerShape(14.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            g.title,
                            color = if (active) Letify.colors.accent else Letify.colors.text,
                            style = Letify.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}
