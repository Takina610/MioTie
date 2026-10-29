package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.huanchengfly.tieba.post.ui.liquidglass.DampedDragAnimation

/**
 * iOS 液体玻璃滑块（照抄 MeiloX 1.55.1 的 `GlasmioSlider`）：
 * 6dp 轨道 + 40×24dp 可拖动玻璃滑块，拖动时直接跟手、松手才回弹。
 *
 * [value] 由调用方持有；进度回写是异步的（写盘/重启类设置）时，
 * 应由调用方保留一份本地草稿值，否则拖动中会被旧值拉回去。
 *
 * [onValueChangeFinished] 带上手势结束时的值：回调若去读调用方组合里的 `value`，
 * 拿到的是上一次重组的值，点按轨道这种「按下即改值、随即抬起」的手势会被写回旧值。
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    onValueChangeFinished: ((Float) -> Unit)? = null,
) {
    require(value.isFinite()) { "Slider value must be finite." }
    require(
        valueRange.start.isFinite() && valueRange.endInclusive.isFinite() &&
                valueRange.start < valueRange.endInclusive &&
                (valueRange.endInclusive - valueRange.start).isFinite()
    ) {
        "Slider range must have finite, increasing bounds and a finite span."
    }
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val light = !colors.isDark
    val accent = colors.accent
    val track = (if (light) {
        Color(0xFF787878).copy(alpha = 0.20f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }).let {
        if (appearance.reduceTransparency) it.compositeOver(colors.elevatedBackground) else it
    }
    val trackBackdrop = rememberLayerBackdrop()
    val currentEnabled by rememberUpdatedState(enabled)
    val currentValue by rememberUpdatedState(value.coerceIn(valueRange))
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    var dragValue by remember(valueRange, enabled) {
        mutableFloatStateOf(value.fastCoerceIn(valueRange.start, valueRange.endInclusive))
    }
    var dragging by remember(valueRange, enabled) { mutableStateOf(false) }
    val displayedValue = if (dragging) dragValue else currentValue
    val updateValue by rememberUpdatedState<(Float) -> Unit> { next ->
        val target = next.coerceIn(valueRange)
        val previous = if (dragging) dragValue else currentValue
        if (dragging) dragValue = target
        if (target != previous) currentOnValueChange(target)
    }
    val progress = {
        (displayedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .offset(y = 6.dp)
            .progressSemantics(displayedValue, valueRange)
            .semantics {
                if (!enabled) disabled()
                setProgress { requested ->
                    if (!enabled || !requested.isFinite()) {
                        false
                    } else {
                        val target = requested.coerceIn(valueRange)
                        if (target == displayedValue) {
                            false
                        } else {
                            updateValue(target)
                            currentOnValueChangeFinished?.invoke(target)
                            true
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth
        val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val currentTrackWidth by rememberUpdatedState(trackWidth)
        val currentLtr by rememberUpdatedState(ltr)
        val scope = rememberCoroutineScope()
        val animation = remember(scope, valueRange, enabled) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = value.fastCoerceIn(valueRange.start, valueRange.endInclusive),
                valueRange = valueRange,
                visibilityThreshold = (valueRange.endInclusive - valueRange.start) / 1000f,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> },
            )
        }
        val valueFromPosition by rememberUpdatedState<(Float) -> Float> { x ->
            val fraction = (x / currentTrackWidth.coerceAtLeast(1)).coerceIn(0f, 1f)
            (if (currentLtr) {
                lerp(valueRange.start, valueRange.endInclusive, fraction)
            } else {
                lerp(valueRange.endInclusive, valueRange.start, fraction)
            }).coerceIn(valueRange)
        }
        val updateFromPosition by rememberUpdatedState<(Float) -> Unit> { x ->
            updateValue(valueFromPosition(x))
        }
        val transformedTrackBackdrop = rememberBackdrop(
            backdrop = trackBackdrop,
            onDraw = remember(animation) {
                { draw ->
                    val p = animation.pressProgress
                    scale(lerp(2f / 3f, 1f, p), lerp(0f, 1f, p)) { draw() }
                }
            },
        )
        val thumbBackdrop = rememberCombinedBackdrop(backdrop, transformedTrackBackdrop)
        val thumbGlassModifier = remember(thumbBackdrop, animation, appearance) {
            Modifier.drawBackdrop(
                backdrop = if (appearance.usesBackdrop) thumbBackdrop else emptyBackdrop(),
                shape = { Capsule() },
                effects = {
                    if (appearance.usesBackdrop) {
                        val p = animation.pressProgress
                        blur(8.dp.toPx() * if (appearance.usesRefraction) 1f - p else 1f)
                        if (appearance.usesRefraction) {
                            lens(10.dp.toPx() * p, 14.dp.toPx() * p, chromaticAberration = true)
                        }
                    }
                },
                highlight = {
                    Highlight.Ambient.copy(
                        width = Highlight.Ambient.width / 1.5f,
                        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                        alpha = animation.pressProgress,
                    )
                },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.15f)) },
                innerShadow = {
                    InnerShadow(
                        radius = 4.dp * animation.pressProgress,
                        alpha = animation.pressProgress,
                    )
                },
                layerBlock = {
                    scaleX = animation.scaleX
                    scaleY = animation.scaleY
                },
                onDrawSurface = {
                    val alpha = if (appearance.reduceTransparency) 1f else 1f - animation.pressProgress
                    drawRect(Color.White.copy(alpha = alpha))
                },
            )
        }
        Box(
            Modifier.then(if (appearance.usesBackdrop) Modifier.layerBackdrop(trackBackdrop) else Modifier)
                .fillMaxWidth(),
        ) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(track)
                    .height(6.dp)
                    .fillMaxWidth(),
            )
            Box(
                Modifier
                    .clip(Capsule())
                    .background(accent)
                    .height(6.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * progress()).fastRoundToInt()
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    },
            )
        }
        Box(
            Modifier
                .graphicsLayer {
                    // 位置直接跟随指针，弹簧只负责玻璃形变
                    translationX = (-size.width / 2f + trackWidth * progress())
                        .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (ltr) 1f else -1f
                    alpha = if (enabled) 1f else 0.45f
                }
                .then(thumbGlassModifier)
                .size(width = 40.dp, height = 24.dp),
        )
        // 手势层固定在轨道上：在会位移的滑块坐标系里量增量会被自身位移抵消，拖起来发滞。
        //
        // 点按与拖动必须由同一个 detector 处理：两个 pointerInput 叠在同一节点上会互相
        // 抢事件，先消费手势的那个赢，另一个永远收不到，表现为「能拖不能点」。
        // 这里统一走水平拖动 + 抬起判定：按下即定位，位移后进入拖动，抬起才提交。
        Box(
            Modifier.fillMaxSize()
                .pointerInput(enabled, valueRange, animation) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 抬起时用最后一次指针位置还原目标值，而不是读调用方的 value：
                        // 那个值要等下一次重组才更新，点按会被写回旧档位。
                        var lastX = down.position.x
                        if (currentEnabled) {
                            dragValue = currentValue
                            dragging = true
                            animation.press()
                            updateFromPosition(lastX)
                        }
                        var moved = false
                        var settled = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                if (currentEnabled) {
                                    currentOnValueChangeFinished?.invoke(valueFromPosition(lastX))
                                }
                                dragging = false
                                animation.release()
                                settled = true
                                break
                            }
                            if (!change.isConsumed) {
                                lastX = change.position.x
                                if (change.positionChange().x != 0f) {
                                    moved = true
                                    change.consume()
                                }
                                if (moved) updateFromPosition(lastX)
                            }
                        }
                        // 手势被列表滚动等抢走时收不到 up，这里兜底复位，否则滑块会一直保持按下态
                        if (!settled) {
                            dragging = false
                            animation.release()
                        }
                    }
                },
        )
    }
}
