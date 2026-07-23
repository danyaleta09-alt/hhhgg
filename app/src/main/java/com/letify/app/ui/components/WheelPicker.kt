package com.letify.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letify.app.ui.theme.Letify
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * Cyclic iOS-style wheel/drum picker.
 *
 * Internally we render an "infinite" virtual list (~1B virtual rows)
 * and translate each virtual index into a real one via `index mod N`,
 * so the user can scroll up or down forever — `1·2·3·…·9·1·2·…`
 * loops cleanly in both directions without ever hitting an edge.
 *
 * Two perf details that made the previous version feel laggy ("барабан
 * подлагивает / вращается непоароно") and that this revision fixes:
 *
 *  1. Per-row visual state (alpha + scale based on distance from the
 *     column centre) used to be read in composition via
 *     `derivedStateOf` + plain Kotlin val, then captured in the
 *     `graphicsLayer { … }` modifier lambda. That forced a recomposition
 *     of every visible row on **every** scroll position change. We now
 *     read `listState.layoutInfo` *inside* the `graphicsLayer` lambda
 *     itself, so the GPU layer parameters update on each frame without
 *     ever recomposing the row's content — same visual, far less work.
 *
 *  2. The previous implementation rendered the centre row with a
 *     "selected" typography style and other rows with a smaller one,
 *     swapping styles on every snap-fling tick. That caused a hard
 *     re-layout on every centre change. We now use a single typography
 *     style and let the smooth `scaleX/scaleY` in graphicsLayer do the
 *     "bigger when centred" effect analogically.
 *
 * Snap-fling makes the inertial scroll always land a row exactly in the
 * middle slot, and haptic ticks fire on every index change just like
 * the iOS spinner. The currently-selected value is reported through
 * [onSelected] each time the centre row changes (initial value is
 * suppressed).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    values: List<T>,
    initialIndex: Int,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleItems: Int = 5,
    textStyle: TextStyle = Letify.typography.headlineLarge,
    selectedTextStyle: TextStyle = Letify.typography.displayMedium,
    onSelected: (index: Int, value: T) -> Unit,
    label: (T) -> String,
) {
    require(visibleItems % 2 == 1) { "visibleItems must be odd so a row sits dead centre" }
    require(values.isNotEmpty()) { "values must be non-empty" }
    // `selectedTextStyle` is intentionally accepted but unused now —
    // the centre row uses the same style and just grows analogically.
    @Suppress("UNUSED_PARAMETER") selectedTextStyle.toString()

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    val itemHeightPx = with(density) { itemHeight.toPx() }
    val totalHeight = itemHeight * visibleItems

    val n = values.size

    // Virtual cyclic list: ~1B rows. LazyColumn only materialises
    // visible rows, so the up-front cost is identical to a finite list.
    // Start near the middle so the user has roughly the same scrollable
    // distance up and down before reaching the (unreachable in practice)
    // edge.
    // Integer half-count of rows on EACH side of the centre row.
    // With visibleItems=5 the centred row sits at offset 2 from the
    // first visible row (rows 0,1,[2],3,4 fill the box top-to-bottom
    // because contentPadding is zero). This is the same constant used
    // by both the initial-scroll positioning AND the "what's centred
    // now" derived state — keep them in sync or the picker reports a
    // different value than the user sees (the bug that was causing the
    // reminder time, weight wheel, habit target etc. to commit a value
    // shifted by `halfItems` rows from what was visually centred).
    val halfItems = visibleItems / 2

    val virtualCount = remember { Int.MAX_VALUE / 4 }
    val midBlock = remember(n, halfItems) {
        val mid = virtualCount / 2
        // We want the row whose real-mod index equals `initialIndex`
        // to land in the CENTRE of the box. Centre row = first visible
        // + halfItems, so the first visible must equal initialIndex -
        // halfItems (mod n).
        val firstReal = (((initialIndex.coerceIn(0, n - 1) - halfItems) % n) + n) % n
        mid - (mid % n) + firstReal
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = midBlock)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)

    // Selected index = the row whose centre is closest to the column's
    // centre. Centre row = firstVisible + halfItems; if the first-
    // visible row has scrolled past its halfway mark, the row BELOW
    // the geometric centre is the snapped one. Then mod into the real
    // values range.
    //
    // Previously this formula omitted the `+ halfItems` term, so the
    // wheel reported the topmost visible (faded) row as "selected" —
    // for a 5-row wheel resting on `11`, the commit was `09`, which
    // is exactly the symptom the user reported (picker visually at
    // 11:03, parent settings row showed 09:01).
    val selectedIndex by remember(n, halfItems) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val candidate = first + halfItems + if (offset > itemHeightPx / 2f) 1 else 0
            ((candidate % n) + n) % n
        }
    }

    // `onSelected` must be read through rememberUpdatedState: this collector
    // lives in a LaunchedEffect keyed on the STABLE `values`/`haptics`, so it
    // never restarts and would otherwise capture the FIRST `onSelected` lambda
    // forever. When two wheels (e.g. start + end time) each commit onto a draft
    // captured at first composition, only the last-touched field survived — the
    // other reverted to its initial value. Reading the latest lambda fixes that.
    val currentOnSelected by rememberUpdatedState(onSelected)
    LaunchedEffect(values, haptics) {
        snapshotFlow { selectedIndex }
            .drop(1)
            .distinctUntilChanged()
            .collect { idx ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (idx in values.indices) currentOnSelected(idx, values[idx])
            }
    }

    val half = (visibleItems / 2f).coerceAtLeast(1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.Center,
    ) {
        // No selection pill — iOS-style wheel: the centre row reads as
        // "selected" purely through its larger/bolder type and the fact
        // that adjacent rows fade and shrink away. A boxed background
        // behind the centre row looks disconnected (user feedback).

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            LazyColumn(
                state = listState,
                flingBehavior = fling,
                // No vertical content padding — the cyclic list has no
                // edges to leave headroom for, and padding plus snap-fling
                // produces an extra subtle "jump" on the first/last item
                // approach because the snapper has to reconcile the
                // padding offset with the snap offset every fling.
                contentPadding = PaddingValues(vertical = 0.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val mask = Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.18f to Color.Black.copy(alpha = 0.35f),
                            0.34f to Color.Black.copy(alpha = 0.85f),
                            0.50f to Color.Black,
                            0.66f to Color.Black.copy(alpha = 0.85f),
                            0.82f to Color.Black.copy(alpha = 0.35f),
                            1.00f to Color.Transparent,
                        )
                        drawRect(brush = mask, blendMode = BlendMode.DstIn)
                    },
            ) {
                items(count = virtualCount, key = { it }) { virtualIdx ->
                    val realIdx = ((virtualIdx % n) + n) % n
                    WheelRow(
                        text = label(values[realIdx]),
                        height = itemHeight,
                        textStyle = textStyle,
                        listState = listState,
                        virtualIndex = virtualIdx,
                        itemHeightPx = itemHeightPx,
                        half = half,
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelRow(
    text: String,
    height: Dp,
    textStyle: TextStyle,
    listState: LazyListState,
    virtualIndex: Int,
    itemHeightPx: Float,
    half: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                // Defer the state reads to the graphicsLayer phase: this
                // runs on every frame the scroll position changes
                // **without** triggering recomposition of the row's
                // Text() content. That's the difference between a wheel
                // that visibly chops (recomposing every frame) and a
                // wheel that glides (just updating a transform).
                val info = listState.layoutInfo
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val item = info.visibleItemsInfo.firstOrNull { it.index == virtualIndex }
                val d: Float = if (item == null) {
                    Float.POSITIVE_INFINITY
                } else {
                    abs(item.offset + item.size / 2f - viewportCenter) / itemHeightPx
                }
                val t = (d / half).coerceIn(0f, 1f)
                // Quadratic falloff: centre stays crisp, edges drop off
                // without a hard transition. Combined with the DstIn
                // mask above we get the "smoothly fades out at the
                // borders" feel.
                alpha = (1f - t * t).coerceIn(0f, 1f)
                val s = 1f + (0.58f - 1f) * t
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = Letify.colors.text,
        )
    }
}

// Suppress unused warning about [max] / [floor] kept around for future
// callers experimenting with non-cyclic flavours.
@Suppress("unused") private val _keep = max(0, floor(0.0).toInt())
