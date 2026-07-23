package com.letify.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.MediaItem
import com.letify.app.ui.theme.Letify

/**
 * Pinterest-style "Моменты" gallery.
 * Header floats with a soft gradient fade — grid scrolls underneath.
 */
@Composable
fun MediaScreen(
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
) {
    val state = LocalAppState.current
    val context = LocalContext.current
    LaunchedEffect(Unit) { state.reloadMedia(context.filesDir) }
    val items = state.mediaItems
    val bg = Letify.colors.bg

    Box(Modifier.fillMaxSize().background(bg)) {
        if (items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .background(Letify.colors.container, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        SolarIcon(
                            name = "gallery-bold-duotone",
                            tint = Letify.colors.muted,
                            size = 28.dp,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Пока пусто",
                        color = Letify.colors.text,
                        style = Letify.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Нажми + чтобы сделать фото\nили удерживай для видео",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    end = 10.dp,
                    top = 96.dp,
                    bottom = 110.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(items = items, key = { it.id }) { item ->
                    val ratio = item.aspectRatio.coerceIn(0.5f, 1.5f)
                    MediaCard(item = item, aspectRatio = ratio)
                }
            }
        }

        // Floating header — content scrolls under the gradient. Many stops
        // following a quadratic ease-out (alpha ∝ (1-t)²) so the fade reads
        // as one continuous soft haze with no visible seam, never darkens
        // past a light scrim (peak alpha 0.45, all the way down to fully
        // transparent by the bottom of the header) — not a hard curtain.
        Box(
            Modifier
                .fillMaxWidth()
                .zIndex(2f)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.000f to bg.copy(alpha = 0.45f),
                            0.125f to bg.copy(alpha = 0.344f),
                            0.250f to bg.copy(alpha = 0.253f),
                            0.375f to bg.copy(alpha = 0.176f),
                            0.500f to bg.copy(alpha = 0.113f),
                            0.625f to bg.copy(alpha = 0.063f),
                            0.750f to bg.copy(alpha = 0.028f),
                            0.875f to bg.copy(alpha = 0.007f),
                            1.000f to bg.copy(alpha = 0f),
                        ),
                    ),
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = 18.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoFeedbackButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SolarIcon(
                            name = "alt-arrow-left-outline",
                            tint = Letify.colors.text,
                            size = 28.dp,
                        )
                    }
                }
                Text(
                    "Моменты",
                    color = Letify.colors.text,
                    style = Letify.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        NoFeedbackButton(
            onClick = onOpenCamera,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp)
                .size(56.dp)
                .zIndex(3f),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Letify.colors.text, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SolarIcon(name = "add-bold", tint = Letify.colors.bg, size = 26.dp)
            }
        }
    }
}

@Composable
private fun MediaCard(item: MediaItem, aspectRatio: Float) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(Letify.colors.container),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(item.uri))
                .crossfade(160)
                .size(560)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    item.durationLabel.ifEmpty { "видео" },
                    color = Color.White,
                    style = Letify.typography.bodySmall,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
