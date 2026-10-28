package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.page.main.NavigationItem
import kotlin.math.roundToInt

/** 图标尺寸。 */
internal val DockItemIconSize = 24.dp

/** 名称字号。 */
private val DockItemLabelSize = 12.sp

/** 按下时条目图标放大比例（1 + 0.2）。 */
private const val DockTabPressScaleExtra = 0.2f

/**
 * 导航条目：图标 + 名称常显；配色由指示器位置驱动（拖拽经过时实时渐变到强调色）。
 *
 * @param progress 与指示器的接近度（0..1），驱动文字/图标配色
 * @param pressed 是否处于按压态（图标单独放大）
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
internal fun DockTab(
    navigationItem: NavigationItem,
    selected: Boolean,
    progress: Float,
    compact: Float,
    alpha: Float,
    accent: Color,
    baseColor: Color,
    pressed: Boolean,
    modifier: Modifier = Modifier,
) {
    // 下沉态不显示「选中样式」：配色回落到普通前景色
    val color = lerp(baseColor, accent, progress * (1f - compact))
    // 被按下的条目图标单独放大（spring，与其他条目互不影响）
    val iconScale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        iconScale.animateTo(
            targetValue = if (pressed) 1f + DockTabPressScaleExtra else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f),
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
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
                    .size(DockItemIconSize)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
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
        // 名称常显；下沉时高度与透明度一起收掉，图标回到胶囊正中
        Text(
            text = navigationItem.title(selected),
            maxLines = 1,
            fontSize = DockItemLabelSize,
            fontWeight = FontWeight.SemiBold,
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