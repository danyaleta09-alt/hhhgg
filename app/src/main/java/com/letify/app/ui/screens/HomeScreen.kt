package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.ProgressRing
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.state.LocalAppState
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
    onAddMeal: () -> Unit = {},
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
                    color = Letify.colors.text,
                    style = Letify.typography.titleMedium,
                )
                Text(
                    dateShort.replaceFirstChar { it.titlecase(Locale("ru")) },
                    color = Letify.colors.muted,
                    style = Letify.typography.bodySmall,
                )
            }
        },
    ) {
        Column(Modifier.screenHPad().padding(top = 4.dp, bottom = 4.dp)) {
            Text(greet, color = Letify.colors.muted, style = Letify.typography.bodyMedium)
            Text(
                state.userName.ifBlank { "друг" },
                color = Letify.colors.text,
                style = Letify.typography.displayLarge,
            )
        }

        // Ring — no container
        Column(
            Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = overall,
                    color = Letify.colors.accent,
                    size = 140.dp,
                    strokeWidth = 8.dp,
                    discFillAlpha = 0f,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(overall * 100).toInt()}%",
                        color = Letify.colors.text,
                        style = Letify.typography.displayLarge,
                        fontSize = 30.sp,
                    )
                    Text("день", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "прогресс за день",
                color = Letify.colors.muted,
                style = Letify.typography.bodySmall,
            )
        }

        // Compact cards: icon · bar · button
        Row(
            Modifier.screenHPad().fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CompactCard(
                modifier = Modifier.weight(1f),
                composition = sandwich,
                progress = foodProgress,
                barColor = LetifyColors.Cal,
                buttonLabel = "Питание",
                buttonColor = LetifyColors.Cal,
                onClick = onOpenNutrition,
            )
            CompactCard(
                modifier = Modifier.weight(1f),
                composition = coke,
                progress = waterProgress,
                barColor = LetifyColors.Water,
                buttonLabel = "+250 мл",
                buttonColor = LetifyColors.Water,
                onClick = { state.addWater(250, "Вода", "water") },
            )
        }

        Row(
            Modifier
                .screenHPad()
                .fillMaxWidth()
                .padding(top = 22.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Сон", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                Spacer(Modifier.height(3.dp))
                val sleep = state.sleepLog.maxByOrNull { it.dateKey }
                val sleepText = sleep?.let {
                    val m = it.durationMinutes
                    "${m / 60} ч ${m % 60} м"
                } ?: "—"
                NoFeedbackButton(onClick = onAddSleep) {
                    Text(sleepText, color = Letify.colors.text, style = Letify.typography.titleMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Вес", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                Spacer(Modifier.height(3.dp))
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

@Composable
private fun CompactCard(
    modifier: Modifier = Modifier,
    composition: com.airbnb.lottie.LottieComposition?,
    progress: Float,
    barColor: Color,
    buttonLabel: String,
    buttonColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Letify.colors.container)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Letify.colors.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(barColor, RoundedCornerShape(99.dp)),
            )
        }
        Spacer(Modifier.height(12.dp))
        NoFeedbackButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(buttonColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    buttonLabel,
                    color = Color.White,
                    style = Letify.typography.labelMedium,
                )
            }
        }
    }
}
