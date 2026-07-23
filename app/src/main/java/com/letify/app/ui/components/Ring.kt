package com.letify.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify

/**
 * Sanitise a raw progress fraction for a ring. Guards the value/target divisions
 * used all over the app: a 0 target produces NaN (0/0) or +Inf (n/0), and a raw
 * NaN would otherwise sail through `coerceIn` (NaN comparisons are always false)
 * and break the arc / animation. NaN → 0, +Inf (over-goal with 0 target) → full.
 */
fun Float.safeRingFraction(): Float = when {
    isNaN() -> 0f
    this == Float.POSITIVE_INFINITY -> 1f
    else -> coerceIn(0f, 1f)
}

/** Single progress ring with rounded cap. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 138.dp,
    strokeWidth: Dp = 14.dp,
    @Suppress("UNUSED_PARAMETER") trackColor: Color? = null,
    // Default disc fill — bumped from 0.18 → 0.30 so rings read as
    // confident coloured objects on white surfaces instead of barely-
    // there pastel washes ("выцветшее" в светлой теме).
    discFillAlpha: Float = 0.30f,
) {
    // Seed the animation at the *current* value on (re)mount, then only
    // animate when the value actually changes. This kills the jarring
    // "re-fill from 0" that used to play every time a ring was remounted —
    // e.g. switching the Питание Вода/Еда tabs (AnimatedContent remounts the
    // pane). NaN/Inf inputs are sanitised so a 0 target can't break the arc.
    val sanitized = progress.safeRingFraction()
    val anim = remember { Animatable(sanitized) }
    LaunchedEffect(sanitized) {
        anim.animateTo(sanitized, animationSpec = tween(durationMillis = 700))
    }
    val animated = anim.value
    Canvas(modifier.size(size)) {
        val sw = strokeWidth.toPx()
        val inset = sw / 2f
        val arcSize = Size(this.size.width - sw, this.size.height - sw)
        val topLeft = Offset(inset, inset)
        // Soft tinted disc fill behind the ring — gives the habit ring a
        // gentle colour wash so the icon sits in a coloured object instead
        // of a grey donut. No grey track ring — we want a single rounded
        // progress arc on top of the filled disc, nothing more.
        if (discFillAlpha > 0f) {
            drawCircle(
                color = color.copy(alpha = discFillAlpha),
                radius = (this.size.width - sw) / 2f,
                center = Offset(this.size.width / 2f, this.size.height / 2f),
            )
        }
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
    }
}

/** Stacked multi-segment ring (used for macros). */
@Composable
fun StackedRing(
    segments: List<Pair<Color, Float>>, // color to length [0..1]
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp,
) {
    // Theme-aware track so the StackedRing reads correctly on both light and
    // dark backgrounds (used to be a hard-coded translucent white that was
    // invisible against the light theme).
    val track = Letify.colors.track
    Canvas(modifier.size(size)) {
        val sw = strokeWidth.toPx()
        val inset = sw / 2f
        val arcSize = Size(this.size.width - sw, this.size.height - sw)
        val topLeft = Offset(inset, inset)
        // Track
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw),
        )
        var startAngle = -90f
        segments.forEach { (color, len) ->
            val sweep = 360f * len.safeRingFraction()
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            startAngle += sweep
        }
    }
}
