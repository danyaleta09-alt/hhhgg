package com.letify.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.ScreenHeader
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.SectionTitle
import com.letify.app.ui.components.SegItem
import com.letify.app.ui.components.SegmentedTabs
import com.letify.app.ui.components.StackedRing
import com.letify.app.ui.components.WCard
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun NutritionScreen(onAddMeal: () -> Unit = {}, onWaterHistory: () -> Unit = {}) {
    var tab by remember { mutableStateOf("water") }

    ScreenScaffold(
        pinnedHeader = {
            ScreenHeader(
                title = "питание",
                trailingIcon = "add-bold",
                trailingAccent = true,
                onTrailingClick = onAddMeal,
            )
        }
    ) {
        Box(Modifier.screenHPad()) {
            SegmentedTabs(
                items = listOf(
                    SegItem("water", "Вода", "bottle-bold-duotone"),
                    SegItem("food", "Еда", "apple-bold-duotone"),
                ),
                selected = tab,
                onSelect = { tab = it },
            )
        }
        Box(Modifier.height(12.dp))
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInHorizontally(tween(260)) { it / 12 })
                    .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 24 })
            },
            label = "nutrition_pane"
        ) { current ->
            if (current == "water") WaterPane(onHistory = onWaterHistory) else FoodPane(onAddMeal = onAddMeal)
        }
    }
}

// ── Water pane ──────────────────────────────────────────────────────────────

@Composable
private fun WaterPane(onHistory: () -> Unit = {}) {
    val state = LocalAppState.current
    // Default amount — reset back here after each successful add.
    val defaultAmountMl = 250
    var amountMl by remember { mutableIntStateOf(defaultAmountMl) }

    Column {
        // Filling glass hero
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                WaterGlass(
                    progress = (state.waterMl.toFloat() / state.waterTarget).coerceIn(0f, 1.15f),
                    modifier = Modifier.size(width = 130.dp, height = 170.dp),
                )
                Box(Modifier.height(10.dp))
                Text(
                    "${state.waterMl}",
                    color = Letify.colors.text,
                    style = Letify.typography.displayLarge,
                )
                Text(
                    "из ${state.waterTarget} мл",
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
        }

        SectionTitle("Добавить")

        // Amount slider + add button
        Column(
            Modifier.screenHPad(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            WaterAmountSlider(
                valueMl = amountMl,
                onValueChange = { amountMl = it },
                minMl = 50,
                maxMl = 1000,
                stepMl = 50,
            )
            NoFeedbackButton(
                onClick = {
                    state.addWater(amountMl, labelFor(amountMl), iconFor(amountMl))
                    // Reset slider so the user can't spam the same amount.
                    amountMl = defaultAmountMl
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(LetifyColors.Water, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Добавить · $amountMl мл",
                        color = Color.White,
                        style = Letify.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        SectionTitle("История")
        WCard(
            modifier = Modifier.screenHPad().noFeedbackClick(onClick = onHistory),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(LetifyColors.Water.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "calendar-bold-duotone", tint = LetifyColors.Water, size = 20.dp)
                }
                Box(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Вся история", color = Letify.colors.text, style = Letify.typography.titleSmall)
                    Text(
                        "Записи по дням и статистика",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodySmall,
                    )
                }
                SolarIcon(name = "alt-arrow-right-outline", tint = Letify.colors.muted, size = 18.dp)
            }
        }
    }
}

private fun labelFor(ml: Int): String = when {
    ml <= 150 -> "Глоток"
    ml <= 300 -> "Стакан воды"
    ml <= 550 -> "Бутылка"
    else -> "Большая бутылка"
}

private fun iconFor(ml: Int): String = when {
    ml <= 150 -> "waterdrop-outline"
    ml <= 300 -> "cup-paper-bold-duotone"
    else -> "bottle-bold-duotone"
}

/**
 * Water level as a clean vertical capsule — no fake glass rim, no 3D shell.
 * Empty track is a soft wash of the water color; fill rises from the bottom
 * with a gentle curved surface. Works in light and dark themes.
 */
@Composable
private fun WaterGlass(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val sanitized = progress.coerceIn(0f, 1.25f)
    val anim = remember { Animatable(sanitized) }
    LaunchedEffect(sanitized) {
        anim.animateTo(sanitized, animationSpec = tween(durationMillis = 600))
    }
    val fill = anim.value.coerceIn(0f, 1f)
    val water = LetifyColors.Water
    val isDark = Letify.colors.isDark
    val track = if (isDark) water.copy(alpha = 0.12f) else water.copy(alpha = 0.10f)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Tall rounded capsule (stadium)
        val padX = w * 0.22f
        val padY = h * 0.04f
        val left = padX
        val right = w - padX
        val top = padY
        val bot = h - padY
        val radius = (right - left) / 2f

        val vessel = Path().apply {
            addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bot,
                    cornerRadius = CornerRadius(radius, radius),
                ),
            )
        }

        // Empty track
        drawPath(vessel, color = track)

        if (fill > 0.001f) {
            val waterTop = bot - (bot - top) * fill
            clipPath(vessel) {
                // Body
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            water.copy(alpha = 0.75f),
                            water,
                            Color(0xFF3A8FE0),
                        ),
                        startY = waterTop,
                        endY = bot,
                    ),
                    topLeft = Offset(left, waterTop),
                    size = Size(right - left, bot - waterTop + 1f),
                )
            }
        }
    }
}

/**
 * Smooth amount slider.
 *
 * Drag uses a plain Float (zero-lag, no coroutines). A spring Animatable
 * runs only on finger-up and on external reset — never during the gesture.
 */
@Composable
private fun WaterAmountSlider(
    valueMl: Int,
    onValueChange: (Int) -> Unit,
    minMl: Int = 50,
    maxMl: Int = 1000,
    stepMl: Int = 50,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val water = LetifyColors.Water
    val trackColor = Letify.colors.container
    val scope = rememberCoroutineScope()

    fun mlToFrac(ml: Int): Float =
        ((ml - minMl).toFloat() / (maxMl - minMl).toFloat()).coerceIn(0f, 1f)

    fun fracToMl(f: Float): Int {
        val raw = minMl + f.coerceIn(0f, 1f) * (maxMl - minMl)
        val stepped = ((raw - minMl) / stepMl.toFloat()).roundToInt() * stepMl + minMl
        return stepped.coerceIn(minMl, maxMl)
    }

    // Live drag position (sync writes).
    var dragFrac by remember { mutableFloatStateOf(mlToFrac(valueMl)) }
    // Spring position (async, only for settle / reset).
    val springFrac = remember { Animatable(mlToFrac(valueMl)) }
    var dragging by remember { mutableStateOf(false) }

    // What we actually render.
    val fraction = if (dragging) dragFrac else springFrac.value
    val displayMl = fracToMl(fraction)

    // Soft spring when parent resets amount (after add).
    LaunchedEffect(valueMl) {
        if (!dragging) {
            val target = mlToFrac(valueMl)
            if (kotlin.math.abs(target - springFrac.value) > 0.0005f) {
                springFrac.animateTo(target, spring(dampingRatio = 0.82f, stiffness = 140f))
                dragFrac = springFrac.value
            } else {
                dragFrac = target
                springFrac.snapTo(target)
            }
        }
    }

    // Tell parent about stepped value changes (for the "Добавить" label).
    LaunchedEffect(displayMl) {
        if (displayMl != valueMl) onValueChange(displayMl)
    }

    val bubbleH = 30.dp
    val tailH = 8.dp
    val gap = 8.dp
    val trackH = 28.dp
    val totalH = bubbleH + tailH + gap + trackH

    Box(modifier.fillMaxWidth().height(totalH)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(totalH)
                .pointerInput(minMl, maxMl, stepMl) {
                    val endR = trackH.toPx() / 2f
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            val travel = (size.width - endR * 2f).coerceAtLeast(1f)
                            dragFrac = ((offset.x - endR) / travel).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val travel = (size.width - endR * 2f).coerceAtLeast(1f)
                            dragFrac = ((change.position.x - endR) / travel).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val snapped = fracToMl(dragFrac)
                            val target = mlToFrac(snapped)
                            // Hand off to spring, then release the drag flag so
                            // render switches to springFrac mid-animation.
                            scope.launch {
                                springFrac.snapTo(dragFrac)
                                dragging = false
                                springFrac.animateTo(
                                    target,
                                    spring(dampingRatio = 0.78f, stiffness = 120f),
                                )
                                dragFrac = springFrac.value
                                onValueChange(snapped)
                            }
                        },
                        onDragCancel = {
                            dragging = false
                        },
                    )
                },
        ) {
            val w = size.width
            val endR = trackH.toPx() / 2f
            val knobR = 10.dp.toPx()
            val travel = (w - endR * 2f).coerceAtLeast(1f)
            val knobCx = endR + fraction * travel
            val trackTop = (bubbleH + tailH + gap).toPx()
            val trackCy = trackTop + trackH.toPx() / 2f

            // Bubble
            val bW = 74.dp.toPx()
            val bH = bubbleH.toPx()
            val bR = 15.dp.toPx()
            val bLeft = (knobCx - bW / 2f).coerceIn(0f, (w - bW).coerceAtLeast(0f))
            val bBottom = bH
            val tipX = knobCx.coerceIn(bLeft + bR + 4.dp.toPx(), bLeft + bW - bR - 4.dp.toPx())
            val half = 7.dp.toPx()
            val overlap = 2.5.dp.toPx()

            drawRoundRect(
                color = water,
                topLeft = Offset(bLeft, 0f),
                size = Size(bW, bH),
                cornerRadius = CornerRadius(bR, bR),
            )
            drawPath(
                Path().apply {
                    moveTo(tipX - half, bBottom - overlap)
                    lineTo(tipX, bBottom + tailH.toPx())
                    lineTo(tipX + half, bBottom - overlap)
                    close()
                },
                color = water,
            )

            // Track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, trackTop),
                size = Size(w, trackH.toPx()),
                cornerRadius = CornerRadius(endR, endR),
            )
            // Fill
            val fillW = (knobCx + endR).coerceIn(endR * 2f, w)
            drawRoundRect(
                color = water,
                topLeft = Offset(0f, trackTop),
                size = Size(fillW, trackH.toPx()),
                cornerRadius = CornerRadius(endR, endR),
            )
            // Knob
            drawCircle(Color.White, radius = knobR, center = Offset(knobCx, trackCy))
        }

        // Label over bubble
        var boxW by remember { mutableFloatStateOf(0f) }
        val endRpx = with(density) { (trackH / 2).toPx() }
        Box(
            Modifier
                .fillMaxWidth()
                .height(bubbleH)
                .onSizeChanged { boxW = it.width.toFloat() },
        ) {
            if (boxW > 0f) {
                val travel = (boxW - endRpx * 2f).coerceAtLeast(1f)
                val knobCx = endRpx + fraction * travel
                val bWpx = with(density) { 74.dp.toPx() }
                val bLeft = (knobCx - bWpx / 2f).coerceIn(0f, (boxW - bWpx).coerceAtLeast(0f))
                Box(
                    Modifier
                        .offset { IntOffset(bLeft.roundToInt(), 0) }
                        .width(74.dp)
                        .height(bubbleH),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$displayMl мл",
                        color = Color.White,
                        style = Letify.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

// ── Food pane ───────────────────────────────────────────────────────────────

@Composable
private fun FoodPane(onAddMeal: () -> Unit = {}) {
    val state = LocalAppState.current
    Column {
        Box(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                val pLen = (state.protein.toFloat() / state.proteinTarget).coerceIn(0f, 1f) * 0.33f
                val fLen = (state.fat.toFloat() / state.fatTarget).coerceIn(0f, 1f) * 0.33f
                val cLen = (state.carb.toFloat() / state.carbTarget).coerceIn(0f, 1f) * 0.33f
                StackedRing(
                    segments = listOf(
                        LetifyColors.Protein to pLen,
                        LetifyColors.Fat to fLen,
                        LetifyColors.Carb to cLen,
                    ),
                    size = 200.dp,
                    strokeWidth = 14.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.kcal.toString(), color = Letify.colors.text, style = Letify.typography.displayLarge)
                    Text(
                        "из ${state.kcalTarget} ккал",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodySmall,
                    )
                }
            }
        }
        Box(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().screenHPad(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MacroRow("Белки", state.protein, state.proteinTarget, "г", LetifyColors.Protein)
            MacroRow("Жиры", state.fat, state.fatTarget, "г", LetifyColors.Fat)
            MacroRow("Углеводы", state.carb, state.carbTarget, "г", LetifyColors.Carb)
        }
        SectionTitle("Сегодня")
        WCard(modifier = Modifier.screenHPad(), contentPadding = PaddingValues(8.dp)) {
            if (state.meals.isEmpty()) {
                EmptyHint("Пока нет приёмов пищи — добавь первый через «+» вверху")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.meals.forEach { meal ->
                        MealRow(
                            meal.title,
                            meal.icon,
                            meal.color,
                            meal.kcal,
                            meal.description ?: "",
                            onAdd = onAddMeal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Letify.colors.muted,
            style = Letify.typography.bodySmall,
        )
    }
}

@Composable
private fun MacroRow(label: String, value: Int, target: Int, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(999.dp)))
        Box(Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value.toString(), color = Letify.colors.text, style = Letify.typography.titleMedium)
                Text(" / $target $unit", color = Letify.colors.muted, style = Letify.typography.bodySmall)
            }
            Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
    }
}

@Composable
private fun MealRow(
    title: String,
    icon: String,
    color: Color,
    kcal: Int?,
    description: String,
    onAdd: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = color, size = 22.dp)
        }
        Box(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Letify.colors.text, style = Letify.typography.titleSmall)
            Text(
                if (kcal != null) "$description · $kcal ккал" else description,
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }
        NoFeedbackButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
            SolarIcon(name = "add-circle-bold-duotone", tint = Letify.colors.accent, size = 22.dp)
        }
    }
}
