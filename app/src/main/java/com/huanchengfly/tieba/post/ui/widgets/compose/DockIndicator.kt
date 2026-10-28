package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import kotlin.math.roundToInt

/** 指示器速度形变：归一化槽位速度 → scaleX 拉伸 / scaleY 压扁（约 ±20% 封顶）。 */
internal const val DockVelocityDivisor = 10f
internal const val DockVelocityScaleXMultiplier = 0.75f
internal const val DockVelocityClamp = 0.2f

/** 按下 bloom：78/56（MeiloX pressedScale，静止 56dp 基高）。 */
internal const val DockIndicatorPressScale = 78f / 56f

/**
 * 选中指示器（MeiloX 关闭液态玻璃后的形态）：
 * 不采样背景（无折射/模糊/增饱和），静止只有 10% 黑/白纯色填充；
 * 按下/拖动时 bloom，并叠加 3% 压暗、白色高光边（alpha=(0.54+0.38p)·p）
 * 与底边内阴影，与 MeiloX 的 Highlight/InnerShadow 一致。
 */
@Composable
internal fun DockIndicator(
    positionPx: Float,
    widthPx: Float,
    height: Dp,
    pressedScaleX: Float,
    pressedScaleY: Float,
    velocity: Float,
    pressProgress: Float,
    panelOffsetPx: () -> Float,
    isNightMode: Boolean,
    indicatorAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val press = pressProgress.coerceIn(0f, 1f)
    val fill = if (isNightMode) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .offset {
                IntOffset((positionPx + panelOffsetPx()).roundToInt(), 0)
            }
            .width(with(LocalDensity.current) { widthPx.toDp() })
            .height(height)
            .graphicsLayer {
                alpha = indicatorAlpha
            }
            .graphicsLayer {
                val termX = (velocity / DockVelocityDivisor * DockVelocityScaleXMultiplier)
                    .coerceIn(-DockVelocityClamp, DockVelocityClamp)
                val termY = (velocity / DockVelocityDivisor * 0.25f)
                    .coerceIn(-DockVelocityClamp, DockVelocityClamp)
                scaleX = pressedScaleX / (1f - termX)
                scaleY = pressedScaleY * (1f - termY)
            }
            .clip(CircleShape)
            .drawBackdrop(
                // 液态玻璃关闭：不采样背景，避免胶囊在内容多变的页面之间呈现「玻璃窗」差异
                backdrop = emptyBackdrop(),
                shape = { CircleShape },
                effects = {},
                highlight = {
                    // MeiloX 指示器 Highlight：(0.54 + 0.38p) × p
                    Highlight.Default.copy(
                        alpha = (0.54f + 0.38f * press) * press,
                        style = HighlightStyle.Default(angle = 90f),
                    )
                },
                shadow = null,
                onDrawSurface = {
                    drawRect(color = fill)
                    if (press > 0f) {
                        drawRect(color = Color.Black.copy(alpha = 0.03f * press))
                        val outline = CircleShape.createOutline(size, layoutDirection, this)
                        // MeiloX InnerShadow：按下时底边内侧压暗
                        drawOutlineStroke(
                            outline = outline,
                            brush = Brush.verticalGradient(
                                0.5f to Color.Black.copy(alpha = 0f),
                                1f to Color.Black.copy(alpha = 0.18f * press),
                            ),
                            width = 6.dp.toPx(),
                        )
                    }
                },
            )
    )
}

/** 指示器垂直居中，无描边不再需要视觉补偿。 */
internal val DockIndicatorLift = 0.dp
