package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.roundToInt

/** 指示器轮廓高光线宽度。 */
private val IndicatorRimWidth = 1.dp

/** 指示器轮廓内侧光带宽度。 */
private val IndicatorRimGlowWidth = 2.dp

/** 指示器视觉居中补偿：描边上下不对称导致视觉重心略沉，几何上移抵消。 */
private val IndicatorVisualLift = 0.5.dp

/** 指示器外圈微光晕分层（壳同款技法，层数略少、跨度略窄）。 */
private val IndicatorHaloLayers = listOf(
    0.75f to 0.30f, 1.5f to 0.22f, 2.25f to 0.16f, 3f to 0.117f,
    3.75f to 0.085f, 4.5f to 0.062f,
)

/** 指示器速度形变：归一化槽位速度 → scaleX 拉伸 / scaleY 压扁（约 ±20% 封顶）。 */
internal const val DockVelocityDivisor = 10f
internal const val DockVelocityScaleXMultiplier = 0.75f
internal const val DockVelocityClamp = 0.2f

/** 按下 bloom 目标高度 78dp / 静止 56dp（≈1.39×）。 */
internal const val DockIndicatorPressScale = 78f / 56f

/**
 * 选中指示器（玻璃透镜胶囊）：跟随拖拽位置、按下 bloom（可越界超出壳）、速度形变、外圈光晕。
 *
 * 与壳体同材质：模糊采样 + 壳底色 + 指示色，越界放大时呈现与壳一致的玻璃质感。
 */
@Composable
internal fun DockIndicator(
    positionPx: Float,
    widthPx: Float,
    height: Dp,
    pressedScaleX: Float,
    pressedScaleY: Float,
    velocity: Float,
    panelOffsetPx: () -> Float,
    isNightMode: Boolean,
    hazeState: HazeState,
    blurSupported: Boolean,
    surfaceTint: Color,
    indicatorAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val indicatorColor = if (isNightMode) Color.White else Color.Black
    val shape = CircleShape
    // 按压进度（0..1）：驱动填充淡出、顶部高光增强与光晕增强
    val bloomProgress =
        ((pressedScaleX - 1f) / (DockIndicatorPressScale - 1f)).coerceIn(0f, 1f)
    // 三层轮廓光（对齐壳体：外圈微光晕 + 顶部高光弧 + 底部次级弧）。
    // 浅色：顶部白高光在玻璃材质上可见；底部暗边勾勒边界；夜间：双峰白色反光。
    // 按压时高光增强，边缘随按压「亮起」强化透镜感
    val rimTopAlpha = ((if (isNightMode) 0.25f else 0.80f) + 0.10f * bloomProgress)
        .coerceAtMost(1f)
    val rimBottomColor = if (isNightMode) Color.White else Color.Black
    val rimBottomAlpha = if (isNightMode) 0.10f else 0.12f
    // 外圈微光晕：浅色贴边暗晕勾勒边界，夜间贴边发光（壳同款）
    val haloColor = if (isNightMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)
    val rimBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = rimTopAlpha),
        0.35f to Color.White.copy(alpha = 0f),
        0.65f to rimBottomColor.copy(alpha = 0f),
        1f to rimBottomColor.copy(alpha = rimBottomAlpha),
    )
    val rimGlowBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = rimTopAlpha * 0.2f),
        0.35f to Color.White.copy(alpha = 0f),
        0.65f to rimBottomColor.copy(alpha = 0f),
        1f to rimBottomColor.copy(alpha = rimBottomAlpha * 0.2f),
    )
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
            .drawBehind {
                // 外圈微光晕：多层同心描边向外递减（壳同款，避免单层实线「套圈」感）
                IndicatorHaloLayers.forEach { (widthDp, factor) ->
                    val wPx = widthDp.dp.toPx()
                    val half = wPx / 2f
                    drawRoundRect(
                        color = haloColor.copy(alpha = haloColor.alpha * factor * (1f + 2f * bloomProgress)),
                        topLeft = Offset(-half, -half),
                        size = Size(size.width + wPx, size.height + wPx),
                        cornerRadius = CornerRadius(size.height / 2f + half),
                        style = Stroke(width = wPx),
                    )
                }
            }
            .clip(shape)
            .then(
                // 玻璃透镜：指示器自身采样背景模糊，按压放大越出壳体的部分也能透出模糊内容
                if (blurSupported) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurEffect {
                            style = HazeBlurStyle(
                                backgroundColor = Color.Transparent,
                                colorEffects = emptyList(),
                                blurRadius = 10.dp,
                                noiseFactor = 0f,
                                fallbackColorEffect = HazeColorEffect.tint(Color.Transparent),
                            )
                            blurEnabled = true
                            blurredEdgeTreatment = BlurredEdgeTreatment(shape)
                        }
                    }
                } else Modifier
            )
            .drawBehind {
                val cornerRadius = CornerRadius(size.height / 2f)
                // 壳体同款底色打底：指示器=「模糊(内容)+壳底色+指示色」，等效于壳上叠一块玻璃透镜
                drawRoundRect(color = surfaceTint, cornerRadius = cornerRadius)
                // 选中指示色：静止时 10% 托底；按压时淡出至 3% 残量（透镜仍可辨）
                val fillAlpha = if (blurSupported) {
                    0.10f * (1f - 0.7f * bloomProgress)
                } else 0.10f
                drawRoundRect(
                    color = indicatorColor.copy(alpha = fillAlpha),
                    cornerRadius = cornerRadius,
                )
                drawRoundRect(
                    brush = rimGlowBrush,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = IndicatorRimGlowWidth.toPx()),
                )
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = IndicatorRimWidth.toPx()),
                )
            }
    )
}

/** 指示器相对胶囊上下各留的边距（旧版：64dp 壳 / 56dp 指示器）。 */
internal val DockIndicatorVerticalInset = 4.dp

/** 视觉上移补偿，供调用方放置指示器。 */
internal val DockIndicatorLift = IndicatorVisualLift