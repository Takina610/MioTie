package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

/** 普通毛玻璃模糊半径（Glasmio navigationGlassBackground 默认 2dp）。 */
private val GlassBlurRadius = 2.dp

/** 无背景模糊能力时的壳底色不透明度。 */
const val GlassSurfaceAlphaFallback = 0.92f

/** MeiloX 玻璃壳容器色（GlassColors.container）。 */
private val GlassContainerDark = Color(0xFF1C1C1E)
private val GlassContainerLight = Color(0xFFF8F8FA)

/**
 * MeiloX 关闭「液态玻璃」后的底栏容器色：
 * 深色 1C1C1E@54%、浅色 F8F8FA@64%；无模糊能力时退化为高不透明度壳。
 */
fun glassDockTint(isNightMode: Boolean, blurSupported: Boolean = supportsBackdropBlur()): Color {
    val alpha = if (!blurSupported) {
        GlassSurfaceAlphaFallback
    } else if (isNightMode) 0.54f else 0.64f
    return (if (isNightMode) GlassContainerDark else GlassContainerLight).copy(alpha = alpha)
}

/** 描边画法：Compose 无 drawOutline，用 Outline 路径 + Stroke。 */
internal fun DrawScope.drawOutlineStroke(
    outline: Outline,
    brush: Brush,
    width: Float,
    blendMode: BlendMode = BlendMode.SrcOver,
) {
    drawPath(
        path = Path().apply { addOutline(outline) },
        brush = brush,
        style = Stroke(width = width),
        blendMode = blendMode,
    )
}

/** 壳边白色高光峰值（Glasmio Highlight.Default alpha）。 */
private const val GlassEdgeHighlightAlpha = 0.54f

private val GlassEdgeInnerShadowWidth = 3.dp

/**
 * 悬浮毛玻璃胶囊（MeiloX 关闭液态玻璃后的底栏材质）：
 * 背景模糊 + 中性明度层 + 低透明度容器色，无折射、无外圈光晕；
 * 顶边常驻白色高光 + 底边内阴影（MeiloX Highlight.Default / InnerShadow），让壳有厚度。
 *
 * 通用材质系数照抄 MeiloX 底栏：容器 alpha × 1.25，深色再 × 0.6、浅色 × 0.5；
 * 深色叠 #999999@17% 明度层，浅色叠 #F8F8F8@25% 明度 + #444444@15% 加色层。
 *
 * API < 31 无模糊能力时退化为增强半透明纯色壳。
 */
@Composable
fun GlassCapsule(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    backdrop: Backdrop?,
    tint: Color,
    isNightMode: Boolean,
    blurSupported: Boolean = supportsBackdropBlur(),
    content: @Composable BoxScope.() -> Unit,
) {
    val useBackdrop = blurSupported && backdrop != null
    val surfaceAlpha = if (useBackdrop) {
        tint.alpha * 1.25f * if (isNightMode) 0.6f else 0.5f
    } else {
        tint.alpha
    }
    val shadowAlpha = if (isNightMode) 0.12f else 0.07f
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = shadowAlpha),
                spotColor = Color.Black.copy(alpha = shadowAlpha),
            )
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop ?: emptyBackdrop(),
                shape = { shape },
                effects = {
                    if (useBackdrop) {
                        vibrancy()
                        blur(GlassBlurRadius.toPx())
                    }
                },
                highlight = {
                    Highlight.Default.copy(
                        alpha = GlassEdgeHighlightAlpha,
                        style = HighlightStyle.Default(angle = 90f),
                    )
                },
                shadow = null,
                onDrawSurface = {
                    if (useBackdrop) {
                        if (isNightMode) {
                            drawRect(
                                color = Color(0xFF999999).copy(alpha = 0.17f),
                                blendMode = BlendMode.Luminosity,
                            )
                        } else {
                            drawRect(
                                color = Color(0xFFF8F8F8).copy(alpha = 0.25f),
                                blendMode = BlendMode.Luminosity,
                            )
                            drawRect(
                                color = Color(0xFF444444).copy(alpha = 0.15f),
                                blendMode = BlendMode.Plus,
                            )
                        }
                    }
                    drawRect(color = tint.copy(alpha = surfaceAlpha))
                    // 玻璃厚度：底边内阴影压住边缘（Glasmio navigation inner shadow）
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutlineStroke(
                        outline = outline,
                        brush = Brush.verticalGradient(
                            0.6f to Color.Black.copy(alpha = 0f),
                            1f to Color.Black.copy(alpha = if (isNightMode) 0.08f else 0.12f),
                        ),
                        width = GlassEdgeInnerShadowWidth.toPx(),
                    )
                },
            )
    ) {
        content()
    }
}
