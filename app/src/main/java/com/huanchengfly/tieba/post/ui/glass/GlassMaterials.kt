package com.huanchengfly.tieba.post.ui.glass

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import kotlin.math.ceil

@Immutable
internal data class NavigationGlassFill(val color: Color, val blendMode: BlendMode)

/** Ordered paint parameters from Apple's iOS 27 native glass materials. */
enum class NavigationGlassMaterial(
    val frostRadius: Dp,
    val shadowRadius: Dp,
    val lightShadowAlpha: Float,
    val darkShadowAlpha: Float,
    val nativeLightIntensity: Float,
) {
    RegularSmall(6.dp, 15.dp, 0.02f, 0.04f, 0.25f),
    RegularMedium(16.dp, 48.dp, 0.25f, 0.45f, 0.25f),
    RegularLarge(16.dp, 48.dp, 0.25f, 0.45f, 0.25f),
    Clear(6.dp, 15.dp, 0.02f, 0.02f, 0.4f);

    val shadowOffsetY: Dp get() = 8.dp

    internal fun fills(isDark: Boolean): List<NavigationGlassFill> = when (this) {
        RegularSmall -> if (isDark) SmallDark else SmallLight
        RegularMedium, RegularLarge -> if (isDark) MediumLargeDark else MediumLargeLight
        Clear -> ClearFills
    }
}

private val SmallLight = listOf(
    NavigationGlassFill(Color.Black.copy(alpha = 0.25f), BlendMode.SrcOver),
    NavigationGlassFill(Color.White.copy(alpha = 0.25f), BlendMode.SrcOver),
    NavigationGlassFill(Color(0xFF444444).copy(alpha = 0.6f), BlendMode.Plus),
    NavigationGlassFill(Color(0xFFF8F8F8).copy(alpha = 0.2f), BlendMode.Luminosity),
)
private val SmallDark = listOf(
    NavigationGlassFill(Color(0xFF999999).copy(alpha = 0.17f), BlendMode.Luminosity),
)
private val MediumLargeLight = listOf(
    NavigationGlassFill(Color.White.copy(alpha = 0.7f), BlendMode.Lighten),
    NavigationGlassFill(Color(0xFFBFBFBF).copy(alpha = 0.1f), BlendMode.Darken),
)
private val MediumLargeDark = listOf(
    NavigationGlassFill(Color(0xFF1A1A1A).copy(alpha = 0.7f), BlendMode.Luminosity),
    NavigationGlassFill(Color(0xFF1A1A1A).copy(alpha = 0.9f), BlendMode.Luminosity),
    NavigationGlassFill(Color(0xFF1A1A1A), BlendMode.Lighten),
)
private val ClearFills = listOf(
    NavigationGlassFill(Color(0xFF101010), BlendMode.Plus),
    NavigationGlassFill(Color.White.copy(alpha = 0.04f), BlendMode.Luminosity),
)

internal fun DrawScope.drawNavigationGlassMaterial(
    containerColor: Color,
    isDark: Boolean,
    containerAlphaMultiplier: Float = 1.25f,
    material: NavigationGlassMaterial? = null,
    materialTint: Color? = null,
    alpha: Float = 1f,
) {
    val opacity = alpha.coerceIn(0f, 1f)
    if (opacity <= 0f) return
    if (material != null) {
        material.fills(isDark).forEach { fill ->
            drawRect(fill.color, alpha = opacity, blendMode = fill.blendMode)
        }
        materialTint?.let { tint ->
            drawRect(tint.copy(alpha = (tint.alpha * containerAlphaMultiplier).coerceIn(0f, 1f)), alpha = opacity)
        }
        return
    }
    if (isDark) {
        drawRect(Color(0xFF999999).copy(alpha = 0.17f), alpha = opacity, blendMode = BlendMode.Luminosity)
    }
    drawRect(
        containerColor.copy(
            alpha = containerColor.alpha * containerAlphaMultiplier.coerceAtLeast(0f) *
                if (isDark) 0.6f else 1f,
        ),
        alpha = opacity,
    )
}

/**
 * Figma's exterior-only black navigation shadow, blurred on the GPU outside the glass clip.
 * Mirrors Glasmio `NavigationDropShadowRenderer`; unavailable below API 29 (no RenderNode).
 */
@androidx.annotation.RequiresApi(29)
internal class NavigationDropShadowRenderer(
    private val shadowRadius: Dp = 15.dp,
    private val shadowOffsetY: Dp = 8.dp,
    private val shadowAlpha: Float = 0.02f,
) {
    private val mask = RenderNode("Navigation exterior drop shadow")
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
    private var outlinePath = android.graphics.Path()
    private var key: GeometryKey? = null
    private var blurDensity = Float.NaN
    private var padding = 0f
    private var verticalOffset = 0f
    private var maskWidth = 0
    private var maskHeight = 0
    private val maskClipBounds = android.graphics.Rect()

    fun draw(scope: DrawScope, shape: Shape, alpha: Float = 1f) {
        val canvas = scope.drawContext.canvas.nativeCanvas
        if (!canvas.isHardwareAccelerated || scope.size.minDimension <= 0f || alpha <= 0f) return
        prepare(scope, shape)
        mask.alpha = shadowAlpha * alpha.coerceIn(0f, 1f)
        val save = canvas.save()
        try {
            // Remove the source interior after the blur so the shadow never leaks inside
            // translucent glass.
            canvas.clipOutPath(outlinePath)
            canvas.translate(-padding, verticalOffset - padding)
            canvas.clipRect(maskClipBounds)
            canvas.drawRenderNode(mask)
        } finally {
            canvas.restoreToCount(save)
        }
    }

    private fun prepare(scope: DrawScope, shape: Shape) {
        val nextKey = GeometryKey(shape, scope.size, scope.layoutDirection, scope.density, scope.fontScale)
        if (nextKey == key && mask.hasDisplayList()) return
        val outline = shape.createOutline(scope.size, scope.layoutDirection, scope)
        outlinePath = Path().apply {
            when (outline) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
        }.asAndroidPath()
        if (blurDensity != scope.density) {
            // Figma blur 15 is Gaussian sigma 7.5; Android maps radius to sigma = r * 0.57735 + 0.5.
            val sigma = (shadowRadius.value * 0.5f) * scope.density
            val radius = ((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.001f)
            mask.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
            padding = ceil(3f * sigma)
            verticalOffset = shadowOffsetY.value * scope.density
            blurDensity = scope.density
        }
        val requiredWidth = ceil(scope.size.width + padding * 2f).toInt()
        val requiredHeight = ceil(scope.size.height + padding * 2f + verticalOffset).toInt()
        maskWidth = maxOf(maskWidth, (requiredWidth + 63) / 64 * 64)
        maskHeight = maxOf(maskHeight, (requiredHeight + 63) / 64 * 64)
        maskClipBounds.set(0, 0, requiredWidth, requiredHeight)
        val width = maskWidth
        val height = maskHeight
        mask.setPosition(0, 0, width, height)
        val recording = mask.beginRecording(width, height)
        try {
            recording.translate(padding, padding)
            recording.drawPath(outlinePath, fill)
        } finally {
            mask.endRecording()
        }
        key = nextKey
    }

    fun release() {
        mask.discardDisplayList()
        key = null
        maskWidth = 0
        maskHeight = 0
        maskClipBounds.setEmpty()
    }

    private data class GeometryKey(
        val shape: Shape,
        val size: Size,
        val layoutDirection: LayoutDirection,
        val density: Float,
        val fontScale: Float,
    )
}

/** Soft exterior shadow drawn outside the backdrop clip while sharing its press transform. */
internal fun Modifier.navigationGlassDropShadow(
    shape: () -> Shape,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    shadowRadius: Dp = 15.dp,
    shadowOffsetY: Dp = 8.dp,
    shadowAlpha: Float = 0.02f,
): Modifier = then(
    NavigationGlassDropShadowElement(shape, layerBlock, shadowRadius, shadowOffsetY, shadowAlpha)
)

private class NavigationGlassDropShadowElement(
    private val shape: () -> Shape,
    private val layerBlock: (GraphicsLayerScope.() -> Unit)?,
    private val shadowRadius: Dp,
    private val shadowOffsetY: Dp,
    private val shadowAlpha: Float,
) : ModifierNodeElement<NavigationGlassDropShadowNode>() {
    override fun create() = NavigationGlassDropShadowNode(shape, layerBlock, shadowRadius, shadowOffsetY, shadowAlpha)

    override fun update(node: NavigationGlassDropShadowNode) {
        node.shape = shape
        node.layerBlock = layerBlock
        node.shadowRadius = shadowRadius
        node.shadowOffsetY = shadowOffsetY
        node.shadowAlpha = shadowAlpha
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "navigationGlassDropShadow"
        properties["shape"] = shape
        properties["layerBlock"] = layerBlock
    }

    override fun equals(other: Any?): Boolean =
        other is NavigationGlassDropShadowElement &&
            shape == other.shape &&
            layerBlock == other.layerBlock &&
            shadowRadius == other.shadowRadius &&
            shadowOffsetY == other.shadowOffsetY &&
            shadowAlpha == other.shadowAlpha

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + (layerBlock?.hashCode() ?: 0)
        result = 31 * result + shadowRadius.hashCode()
        result = 31 * result + shadowOffsetY.hashCode()
        result = 31 * result + shadowAlpha.hashCode()
        return result
    }
}

private class NavigationGlassDropShadowNode(
    var shape: () -> Shape,
    var layerBlock: (GraphicsLayerScope.() -> Unit)?,
    var shadowRadius: Dp,
    var shadowOffsetY: Dp,
    var shadowAlpha: Float,
) : DrawModifierNode, Modifier.Node() {
    private var renderer = newRenderer()
    private val layerScope = GraphicsLayerBlockScope()

    private fun newRenderer() = if (android.os.Build.VERSION.SDK_INT >= 29) {
        NavigationDropShadowRenderer(shadowRadius, shadowOffsetY, shadowAlpha)
    } else null

    override fun ContentDrawScope.draw() {
        val shadow = renderer
        if (shadow == null) {
            drawContent()
            return
        }
        val block = layerBlock
        if (block == null) {
            shadow.draw(this, shape(), 1f)
            drawContent()
            return
        }
        layerScope.reset(this, size)
        block(layerScope)
        val alpha = layerScope.alpha
        if (alpha <= 0f) {
            drawContent()
            return
        }
        val scaleX = layerScope.scaleX
        val scaleY = layerScope.scaleY
        val translationX = layerScope.translationX
        val translationY = layerScope.translationY
        if (scaleX == 1f && scaleY == 1f && translationX == 0f && translationY == 0f) {
            shadow.draw(this, shape(), alpha)
        } else {
            // 外部投影必须和应用到玻璃层的缩放/位移同步，否则菜单展开时投影会以全尺寸留在原地。
            val canvas = drawContext.canvas
            val pivotX = layerScope.transformOrigin.pivotFractionX * size.width
            val pivotY = layerScope.transformOrigin.pivotFractionY * size.height
            canvas.save()
            canvas.translate(translationX, translationY)
            canvas.translate(pivotX, pivotY)
            canvas.scale(scaleX, scaleY)
            canvas.translate(-pivotX, -pivotY)
            shadow.draw(this, shape(), alpha)
            canvas.restore()
        }
        drawContent()
    }

    override fun onDetach() {
        renderer?.release()
        renderer = null
    }
}

/**
 * Blurs the scene behind a modal sheet at quarter resolution and hands the result to
 * [com.kyant.backdrop.drawBackdrop]. Sheet content stays at native resolution.
 */
@Composable
internal fun rememberSheetBackdropRenderer(backdrop: Backdrop, blurRadius: Dp): Backdrop {
    val density = LocalDensity.current
    val layer = rememberGraphicsLayer()
    return remember(backdrop, blurRadius, density, layer) {
        SheetBackdropRenderer(backdrop, density, with(density) { blurRadius.toPx() }, layer)
    }
}

internal class SheetBackdropRenderer(
    private val backdrop: Backdrop,
    private val density: Density,
    private val blurRadius: Float,
    private val layer: GraphicsLayer,
) : Backdrop {
    override val isCoordinatesDependent: Boolean get() = true

    private val renderScale = 0.25f

    init {
        layer.clip = true
        // Compose 的 BlurEffect 在 API 31 以下会安全降级，避免直接引用 android RenderEffect。
        layer.renderEffect = BlurEffect(
            blurRadius * renderScale,
            blurRadius * renderScale,
            TileMode.Clamp,
        )
    }

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return
        val logicalSize = size
        // Include the blur kernel outside the sheet to avoid clamped edges while it slides.
        val padding = ceil(blurRadius * 3f)
        val bufferSize = IntSize(
            ceil((logicalSize.width + padding * 2f) * renderScale).toInt().coerceAtLeast(1),
            ceil((logicalSize.height + padding * 2f) * renderScale).toInt().coerceAtLeast(1),
        )
        layer.record(bufferSize) {
            withTransform({
                scale(renderScale, renderScale, Offset.Zero)
                translate(padding, padding)
            }) {
                val previousSize = drawContext.size
                drawContext.size = logicalSize
                try {
                    with(backdrop) {
                        drawBackdrop(this@SheetBackdropRenderer.density, coordinates, layerBlock)
                    }
                } finally {
                    drawContext.size = previousSize
                }
            }
        }
        withTransform({
            translate(-padding, -padding)
            scale(1f / renderScale, 1f / renderScale, Offset.Zero)
        }) { drawLayer(layer) }
    }
}
