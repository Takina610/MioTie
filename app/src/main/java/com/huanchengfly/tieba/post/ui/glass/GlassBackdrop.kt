package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.util.WeakHashMap
import kotlin.math.ceil

val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop> {
    error("Glass controls must be hosted by GlassBackdropHost or GlassBackdropProvider")
}

/** Backdrop source reserved for modal surfaces that sample the rendered page. */
val LocalBlurBackdrop = staticCompositionLocalOf<Backdrop> {
    error("Blur glass must be hosted by the app backdrop provider")
}

@Composable
fun GlassBackdropProvider(
    backdrop: Backdrop,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalGlassBackdrop provides backdrop,
        LocalBlurBackdrop provides backdrop,
        content = content,
    )
}

/**
 * Keeps sampled content and glass overlays in separate layers. Glass controls must never be
 * placed inside [sampledContent], otherwise the backdrop can recursively sample itself.
 */
@Composable
fun GlassBackdropHost(
    modifier: Modifier = Modifier,
    sampledContent: @Composable BoxScope.(LayerBackdrop) -> Unit,
    overlayContent: @Composable BoxScope.(LayerBackdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(
        LocalGlassBackdrop provides backdrop,
        LocalBlurBackdrop provides backdrop,
    ) {
        Box(modifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                sampledContent(backdrop)
            }
            overlayContent(backdrop)
        }
    }
}

fun Modifier.glassBackdropSource(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)

/**
 * Full-page backdrop source that replays its content once per draw.
 *
 * Upstream `Modifier.layerBackdrop` draws the content to the screen and then replays the same
 * drawing a second time into the backdrop layer. For full-screen sources (pages and the tab
 * host) that replay dominates frame time while scrolling, so record into the layer and draw the
 * layer instead. Sampling mirrors `LayerBackdrop`: map the glass back into source-local
 * coordinates and invert press/drag scaling so sampled pixels stay screen-anchored.
 */
@Stable
class RecordedBackdrop internal constructor(
    internal val graphicsLayer: GraphicsLayer,
) : Backdrop {
    internal var layerCoordinates: LayoutCoordinates? = null

    override val isCoordinatesDependent: Boolean get() = true

    private val inverseLayerScope = GraphicsLayerBlockScope()

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        val glassCoordinates = coordinates ?: return
        if (!glassCoordinates.isAttached) return
        val source = layerCoordinates?.takeIf { it.isAttached } ?: return
        val offset = runCatching {
            source.localPositionOf(glassCoordinates, Offset.Zero)
        }.getOrElse {
            glassCoordinates.positionInWindow() - source.positionInWindow()
        }
        withTransform({
            if (layerBlock != null) {
                inverseLayerScope.reset(
                    drawScope = density,
                    size = Size(
                        glassCoordinates.size.width.toFloat(),
                        glassCoordinates.size.height.toFloat(),
                    ),
                )
                layerBlock.invoke(inverseLayerScope)
                scale(
                    1f / inverseLayerScope.scaleX.coerceAtLeast(0.001f),
                    1f / inverseLayerScope.scaleY.coerceAtLeast(0.001f),
                    Offset.Zero,
                )
            }
            translate(-offset.x, -offset.y)
        }) {
            drawLayer(graphicsLayer)
        }
    }
}

@Composable
fun rememberRecordedBackdrop(): RecordedBackdrop {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(graphicsLayer) { RecordedBackdrop(graphicsLayer) }
}

/** Single-pass counterpart of `Modifier.layerBackdrop(backdrop)`. */
fun Modifier.recordBackdropSource(backdrop: RecordedBackdrop): Modifier =
    this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) backdrop.layerCoordinates = coordinates
        }
        .drawWithContent {
            val widthPx = size.width.toInt()
            val heightPx = size.height.toInt()
            if (widthPx <= 0 || heightPx <= 0) return@drawWithContent drawContent()
            val content = this
            val layer = backdrop.graphicsLayer
            layer.record(IntSize(widthPx, heightPx)) { content.drawContent() }
            drawLayer(layer)
        }

/** Window-space positions of backdrop source layers, for cross-window sampling. */
private val backdropSourcePositions = WeakHashMap<LayerBackdrop, LayoutCoordinates>()

/** Attach next to a [layerBackdrop] recording so dialog-window glass can locate the source. */
fun Modifier.trackBackdropPosition(backdrop: LayerBackdrop): Modifier =
    onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            backdropSourcePositions[backdrop] = coordinates
        }
    }

/**
 * Samples a [LayerBackdrop] that was recorded in another window (e.g. app content from a
 * dialog/sheet window). LayerBackdrop maps coordinates with localPositionOf, which cannot
 * cross compose owners and silently yields a wrong offset there, so glass in a dialog
 * samples nothing. Both windows share the screen origin, making window-space subtraction
 * the correct mapping.
 */
class CrossWindowBackdrop(
    private val source: LayerBackdrop,
    private val rasterized: GraphicsLayer? = null,
    private val renderScale: Float = 1f,
    private val captureHeight: Dp? = null,
) : Backdrop {

    private val inverseLayerScope = GraphicsLayerBlockScope()
    private var recordedSourceSize = IntSize.Zero

    override val isCoordinatesDependent: Boolean get() = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        val glassCoordinates = coordinates ?: return
        if (!glassCoordinates.isAttached) return
        val sourceCoordinates = backdropSourcePositions[source]?.takeIf { it.isAttached } ?: return
        val sourceSize = source.graphicsLayer.size
        val captureTop = captureHeight?.let {
            (sourceSize.height - with(density) { it.roundToPx() }).coerceAtLeast(0)
        } ?: 0
        val layer = rasterized?.also { cached ->
            val bufferSize = IntSize(
                ceil(sourceSize.width * renderScale).toInt(),
                ceil((sourceSize.height - captureTop) * renderScale).toInt(),
            )
            if (bufferSize.width <= 0 || bufferSize.height <= 0) return
            if (cached.size != bufferSize || recordedSourceSize != sourceSize) {
                cached.clip = true
                cached.compositingStrategy = CompositingStrategy.Offscreen
                cached.record(bufferSize) {
                    withTransform({
                        scale(renderScale, renderScale, Offset.Zero)
                        translate(0f, -captureTop.toFloat())
                    }) {
                        drawLayer(source.graphicsLayer)
                    }
                }
                recordedSourceSize = sourceSize
            }
        } ?: source.graphicsLayer
        // Popup/Dialog owners have their own Compose window origin. `positionInWindow()` is
        // therefore local to that owner on Android and makes a popup sample from its host
        // overshoot area (typically near the top edge) instead of the pixels behind the menu.
        // Screen coordinates are shared by both owners and include the real popup placement.
        val offset = glassCoordinates.positionOnScreen() - sourceCoordinates.positionOnScreen()
        withTransform({
            if (layerBlock != null) {
                // drawBackdrop applies layerBlock to the rendered glass surface. Mirror the
                // LayerBackdrop behavior here so the sampled source stays screen-anchored while
                // that surface is pressed and drag-scaled.
                inverseLayerScope.reset(
                    drawScope = density,
                    size = Size(
                        glassCoordinates.size.width.toFloat(),
                        glassCoordinates.size.height.toFloat(),
                    ),
                )
                layerBlock.invoke(inverseLayerScope)
                scale(
                    1f / inverseLayerScope.scaleX.coerceAtLeast(0.001f),
                    1f / inverseLayerScope.scaleY.coerceAtLeast(0.001f),
                    Offset.Zero,
                )
            }
            translate(-offset.x, -offset.y + captureTop)
            if (rasterized != null) scale(1f / renderScale, 1f / renderScale, Offset.Zero)
        }) {
            drawLayer(layer)
        }
    }
}

/**
 * Wraps a [LayerBackdrop] for glass rendered in a separate window (dialog/sheet).
 * Non-layer backdrops pass through unchanged.
 */
@Composable
fun rememberCrossWindowBackdrop(backdrop: Backdrop): Backdrop = remember(backdrop) {
    if (backdrop is LayerBackdrop) CrossWindowBackdrop(backdrop) else backdrop
}

/** Samples from a stable host while only the glass outline follows its animated layer. */
@Composable
fun rememberStationaryBackdrop(
    backdrop: Backdrop,
    samplingCoordinates: () -> LayoutCoordinates?,
): Backdrop {
    val source = rememberCrossWindowBackdrop(backdrop)
    return remember(source, samplingCoordinates) { StationaryBackdrop(source, samplingCoordinates) }
}

private class StationaryBackdrop(
    private val source: Backdrop,
    private val samplingCoordinates: () -> LayoutCoordinates?,
) : Backdrop {
    private val layerScope = GraphicsLayerBlockScope()
    private val inverseLayer = Matrix()

    override val isCoordinatesDependent: Boolean get() = true

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        val host = samplingCoordinates()?.takeIf { it.isAttached } ?: return
        inverseLayer.reset()
        if (layerBlock != null) {
            // 采样时按实时动画参数求值，避免读到上一帧的缩放或移动中的轴心。
            layerScope.reset(density, Size(host.size.width.toFloat(), host.size.height.toFloat()))
            layerBlock(layerScope)
            inverseLayer.resetToPivotedTransform(
                pivotX = layerScope.size.width * layerScope.transformOrigin.pivotFractionX,
                pivotY = layerScope.size.height * layerScope.transformOrigin.pivotFractionY,
                translationX = layerScope.translationX,
                translationY = layerScope.translationY,
                rotationX = layerScope.rotationX,
                rotationY = layerScope.rotationY,
                rotationZ = layerScope.rotationZ,
                scaleX = layerScope.scaleX,
                scaleY = layerScope.scaleY,
            )
            inverseLayer.invert()
        }
        withTransform({ transform(inverseLayer) }) {
            // 宿主在动画层之外，传 null 避免源背景重复应用一次逆变换。
            with(source) { drawBackdrop(density, host, null) }
        }
    }
}

/** Share a live, flattened player source between modal glass and its small glass controls. */
@Composable
fun rememberRasterizedPlayerBackdrop(source: LayerBackdrop): Backdrop {
    val rasterized = rememberGraphicsLayer()
    return remember(source, rasterized) {
        CrossWindowBackdrop(source, rasterized, renderScale = 0.25f)
    }
}

/**
 * Share rasterized page regions between glass consumers instead of replaying the display
 * list (including nested blur and navigation layers) for every lens. This is a live layer
 * reference: source changes invalidate the GPU cache even when the recording is unchanged.
 * Only sampled pixels use half resolution; text, glass outlines and highlights stay native.
 */
@Composable
fun rememberSharedGlassBackdrop(base: Backdrop, source: LayerBackdrop): Backdrop {
    val full = rememberGraphicsLayer()
    val bottom = rememberGraphicsLayer()
    return remember(base, source, full, bottom) {
        SharedGlassBackdrop(base, source, full, bottom)
    }
}

/** A morph records in root coordinates, but only needs pixels inside its capture viewport. */
internal interface ViewportBackdrop : Backdrop {
    fun DrawScope.drawViewport(density: Density, coordinates: LayoutCoordinates, viewport: Rect)
}

private val BottomCaptureHeight = 240.dp

private class SharedGlassBackdrop(
    private val base: Backdrop,
    private val source: LayerBackdrop,
    fullLayer: GraphicsLayer,
    bottomLayer: GraphicsLayer,
) : ViewportBackdrop {
    private val full = CrossWindowBackdrop(source, fullLayer, renderScale = 0.5f)
    private val bottom = CrossWindowBackdrop(source, bottomLayer, renderScale = 0.5f,
        captureHeight = BottomCaptureHeight)

    override val isCoordinatesDependent: Boolean get() = true

    private fun sample(density: Density, coordinates: LayoutCoordinates?, top: Float): Backdrop {
        val recorded = backdropSourcePositions[source]
        if (coordinates?.isAttached != true || recorded?.isAttached != true) return full
        val sourceTop = coordinates.positionOnScreen().y - recorded.positionOnScreen().y + top
        val bandTop = source.graphicsLayer.size.height - with(density) { BottomCaptureHeight.toPx() }
        // Include the largest navigation lens/blur padding. Upper controls and full-sheet
        // captures use the full page; bottom morphs never replay the top toolbar's blur.
        return if (sourceTop - with(density) { 32.dp.toPx() } >= bandTop) bottom else full
    }

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        with(base) { drawBackdrop(density, coordinates, layerBlock) }
        with(sample(density, coordinates, 0f)) { drawBackdrop(density, coordinates, layerBlock) }
    }

    override fun DrawScope.drawViewport(
        density: Density,
        coordinates: LayoutCoordinates,
        viewport: Rect,
    ) {
        with(base) { drawBackdrop(density, coordinates, null) }
        with(sample(density, coordinates, viewport.top)) { drawBackdrop(density, coordinates, null) }
    }
}
