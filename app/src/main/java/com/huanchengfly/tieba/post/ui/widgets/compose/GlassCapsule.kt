package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect

/** 毛玻璃模糊半径（平衡档）。 */
private val GlassBlurRadius = 10.dp

/**
 * 外圈微光晕分层（宽度 dp, alpha 系数）：多层同心描边、系数向外指数递减，
 * 合成自边界向外柔和淡出的渐变光晕（单层实线描边会形成硬边「套圈」观感）。
 * 每层绘制于胶囊外扩轮廓上，恰好只覆盖边界外侧 [0, width]，不含内侧残留。
 */
private val GlassHaloLayers = listOf(
    0.75f to 0.30f, 1.5f to 0.22f, 2.25f to 0.16f, 3f to 0.117f,
    3.75f to 0.085f, 4.5f to 0.062f, 5.25f to 0.045f, 6f to 0.033f,
)

/** 壳轮廓高光线宽度（顶部高光弧 / 底部反光弧）。 */
private val GlassRimWidth = 1.5.dp

/** 壳轮廓内侧光带宽度（玻璃内散光，低 alpha 同渐变）。 */
private val GlassRimGlowWidth = 3.dp

/** 壳底色透明度：有模糊时更低（让模糊透出），无模糊时更高（替代纯色）。 */
const val GlassSurfaceAlphaBlur = 0.68f
const val GlassSurfaceAlphaFallback = 0.92f

/**
 * 悬浮玻璃胶囊：背景模糊 + 高光 rim + 外圈光晕 + 投影。
 *
 * 主界面底部导航胶囊、搜索圆钮与个人信息条共用同一材质，
 * API < 31 无模糊能力时退化为增强半透明纯色壳。
 *
 * @param tint 壳底色（含透明度），由主题的 bottomBarSurface 派生
 */
@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    hazeState: HazeState?,
    tint: Color,
    isNightMode: Boolean,
    blurSupported: Boolean = supportsBackdropBlur(),
    glowProgress: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    // 双峰轮廓光：顶部高光弧（白）；底部次级弧——浅色下用暗边勾勒边界（白线在近白背景上不可见），
    // 夜间/AMOLED 用次级白色反光
    val rimTopAlpha = if (isNightMode) 0.22f else 0.90f
    val rimBottomColor = if (isNightMode) Color.White else Color.Black
    val rimBottomAlpha = if (isNightMode) 0.10f else 0.14f
    // 外圈微光晕：浅色贴边暗晕勾勒边界，夜间贴边发光
    val haloColor = if (isNightMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)
    // 4 色标双峰渐变（中段两端全透明，避免非预乘插值出脏灰）
    val rimBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = rimTopAlpha),
        0.35f to Color.White.copy(alpha = 0f),
        0.65f to rimBottomColor.copy(alpha = 0f),
        1f to rimBottomColor.copy(alpha = rimBottomAlpha),
    )
    val rimGlowBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = rimTopAlpha * 0.25f),
        0.35f to Color.White.copy(alpha = 0f),
        0.65f to rimBottomColor.copy(alpha = 0f),
        1f to rimBottomColor.copy(alpha = rimBottomAlpha * 0.25f),
    )
    val shadowAlpha = if (isNightMode) 0.22f else 0.13f
    // 按压光晕：外圈辉光整体增强（配合调用方的整体放大），松手回落
    val glow = 1f + 3f * glowProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = shadowAlpha),
                spotColor = Color.Black.copy(alpha = shadowAlpha),
            )
            .drawBehind {
                // 外圈微光晕：多层同心描边向外递减合成柔和渐变光晕；
                // 每层绘制于外扩轮廓上，恰好只覆盖边界外侧（无内侧残留暗线）
                GlassHaloLayers.forEach { (widthDp, factor) ->
                    val wPx = widthDp.dp.toPx()
                    val half = wPx / 2f
                    drawRoundRect(
                        color = haloColor.copy(
                            alpha = (haloColor.alpha * factor * glow).coerceAtMost(1f)
                        ),
                        topLeft = Offset(-half, -half),
                        size = Size(size.width + wPx, size.height + wPx),
                        cornerRadius = CornerRadius(size.height / 2f + half),
                        style = Stroke(width = wPx),
                    )
                }
            }
            .clip(shape)
            .then(
                if (blurSupported && hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurEffect {
                            style = HazeBlurStyle(
                                backgroundColor = Color.Transparent,
                                colorEffects = emptyList(),
                                blurRadius = GlassBlurRadius,
                                noiseFactor = 0f,
                                fallbackColorEffect = HazeColorEffect.tint(tint),
                            )
                            blurEnabled = true
                            blurredEdgeTreatment = BlurredEdgeTreatment(shape)
                        }
                    }
                } else Modifier
            )
            .background(tint)
            // 按压高光：玻璃表面轻微提亮（配合整体放大与光晕增强）
            .background(Color.White.copy(alpha = 0.06f * glowProgress.coerceIn(0f, 1f)))
            .border(width = GlassRimGlowWidth, brush = rimGlowBrush, shape = shape)
            .border(width = GlassRimWidth, brush = rimBrush, shape = shape)
    ) {
        content()
    }
}