package com.letify.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.letify.app.telegram.TelegramAuth
import com.letify.app.ui.components.PrimaryActionButton
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.noFeedbackClick
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * "Привязки" screen.
 *
 * Three states cross-fade through a single sealed-interface state machine:
 *   * [BindingUi.Idle]    — TG plate at rest, "Привязать" CTA + help card.
 *   * [BindingUi.Polling] — plate scales down slightly, a single rounded
 *     arc spins smoothly around it. No other motion (no waves / pulses /
 *     content snapping).
 *   * [BindingUi.Bound]   — plate with a green check, name + @username,
 *     red "Отвязать".
 *
 * The bound branch carries the [TelegramAuth.TelegramUser] snapshot so an
 * outgoing fade-out frame can still read it after [unbindTelegram] has
 * cleared AppState — otherwise the previous frame would NPE on the way out.
 */
@Composable
fun BindingsScreen(onBack: () -> Unit) {
    val state = LocalAppState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var sessionToken by remember { mutableStateOf<String?>(null) }
    var pollJob by remember { mutableStateOf<Job?>(null) }
    val polling by remember {
        derivedStateOf { sessionToken != null && pollJob?.isActive == true }
    }

    DisposableEffect(Unit) {
        onDispose { pollJob?.cancel() }
    }

    val startBinding: () -> Unit = {
        pollJob?.cancel()
        val fresh = TelegramAuth.newSessionToken()
        sessionToken = fresh
        TelegramAuth.openBot(context, fresh)
        pollJob = scope.launch {
            var offset = 0L
            while (isActive) {
                when (val result = TelegramAuth.pollOnce(fresh, offset)) {
                    is TelegramAuth.PollResult.Found -> {
                        state.bindTelegram(result.user)
                        sessionToken = null
                        // Resolve profile photo URL asynchronously so the UI
                        // doesn't wait on a second HTTP round trip before
                        // flipping into bound state. Runs on AppState's own
                        // long-lived scope (not this screen's) so it isn't
                        // cancelled if the user navigates away right after
                        // binding, before the fetch has finished.
                        state.fetchTelegramPhoto(result.user.id)
                        return@launch
                    }
                    is TelegramAuth.PollResult.NotFound -> {
                        offset = result.nextOffset
                    }
                    is TelegramAuth.PollResult.Error -> {
                        delay(2000)
                    }
                }
            }
        }
    }

    val cancelBinding: () -> Unit = {
        pollJob?.cancel()
        pollJob = null
        sessionToken = null
    }

    val unbind: () -> Unit = {
        pollJob?.cancel()
        pollJob = null
        sessionToken = null
        state.unbindTelegram()
    }

    val ui: BindingUi = when {
        state.telegramUser != null -> BindingUi.Bound(state.telegramUser!!)
        polling -> BindingUi.Polling
        else -> BindingUi.Idle
    }

    Box(Modifier.fillMaxSize().background(Letify.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 6.dp, bottom = 60.dp),
        ) {
            SettingsHeader(title = "Привязки", onBack = onBack)

            Spacer(Modifier.height(14.dp))
            TelegramHero(
                bound = ui is BindingUi.Bound,
                polling = ui is BindingUi.Polling,
                photoUrl = state.telegramUser?.photoUrl,
            )

            Spacer(Modifier.height(18.dp))

            AnimatedContent(
                targetState = ui,
                transitionSpec = {
                    fadeIn(tween(220)).togetherWith(fadeOut(tween(160)))
                },
                contentKey = { it.key },
                label = "binding-ui",
            ) { snapshot ->
                when (snapshot) {
                    BindingUi.Idle -> IdleBody(onBind = startBinding)
                    BindingUi.Polling -> PollingBody(onCancel = cancelBinding)
                    is BindingUi.Bound -> BoundBody(user = snapshot.user, onUnbind = unbind)
                }
            }
        }
    }
}

private sealed interface BindingUi {
    val key: Any

    data object Idle : BindingUi {
        override val key: Any = "idle"
    }

    data object Polling : BindingUi {
        override val key: Any = "polling"
    }

    data class Bound(val user: TelegramAuth.TelegramUser) : BindingUi {
        override val key: Any = "bound"
    }
}

/**
 * Static-sized hero box. The plate inside scales down slightly when
 * [polling] and a slow custom progress ring fades in around it — the only
 * motion on the screen while we wait for Telegram. No pulse rings, no
 * size jumps in the parent (the outer Box keeps a constant footprint so
 * neighbouring layout doesn't shift when the plate animates).
 */
@Composable
private fun TelegramHero(bound: Boolean, polling: Boolean, photoUrl: String? = null) {
    // Plate scales to 120 * 0.86 ≈ 103dp; ring at 128dp gives a ~12.5dp
    // halo around the shrunken plate — the "чуть совсем больше” gap.
    val frameSize = 140.dp
    val plateRest = 120.dp
    val ringSize = 128.dp
    val context = LocalContext.current
    val plateScale by animateFloatAsState(
        targetValue = if (polling) 0.86f else 1f,
        animationSpec = tween(durationMillis = 320, easing = EaseInOutCubic),
        label = "plate-scale",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (polling) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = EaseInOutCubic),
        label = "ring-alpha",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(frameSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(frameSize),
            contentAlignment = Alignment.Center,
        ) {
            SlowSpinner(
                size = ringSize,
                alpha = ringAlpha,
                stroke = 5.dp,
                color = LetifyColors.TelegramBlue,
            )

            // Plate. graphicsLayer scale so the gradient and glyph shrink
            // as one unit, leaving the icon centred at all times. Always
            // painted first — it's the fallback while the real Telegram
            // photo (below) is still loading, or when the user has none.
            Box(
                Modifier
                    .size(plateRest)
                    .graphicsLayer {
                        scaleX = plateScale
                        scaleY = plateScale
                    }
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LetifyColors.TelegramBlue,
                                LetifyColors.TelegramBlueDeep,
                            ),
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SolarIcon(
                    name = "telegram-plane",
                    tint = Color.White,
                    size = 72.dp,
                )
            }

            // Once bound, the user's real Telegram profile photo crossfades
            // in on top of the plate — previously this hero always kept
            // showing the generic Telegram logo, so the avatar never
            // appeared to have been fetched even after a successful bind.
            if (bound && photoUrl != null) {
                Box(
                    Modifier
                        .size(plateRest)
                        .graphicsLayer {
                            scaleX = plateScale
                            scaleY = plateScale
                        }
                        .clip(CircleShape),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(photoUrl)
                            .crossfade(260)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            if (bound) {
                // The badge sits at the bottom-right corner of the rest-size
                // plate. Padding is computed off the plate radius so it
                // stays anchored even when the frame size changes.
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .padding(
                            start = (plateRest.value * 0.66f).dp,
                            top = (plateRest.value * 0.66f).dp,
                        )
                        .size(34.dp)
                        .background(Letify.colors.bg, CircleShape)
                        .padding(3.dp)
                        .background(LetifyColors.TileGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SolarIcon(name = "check-bold", tint = Color.White, size = 18.dp)
                }
            }
        }
    }
}

/**
 * Slow, uniformly-spinning progress arc.
 *
 * The previous implementation used the Material-3 indeterminate
 * algorithm (linear rotation + keyframed head/tail trim) which on
 * every cycle visibly shrinks the arc to almost nothing and then
 * "jumps" the visible head to a new position — the bug the user
 * called out as "уменьшается почти в точку, а потом чото резко
 * меняет положение, дёргается".
 *
 * For a "waiting for confirmation" indicator we don't need the M3
 * grow-shrink behaviour; a constant-length arc rotating at a steady
 * rate reads much more calmly. We draw a 270° arc and rotate it
 * linearly once every [rotationDurationMs] — no head/tail keyframes,
 * no jump, no perceived stutter at cycle boundary because the start
 * and end of a 360° linear rotation are visually identical.
 */
@Composable
private fun SlowSpinner(
    size: androidx.compose.ui.unit.Dp,
    alpha: Float,
    stroke: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    val rotationDurationMs = 1800
    val arcSweepDegrees = 270f

    val transition = rememberInfiniteTransition(label = "slow-spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Canvas(
        modifier = Modifier
            .size(size)
            .graphicsLayer { this.alpha = alpha },
    ) {
        if (alpha <= 0f) return@Canvas
        val px = stroke.toPx()
        val inset = px / 2f
        drawArc(
            color = color,
            // -90 lines the arc start with 12 o'clock, then the linear
            // rotation carries it around. The start and end of every
            // cycle map to the same angle, so the seam is invisible.
            startAngle = rotation - 90f,
            sweepAngle = arcSweepDegrees,
            useCenter = false,
            style = Stroke(width = px, cap = StrokeCap.Round),
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - px, this.size.height - px),
        )
    }
}

@Composable
private fun IdleBody(onBind: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Telegram",
            color = Letify.colors.text,
            style = Letify.typography.headlineLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Привяжите аккаунт, чтобы получать напоминания и сводки прямо в чате с ботом.",
            color = Letify.colors.muted,
            style = Letify.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, end = 36.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.screenHPad()) {
            PrimaryActionButton(label = "Привязать", onClick = onBind)
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.screenHPad()) {
            HelpCard()
        }
    }
}

@Composable
private fun PollingBody(onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Ожидание подтверждения",
            color = Letify.colors.text,
            style = Letify.typography.headlineLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Откройте Telegram и нажмите «Старт» в чате с ботом.",
            color = Letify.colors.muted,
            style = Letify.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, end = 36.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.screenHPad()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        Letify.colors.container,
                        RoundedCornerShape(18.dp),
                    )
                    .noFeedbackClick(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Отменить",
                    color = Letify.colors.text,
                    style = Letify.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun BoundBody(
    user: TelegramAuth.TelegramUser,
    onUnbind: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            user.displayName,
            color = Letify.colors.text,
            style = Letify.typography.headlineLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        user.username?.let { uname ->
            Spacer(Modifier.height(4.dp))
            Text(
                "@$uname",
                color = Letify.colors.muted,
                style = Letify.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(LetifyColors.TileGreen, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Привязано",
                color = LetifyColors.TileGreen,
                style = Letify.typography.titleSmall,
            )
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.screenHPad()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        LetifyColors.TileRed.copy(alpha = 0.14f),
                        RoundedCornerShape(18.dp),
                    )
                    .noFeedbackClick(onClick = onUnbind),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Отвязать",
                    color = LetifyColors.TileRed,
                    style = Letify.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun HelpCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Letify.colors.container, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SolarIcon(
                    name = "info-circle-bold-duotone",
                    tint = Letify.colors.accent,
                    size = 22.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Как это работает",
                    color = Letify.colors.text,
                    style = Letify.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            HelpItem(
                step = "1",
                text = "Нажмите «Привязать» — откроется бот @${TelegramAuth.BOT_USERNAME}",
            )
            HelpItem(
                step = "2",
                text = "В Telegram нажмите «Старт» внизу чата",
            )
            HelpItem(
                step = "3",
                text = "Бот пришлёт подтверждение — вернитесь в приложение",
            )
        }
    }
}

@Composable
private fun HelpItem(step: String, text: String) {
    Row(
        Modifier.padding(top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .background(Letify.colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                step,
                color = Letify.colors.accent,
                style = Letify.typography.labelSmall,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = Letify.colors.muted,
            style = Letify.typography.bodyMedium,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}
