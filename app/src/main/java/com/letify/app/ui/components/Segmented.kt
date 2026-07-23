package com.letify.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify

data class SegItem(val key: String, val title: String, val icon: String? = null)

/**
 * Compact segmented control. Height and padding intentionally slim so the
 * control doesn't dominate the screen the way the old 44dp pill did.
 */
@Composable
fun SegmentedTabs(
    items: List<SegItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
) {
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val itemWidthDp = with(density) { (size.width / items.size.coerceAtLeast(1)).toDp() }
    val selectedIndex = items.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    val offsetX by animateDpAsState(
        targetValue = itemWidthDp * selectedIndex,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "segx",
    )
    Box(
        modifier
            .fillMaxWidth()
            .background(Letify.colors.container, RoundedCornerShape(999.dp))
            .padding(3.dp)
            .onSizeChanged { size = it }
    ) {
        if (size != IntSize.Zero) {
            Box(
                Modifier
                    .offset(x = offsetX, y = 0.dp)
                    .width(itemWidthDp)
                    .height(height)
                    .background(Letify.colors.accentSoft, RoundedCornerShape(999.dp))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { item ->
                val active = item.key == selected
                NoFeedbackButton(
                    onClick = { onSelect(item.key) },
                    modifier = Modifier.weight(1f).height(height)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (item.icon != null) {
                            SolarIcon(
                                name = item.icon,
                                tint = if (active) Letify.colors.accent else Letify.colors.muted,
                                size = 16.dp,
                            )
                            Box(Modifier.size(6.dp))
                        }
                        Text(
                            item.title,
                            color = if (active) Letify.colors.accent else Letify.colors.muted,
                            style = Letify.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}
