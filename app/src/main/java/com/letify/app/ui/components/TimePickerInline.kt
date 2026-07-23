package com.letify.app.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify

/**
 * Compact two-spinner time picker (HH : MM). Up/down chevrons wrap around,
 * minutes default to step of 5 so the user gets to 7:30 in 6 taps instead
 * of 30. The exposed [onChange] always emits a clamped, valid (h, m) pair.
 */
@Composable
fun TimePickerInline(
    hour: Int,
    minute: Int,
    minuteStep: Int = 5,
    onChange: (Int, Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Letify.colors.track, RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeSpinner(hour, 0, 23, 1) { onChange(it, minute) }
        Text(
            ":",
            color = Letify.colors.text,
            style = Letify.typography.displayMedium,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        TimeSpinner(minute, 0, 59, minuteStep) { onChange(hour, it) }
    }
}

@Composable
private fun TimeSpinner(value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SpinnerBtn(icon = "alt-arrow-down-outline") {
            // Wrap-around: from min, jump to the largest aligned multiple
            // ≤ max so the spinner cycles cleanly with no gap.
            val v = if (value - step < min) {
                val span = max - min + 1
                min + ((span - step) / step) * step
            } else value - step
            onChange(v.coerceIn(min, max))
        }
        Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
            Text(
                "%02d".format(value),
                color = Letify.colors.text,
                style = Letify.typography.displayMedium,
            )
        }
        SpinnerBtn(icon = "alt-arrow-up-outline") {
            val v = if (value + step > max) min else value + step
            onChange(v.coerceIn(min, max))
        }
    }
}

@Composable
private fun SpinnerBtn(icon: String, onClick: () -> Unit) {
    NoFeedbackButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Box(
            Modifier
                .size(36.dp)
                .background(Letify.colors.container, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = Letify.colors.text, size = 18.dp)
        }
    }
}
