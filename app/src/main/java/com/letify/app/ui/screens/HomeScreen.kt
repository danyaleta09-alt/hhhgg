package com.letify.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** Blocks vertical nested-scroll from parent while user is paging horizontally. */
private val BlockVerticalNestedScroll = object : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        available // consume leftover vertical fling so parent doesn't jump
}

@Composable
fun HomeScreen(
    onAddWeight: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onAddSleep: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAddMeal: () -> Unit = {},
) {
    val state = LocalAppState.current
    val overall = state.overallProgress()
    val hour = LocalTime.now().hour
    val greet = when {
        hour < 6 -> "Доброй ночи"
        hour < 12 -> "Доброе утро"
        hour < 18 -> "Добрый день"
        else -> "Добрый вечер"
    }
    val today = LocalDate.now()
    val dateShort = buildString {
        append(today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru")))
        append(", ")
        append(today.dayOfMonth)
        append(" ")
        append(today.month.getDisplayName(TextStyle.FULL, Locale("ru")))
    }

    val waterProgress = (state.waterMl.toFloat() / state.waterTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
    val foodProgress = (state.kcal.toFloat() / state.kcalTarget.coerceAtLeast(1)).coerceIn(0f, 1f)

    val sandwich by rememberLottieComposition(LottieCompositionSpec.Asset("stickers/sandwich.json"))
    val coke by rememberLottieComposition(LottieCompositionSpec.Asset("stickers/coke.json"))

    val pagerState = rememberPagerState(pageCount = { 2 })

    ScreenScaffold(
        pinnedHeader = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .screenHPad()
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "летифай",
                    color = Letify.colors.muted,
                    style = Letify.typography.titleSmall,
                )
                Text(
                    dateShort.replaceFirstChar { it.titlecase(Locale("ru")) },
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
        },
    ) {
        // Greeting — same scale as prototype
        Column(Modifier.screenHPad().padding(top = 10.dp, bottom = 6.dp)) {
            Text(
                greet,
                color = Letify.colors.muted,
                style = Letify.typography.bodyLarge,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.userName.ifBlank { "друг" },
                color = Letify.colors.text,
                style = Letify.typography.displayLarge,
                fontSize = 32.sp,
            )
        }

        // Fixed-height pager — no height jump, no vertical scroll steal
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .nestedScroll(BlockVerticalNestedScroll),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (page) {
                        0 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                DayRing(progress = overall, size = 148.dp)
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "прогресс за день",
                                    color = Letify.colors.muted,
                                    style = Letify.typography.bodyMedium,
                                )
                            }
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            ) {
                                Text(
                                    "ИИ · скоро",
                                    color = Letify.colors.accent,
                                    style = Letify.typography.labelMedium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Letify.colors.accent.copy(alpha = 0.14f))
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Выпей воды — до цели осталось пол-литра. После ужина лучше лёгкая прогулка.",
                                    color = Letify.colors.text,
                                    style = Letify.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "советы появятся здесь",
                                    color = Letify.colors.muted,
                                    style = Letify.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dots
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) { i ->
                val on = pagerState.currentPage == i
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (on) 18.dp else 6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (on) Letify.colors.accent
                            else Color.White.copy(alpha = 0.18f),
                        ),
                )
            }
        }

        // Cards — larger, matching prototype proportions
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompactCard(
                modifier = Modifier.weight(1f),
                composition = sandwich,
                title = "Питание",
                progress = foodProgress,
                progressLabel = "${state.kcal} из ${state.kcalTarget} ккал",
                barColor = LetifyColors.Cal,
                buttonColor = LetifyColors.Cal,
                onAdd = onOpenNutrition,
            )
            CompactCard(
                modifier = Modifier.weight(1f),
                composition = coke,
                title = "Вода",
                progress = waterProgress,
                progressLabel = formatWater(state.waterMl, state.waterTarget),
                barColor = LetifyColors.Water,
                buttonColor = LetifyColors.Water,
                onAdd = { state.addWater(250, "Вода", "water") },
            )
        }

        // Sleep / weight
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Сон", color = Letify.colors.muted, style = Letify.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                val sleep = state.sleepLog.maxByOrNull { it.dateKey }
                val sleepText = sleep?.let {
                    val m = it.durationMinutes
                    "${m / 60} ч ${m % 60} м"
                } ?: "—"
                NoFeedbackButton(onClick = onAddSleep) {
                    Text(sleepText, color = Letify.colors.text, style = Letify.typography.titleLarge)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Вес", color = Letify.colors.muted, style = Letify.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                NoFeedbackButton(onClick = onAddWeight) {
                    Text(
                        String.format("%.1f кг", state.weight).replace('.', ','),
                        color = Letify.colors.text,
                        style = Letify.typography.titleLarge,
                    )
                }
            }
        }
    }
}

private fun formatWater(ml: Int, target: Int): String {
    // Prefer liters when values are large; keep readable
    return if (target >= 1000 || ml >= 1000) {
        val cur = String.format("%.2f", ml / 1000f).trimEnd('0').trimEnd('.', ',').replace('.', ',')
        val goal = String.format("%.1f", target / 1000f).trimEnd('0').trimEnd('.', ',').replace('.', ',')
        "$cur из $goal л"
    } else {
        "$ml мл из $target"
    }
}

@Composable
private fun DayRing(progress: Float, size: Dp) {
    val track = Letify.colors.track
    val accent = Letify.colors.accent
    val p = progress.coerceIn(0f, 1f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = 9.dp.toPx()
            val inset = sw / 2f
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Text(
            "${(p * 100).toInt()}%",
            color = Letify.colors.text,
            style = Letify.typography.displayLarge,
            fontSize = 32.sp,
        )
    }
}

@Composable
private fun CompactCard(
    modifier: Modifier = Modifier,
    composition: LottieComposition?,
    title: String,
    progress: Float,
    progressLabel: String,
    barColor: Color,
    buttonColor: Color,
    onAdd: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Letify.colors.container)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                color = Letify.colors.text,
                style = Letify.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Letify.colors.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(7.dp)
                    .background(barColor, RoundedCornerShape(99.dp)),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            progressLabel,
            color = Letify.colors.muted,
            style = Letify.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        NoFeedbackButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(buttonColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Добавить",
                    color = Color.White,
                    style = Letify.typography.labelLarge,
                )
            }
        }
    }
}
