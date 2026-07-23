package com.letify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Manrope
import com.letify.app.ui.theme.Letify

@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        color = Letify.colors.muted,
        style = Letify.typography.titleSmall,
        modifier = Modifier.padding(bottom = 6.dp, start = 6.dp, top = 6.dp),
    )
}

@Composable
fun TextInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Letify.colors.track, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = Letify.colors.muted, style = Letify.typography.bodyMedium)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Manrope,
                fontSize = 15.sp,
                color = Letify.colors.text,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(Letify.colors.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Two-row grid of icon picker cells. Each cell is equal-width via weight,
 * fixed height for visual uniformity.
 */
@Composable
fun IconCellPicker(
    icons: List<String>,
    selected: String,
    tint: Color,
    onSelect: (String) -> Unit,
) {
    val rows = icons.chunked(4)
    rows.forEachIndexed { i, row ->
        if (i > 0) Box(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { ic ->
                IconCell(ic, selected == ic, tint, Modifier.weight(1f)) { onSelect(ic) }
            }
            // Pad missing cells so the last partial row still aligns with the
            // grid above.
            val missing = 4 - row.size
            repeat(missing) {
                Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IconCell(icon: String, active: Boolean, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    NoFeedbackButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    if (active) tint.copy(alpha = 0.16f) else Letify.colors.track,
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = if (active) tint else Letify.colors.text, size = 22.dp)
        }
    }
}
