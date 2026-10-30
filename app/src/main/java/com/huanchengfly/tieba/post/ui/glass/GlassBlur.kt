package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * [Modifier.blur] 的延迟取值版本，图层参数与 Compose 的 blur 完全一致
 * （BlurEffect(px, px, TileMode.Clamp) + RectangleShape + clip），
 * 只是把半径放到绘制阶段读取，折叠动画期间不必每滚一个像素就重组。
 */
internal fun Modifier.blurBy(radius: () -> Dp): Modifier = graphicsLayer {
    val px = radius().toPx()
    renderEffect = if (px > 0f) BlurEffect(px, px, TileMode.Clamp) else null
    shape = RectangleShape
    clip = true
}
