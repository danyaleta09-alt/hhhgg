package com.letify.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.MediaItem
import com.letify.app.ui.state.TaskStatus
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onAddWeight: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onAddSleep: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAddMeal: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenMoments: () -> Unit = {},
) {
    val state = LocalAppState.current
    val context = LocalContext.current
    val overall = state.overallProgress()
    val name = state.userName.ifBlank { "друг" }
    val photoUrl = state.telegramUser?.photoUrl
    val letter = name.firstOrNull()?.uppercase() ?: "?"

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

    val moments = state.mediaItems.take(3)
    val tasks = state.tasksToday()
    val nowMin = LocalTime.now().toSecondOfDay() / 60
    val dateKey = com.letify.app.ui.state.Dates.todayKey()
    val done = tasks.count { it.statusAt(nowMin, dateKey) == TaskStatus.Done }
    val planPreview = tasks.take(3).joinToString(" · ") { it.name }.ifBlank { "нет задач" }

    Column(
        Modifier
            .fillMaxSize()
            .background(Letify.colors.bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 6.dp, bottom = 24.dp),
    ) {
        // Header → profile
        NoFeedbackButton(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .screenHPad()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Letify.colors.accent, LetifyColors.TilePink)),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(photoUrl).crossfade(180).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    name,
                    color = Letify.colors.text,
                    style = Letify.typography.titleLarge,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Text(
                    dateShort.replaceFirstChar { it.titlecase(Locale("ru")) },
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
        }

        // Progress carousel — fixed height, no parent scroll
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (page == 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DayRing(overall, 140.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("прогресс за день", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                        }
                    } else {
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
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Выпей воды — до цели осталось пол-литра. После ужина лучше лёгкая прогулка.",
                                color = Letify.colors.text,
                                style = Letify.typography.titleSmall,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("советы появятся здесь", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(2) { i ->
                val on = pagerState.currentPage == i
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (on) 16.dp else 6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (on) Letify.colors.accent else Color.White.copy(alpha = 0.15f)),
                )
            }
        }

        // Moments — compact
        NoFeedbackButton(
            onClick = onOpenMoments,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Letify.colors.container)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MomentsStack(moments)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Моменты", color = Letify.colors.text, style = Letify.typography.titleSmall)
                    Text(
                        if (moments.isEmpty()) "добавь первый"
                        else "${state.mediaItems.size} в ленте",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodySmall,
                    )
                }
                Text("›", color = Letify.colors.muted, style = Letify.typography.titleMedium)
            }
        }

        // Plan — compact row
        NoFeedbackButton(
            onClick = onOpenPlan,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Letify.colors.container)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Letify.colors.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("☰", color = Letify.colors.accent, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("План", color = Letify.colors.text, style = Letify.typography.titleSmall)
                    Text(
                        planPreview,
                        color = Letify.colors.muted,
                        style = Letify.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "$done / ${tasks.size}",
                    color = Letify.colors.accent,
                    style = Letify.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Letify.colors.accent.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        // Food + Water
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                composition = sandwich,
                title = "Питание",
                progress = foodProgress,
                label = "${state.kcal} из ${state.kcalTarget} ккал",
                color = LetifyColors.Cal,
                onAdd = onOpenNutrition,
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                composition = coke,
                title = "Вода",
                progress = waterProgress,
                label = formatWater(state.waterMl, state.waterTarget),
                color = LetifyColors.Water,
                onAdd = { state.addWater(250, "Вода", "water") },
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Сон", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                val sleep = state.sleepLog.maxByOrNull { it.dateKey }
                val sleepText = sleep?.let { "${it.durationMinutes / 60} ч ${it.durationMinutes % 60} м" } ?: "—"
                NoFeedbackButton(onClick = onAddSleep) {
                    Text(sleepText, color = Letify.colors.text, style = Letify.typography.titleMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Вес", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                NoFeedbackButton(onClick = onAddWeight) {
                    Text(
                        String.format("%.1f кг", state.weight).replace('.', ','),
                        color = Letify.colors.text,
                        style = Letify.typography.titleMedium,
                    )
                }
            }
        }
    }
}

/** 3 stacked thumbs. Empty → first is «+», rest silhouettes. */
@Composable
private fun MomentsStack(items: List<MediaItem>) {
    val rotations = listOf(-7f, 2f, 8f)
    val offsets = listOf(0.dp, 14.dp, 28.dp)
    Box(Modifier.width(68.dp).height(44.dp)) {
        // back → front so z-order is correct
        for (i in 2 downTo 0) {
            val item = items.getOrNull(i)
            val isAdd = items.isEmpty() && i == 0
            Box(
                Modifier
                    .padding(start = offsets[i])
                    .size(width = 36.dp, height = 44.dp)
                    .zIndex((3 - i).toFloat())
                    .rotate(rotations[i])
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            item != null -> Letify.colors.track
                            isAdd -> Letify.colors.accent.copy(alpha = 0.18f)
                            else -> Color.White.copy(alpha = 0.06f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    item != null && !item.isVideo -> {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    item != null -> {
                        Text("▶", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    isAdd -> {
                        Text("+", color = Letify.colors.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRing(progress: Float, size: Dp) {
    val track = Letify.colors.track
    val accent = Letify.colors.accent
    val p = progress.coerceIn(0f, 1f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = 8.dp.toPx()
            val inset = sw / 2f
            val arc = Size(this.size.width - sw, this.size.height - sw)
            val origin = Offset(inset, inset)
            drawArc(track, -90f, 360f, false, origin, arc, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(accent, -90f, 360f * p, false, origin, arc, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Text("${(p * 100).toInt()}%", color = Letify.colors.text, style = Letify.typography.displayLarge, fontSize = 30.sp)
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    composition: LottieComposition?,
    title: String,
    progress: Float,
    label: String,
    color: Color,
    onAdd: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Letify.colors.container)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = Letify.colors.text, style = Letify.typography.titleSmall, maxLines = 1)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Letify.colors.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color, RoundedCornerShape(99.dp)),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        NoFeedbackButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text("Добавить", color = Color.White, style = Letify.typography.labelMedium)
            }
        }
    }
}

private fun formatWater(ml: Int, target: Int): String {
    if (target >= 1000 || ml >= 1000) {
        fun fmt(v: Float) = String.format("%.2f", v).trimEnd('0').trimEnd('.', ',').replace('.', ',')
        return "${fmt(ml / 1000f)} из ${fmt(target / 1000f)} л"
    }
    return "$ml мл из $target"
}
