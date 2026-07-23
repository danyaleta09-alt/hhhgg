package com.letify.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify

/**
 * Like [OverlayScreen] but adds a 3-segment progress bar and step label
 * under the title row. Used by AddHabitWizard / AddTaskWizard.
 *
 * Header layout (top → bottom):
 *   • back arrow + title (54.dp)
 *   • 3 thin progress segments (≈ 12.dp incl. padding)
 *   • step label like "ШАГ 1 ИЗ 3 · ЧТО И СКОЛЬКО" (≈ 26.dp incl. padding)
 *
 * The pinned header stays transparent and content scrolls behind it just
 * like OverlayScreen — see that file for the full architecture comment.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WizardScaffold(
    title: String,
    stepCount: Int,
    currentStep: Int,                // 1-based
    stepLabel: String,
    onBack: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    showSecondary: Boolean = false,
    secondaryLabel: String = "",
    onSecondary: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val elastic = rememberElasticOverscroll()
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Letify.colors.bg)
                .nestedScroll(elastic.connection),
        ) {
            val density = LocalDensity.current
            val imeBottomPx = WindowInsets.ime.getBottom(density)
            val isImeVisible = imeBottomPx > 0
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scroll)
                    .graphicsLayer {
                        // See OverlayScreen for the rationale behind suppressing
                        // the elastic translation while the IME is visible.
                        translationY = if (isImeVisible) 0f else elastic.verticalOverscroll.floatValue
                    }
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(HEADER_TOTAL_HEIGHT_DP))
                content()
                Spacer(modifier = Modifier.height(if (showSecondary) 168.dp else 120.dp))
            }

            // ── Pinned header (transparent) ────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .padding(top = 4.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NoFeedbackButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                SolarIcon(
                                    name = "alt-arrow-left-outline",
                                    tint = Letify.colors.text,
                                    size = 28.dp,
                                )
                            }
                        }
                        Box(Modifier.width(4.dp))
                        Text(
                            text = title,
                            color = Letify.colors.text,
                            style = Letify.typography.headlineMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    ProgressSegments(stepCount = stepCount, current = currentStep)
                    Box(Modifier.height(8.dp))
                    Text(
                        text = stepLabel.uppercase(),
                        color = Letify.colors.muted,
                        style = Letify.typography.labelMedium,
                        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                    )
                }
            }

            // ── Floating primary (+ optional secondary "Назад") ────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrimaryActionButton(
                    label = primaryLabel,
                    enabled = primaryEnabled,
                    onClick = onPrimary,
                )
                if (showSecondary) {
                    NoFeedbackButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                secondaryLabel,
                                color = Letify.colors.muted,
                                style = Letify.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressSegments(stepCount: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(stepCount) { idx ->
            val done = idx < current
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (done) Letify.colors.accent else Letify.colors.track,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

/**
 * Total pinned-header height excluding status bar:
 *   54 (title row) + 4 (progress strip incl padding) + 8 (gap) + 16 (label
 *   incl padding) = ~82dp. Rounded up to give breathing room on smaller
 *   fonts.
 */
private val HEADER_TOTAL_HEIGHT_DP = 96.dp
