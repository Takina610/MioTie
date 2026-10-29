package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.page.main.NavigationItem
import kotlin.math.roundToInt

/** 图标尺寸（对齐 MeiloX ExpandedNavigationIconSize）。 */
internal val DockItemIconSize = 28.dp

/** 名称字号（对齐 MeiloX NavigationLabelStyle）。 */
private val DockItemLabelSize = 10.sp

/**
 * 导航条目（对齐 MeiloX NavigationTabContent）：图标 + 名称常显；
 * 选中色只由选中态决定，按压时向前景色回退，下沉时再回退到前景色。
 *
 * @param compact 下沉进度（0..1）：名称高度与透明度一起收掉
 * @param pressProgress 选中指示器的按压进度（0..1）：选中图标/名称随之退成前景色
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
internal fun DockTab(
    navigationItem: NavigationItem,
    selected: Boolean,
    compact: Float,
    alpha: Float,
    accent: Color,
    baseColor: Color,
    pressProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val press = pressProgress.coerceIn(0f, 1f)
    val compactFraction = compact.coerceIn(0f, 1f)
    val color = if (selected) {
        lerp(lerp(accent, baseColor, press), baseColor, compactFraction)
    } else baseColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            // 下沉态四个条目叠在同一位置：靠 zIndex 保证当前页图标在最上层
            .zIndex(if (selected) 1f else 0f)
            .graphicsLayer { this.alpha = alpha },
    ) {
        Box {
            Icon(
                painter = rememberAnimatedVectorPainter(
                    animatedImageVector = navigationItem.icon(),
                    atEnd = selected,
                ),
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .padding(top = 3.dp * (1f - compact))
                    .size(DockItemIconSize),
            )
            if (navigationItem.badge) {
                DockBadge(
                    text = navigationItem.badgeText ?: "",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer {
                            translationX = 8.dp.toPx()
                            translationY = (-6).dp.toPx()
                        },
                )
            }
        }
        Text(
            text = navigationItem.title(selected),
            maxLines = 1,
            fontSize = DockItemLabelSize,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .graphicsLayer { this.alpha = 1f - compact }
                .layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints())
                    val visibleHeight = (placeable.height * (1f - compact)).roundToInt()
                    layout(placeable.width, visibleHeight) {
                        placeable.place(0, (visibleHeight - placeable.height) / 2)
                    }
                },
        )
    }
}

@Composable
private fun DockBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(15.dp)
            .widthIn(min = 15.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.secondary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            maxLines = 1,
            fontSize = 9.sp,
            color = White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 3.dp),
        )
    }
}
