package com.letify.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.letify.app.ui.components.GoalProgressBar
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.SettingsCard
import com.letify.app.ui.components.SettingsRow
import com.letify.app.ui.components.SettingsRowDivider
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.calculateGoalProgress
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors

/**
 * Profile screen. Recently rebuilt around the user-approved HTML
 * prototype — the old four-up "Вес / Цель / Вода / Ккал" stat row was
 * replaced with a thick weighted-goal progress bar plus three rounded
 * quick-action buttons. The settings stack underneath stays untouched.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onGoals: () -> Unit = {},
    onAppearance: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onOther: () -> Unit = {},
    onProgressDetail: () -> Unit = {},
    onMedia: () -> Unit = {},
    onQuickCamera: () -> Unit = {},
    onQuickScan: () -> Unit = {},
    onQuickWeight: () -> Unit = {},
) {
    val state = LocalAppState.current
    val breakdown = calculateGoalProgress(state)
    val percent = (breakdown.overall * 100f).toInt()
    // Bounds of the "Камера" quick-action tile in root coordinates — the
    // camera container-transform grows from exactly this rect.

    ScreenScaffold(topPadding = 0.dp) {
        // Header row: back button (start) and edit-pencil (end) share one row,
        // flush with the status bar, like a typical screen header. Previously
        // these were two separate stacked rows, which dropped the pencil
        // ~40dp below the back arrow instead of aligning with it.
        Box(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .padding(top = 4.dp, bottom = 4.dp),
        ) {
            NoFeedbackButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("←", color = Letify.colors.text, style = Letify.typography.titleLarge)
            }
            NoFeedbackButton(
                onClick = onEditProfile,
                modifier = Modifier.align(Alignment.CenterEnd).size(44.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SolarIcon(name = "pen-outline", tint = Letify.colors.text, size = 24.dp)
                }
            }
        }

        // Avatar. Falls back to the accent-tinted user glyph when no
        // Telegram photo is bound — the fallback is painted first so the
        // empty slot never shows through.
        val tgUser = state.telegramUser
        val photoUrl = tgUser?.photoUrl
        val context = LocalContext.current
        // One more kick if we somehow still have a bound user without a photo
        // (e.g. previous session's fetch failed offline). Cheap no-op when the
        // URL is already present.
        LaunchedEffect(tgUser?.id, photoUrl) {
            if (tgUser != null && photoUrl.isNullOrBlank()) {
                state.fetchTelegramPhoto(tgUser.id)
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Letify.colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SolarIcon(name = "user-outline", tint = Letify.colors.accent, size = 46.dp)
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .crossfade(true)
                            .allowHardware(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        // Name + subtitle. `userName` is the single source of truth shared
        // between Telegram bind, profile editor and this header.
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                state.userName,
                color = Letify.colors.text,
                style = Letify.typography.headlineLarge,
            )
        }
        Box(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                "${state.age} лет · ${state.gender.title}",
                color = Letify.colors.muted,
                style = Letify.typography.bodyMedium,
            )
        }

        // ── Goal progress block ──────────────────────────────────────────
        //
        // Matches HTML prototype 1:1 — bare bar on background, then a
        // single centred caption "Цель достигнута на N%" (percent in
        // text/bold, prefix in muted). Тap anywhere in the block opens
        // the detail screen — the chevron/«подробнее» row was removed
        // per user request because the bar itself is the hit target.
        Box(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .noFeedbackClick(onClick = onProgressDetail),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GoalProgressBar(progress = breakdown.overall, modifier = Modifier.fillMaxWidth())
                Box(Modifier.height(10.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Letify.colors.muted)) {
                            append("Цель достигнута на ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Letify.colors.text,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append("$percent%")
                        }
                    },
                    style = Letify.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ── 3 quick action buttons ───────────────────────────────────────
        //
        // Spacing matches the prototype: 16dp top / 18dp bottom around
        // the row, 10dp gap between tiles, 62dp tile height, 22dp radius.
        Row(
            Modifier
                .fillMaxWidth()
                .screenHPad()
                .padding(top = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = "scanner-outline",
                label = "Сканер",
                onClick = onQuickScan,
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = "scale-outline",
                label = "Вес",
                onClick = onQuickWeight,
            )
            QuickAction(
                modifier = Modifier.weight(1f),
                icon = "camera-bold-duotone",
                label = "Камера",
                onClick = onQuickCamera,
            )
        }

        // ── Settings list ────────────────────────────────────────────────
        SettingsCard(
            modifier = Modifier.screenHPad(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            SettingsRow(
                icon = "target-bold",
                iconTile = LetifyColors.TileGreen,
                title = "Цели",
                onClick = onGoals,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = "moon-stars-bold",
                iconTile = LetifyColors.TileViolet,
                title = "Оформление",
                onClick = onAppearance,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = "bell-bold",
                iconTile = LetifyColors.TileRed,
                title = "Уведомления",
                onClick = onNotifications,
            )
            SettingsRowDivider()
            SettingsRow(
                icon = "gallery-bold-duotone",
                iconTile = LetifyColors.TilePink,
                title = "Моменты",
                onClick = onMedia,
            )
        }

        Box(Modifier.height(18.dp))
        SettingsCard(
            modifier = Modifier.screenHPad(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            SettingsRow(
                icon = "menu-dots-bold",
                iconTile = LetifyColors.TileBlue,
                title = "Другое",
                onClick = onOther,
            )
        }
    }
}

/**
 * Single rounded quick-action tile. Centred icon over a small label —
 * matches the proto. Buttons are visually unified (same height /
 * radius / background) so the row reads as a connected control strip
 * rather than three disparate chips.
 */
@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    onClick: () -> Unit,
) {
    // Soft "squat" on press: a gentle, springy scale-down that eases
    // back. Subtle on purpose (0.95) — reads as tactile, not bouncy.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "quickActionSquat",
    )
    Box(
        modifier
            .scale(scale)
            .height(62.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Letify.colors.container, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SolarIcon(name = icon, tint = Letify.colors.accent, size = 24.dp)
            Box(Modifier.height(4.dp))
            Text(
                label,
                color = Letify.colors.muted,
                style = Letify.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
