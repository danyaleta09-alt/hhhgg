package com.letify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import com.letify.app.ui.theme.LetifyTheme

/**
 * Reusable fully-rounded progress bar with smooth colour interpolation
 * from red → orange → yellow → mint as the user closes in on the goal.
 *
 * The colour gradient is intentionally NOT bound to [Letify.colors.accent]
 * because progress communicates *health of the goal*, not brand. Even when
 * the user picks a different accent for the rest of the app, a 90 % goal
 * still reads as "green = good". The reached state lands on
 * [LetifyColors.Mint] which is the most universally legible "done" hue.
 *
 * Stops match the HTML prototype that was reviewed and approved:
 *   0 %   Red    #F06262
 *   35 %  Amber  #FFB347
 *   55 %  Yellow #EFCF4A
 *   100 % Mint   #7CD992
 *
 * The bar animates both width and colour. Disable [animated] for static
 * @Preview / paginated lists where motion would be distracting.
 */
@Composable
fun GoalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 18.dp,
    animated: Boolean = true,
) {
    val target = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animated) tween(durationMillis = 700, easing = EaseOutCubic)
                        else tween(durationMillis = 0),
        label = "goal-progress-width",
    )

    val targetColor = progressColor(target)
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = if (animated) tween(durationMillis = 500) else tween(durationMillis = 0),
        label = "goal-progress-color",
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .clip(RoundedCornerShape(50))
            .background(Letify.colors.track),
    ) {
        // Only paint the fill when there's at least a sliver of progress —
        // a 0 px width Box on a rounded clip would still render the radius
        // arcs on some devices, leaving a thin coloured smudge at the
        // start of the track. Hiding the fill at <0.5 % avoids that.
        if (animatedProgress > 0.005f) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                animatedColor,
                                lerp(animatedColor, Color.White, 0.18f),
                            ),
                        ),
                    ),
            )
        }
    }
}

/**
 * Maps progress (0..1) to a colour by piecewise-linear interpolation
 * between four stops. Exposed so screens that need just the colour
 * (e.g. the headline percentage tint) can stay perfectly in sync with
 * the bar without re-implementing the table.
 */
fun progressColor(progress: Float): Color {
    val p = progress.coerceIn(0f, 1f)
    val red = Color(0xFFF06262)
    val amber = Color(0xFFFFB347)
    val yellow = Color(0xFFEFCF4A)
    val mint = Color(0xFF7CD992)
    return when {
        p <= 0.35f -> lerp(red, amber, p / 0.35f)
        p <= 0.55f -> lerp(amber, yellow, (p - 0.35f) / 0.20f)
        else -> lerp(yellow, mint, (p - 0.55f) / 0.45f)
    }
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(name = "Dark – 14%", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GoalProgressBarPreviewDark14() {
    LetifyTheme {
        Box(Modifier.background(Letify.colors.bg).fillMaxWidth()) {
            GoalProgressBar(progress = 0.14f, modifier = Modifier.fillMaxWidth(), animated = false)
        }
    }
}

@Preview(name = "Dark – 50%", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GoalProgressBarPreviewDark50() {
    LetifyTheme {
        Box(Modifier.background(Letify.colors.bg).fillMaxWidth()) {
            GoalProgressBar(progress = 0.50f, modifier = Modifier.fillMaxWidth(), animated = false)
        }
    }
}

@Preview(name = "Dark – 92%", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GoalProgressBarPreviewDark92() {
    LetifyTheme {
        Box(Modifier.background(Letify.colors.bg).fillMaxWidth()) {
            GoalProgressBar(progress = 0.92f, modifier = Modifier.fillMaxWidth(), animated = false)
        }
    }
}

@Preview(name = "Light – 50%", showBackground = true, backgroundColor = 0xFFF1F1F3)
@Composable
private fun GoalProgressBarPreviewLight50() {
    LetifyTheme(mode = com.letify.app.ui.theme.ThemeMode.Light) {
        Box(Modifier.background(Letify.colors.bg).fillMaxWidth()) {
            GoalProgressBar(progress = 0.50f, modifier = Modifier.fillMaxWidth(), animated = false)
        }
    }
}
