package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.liquidglass.DampedDragAnimation
import com.huanchengfly.tieba.post.ui.liquidglass.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlin.math.roundToInt

private val SegmentedIndicatorRefractionHeight = 5.dp
private val SegmentedIndicatorRefractionAmount = 7.dp
private val SegmentedIndicatorHorizontalExpansion = 16.dp
private const val SegmentedIndicatorPressedScale = 39f / 28f

/** Measure the deformed surface before resolving Capsule and its optical outline. */
private fun segmentedIndicatorSize(
    restingSize: Size,
    scaleX: Float,
    scaleY: Float,
    velocity: Float,
    expansion: Float,
): Size {
    val horizontalProgress = (scaleX - 1f) / (SegmentedIndicatorPressedScale - 1f)
    return Size(
        ((restingSize.width + expansion * horizontalProgress) /
            (1f - (velocity * 0.75f).coerceIn(-0.18f, 0.18f))).coerceAtLeast(1f),
        (restingSize.height * scaleY *
            (1f - (velocity * 0.25f).coerceIn(-0.14f, 0.14f))).coerceAtLeast(1f),
    )
}

/**
 * iOS 分段控件（逐行照搬 Glasmio `GlasmioSegmentedControl`）：
 * 可见灰色轨道 → 隐藏的玻璃源层 → 采样两者的透镜指示器；
 * 指示器随拖动拉伸，按下时折射并重绘标签，标签始终停在固定轨道位置。
 */
@Composable
fun <T> IosSegmentedControl(
    items: List<Pair<T, String>>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    height: Dp = 32.dp,
    enabled: Boolean = true,
) {
    require(items.isNotEmpty())
    require(height.value.isFinite() && height > 4.dp) { "Segment height must be finite and greater than 4 dp." }
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val isLight = !colors.isDark
    val selectedIndex = if (selected == null) -1 else items.indexOfFirst { it.first == selected }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentItems by rememberUpdatedState(items)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val scope = rememberCoroutineScope()
    val tabsBackdrop = rememberLayerBackdrop()
    val indicatorBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val trackColor = colors.segmentedControlBackground.let {
        if (appearance.reduceTransparency) it.compositeOver(colors.elevatedBackground) else it
    }
    val indicatorHeight = height - 4.dp
    // 隐藏的玻璃源层与可见轨道同高，拖动换算需要以它为准。
    val dragCoordinateSpace = remember { mutableStateOf<LayoutCoordinates?>(null) }

    BoxWithConstraints(
        modifier
            .height(height)
            .selectableGroup()
            .alpha(if (enabled) 1f else 0.45f)
            .onGloballyPositioned { dragCoordinateSpace.value = it },
    ) {
        val density = LocalDensity.current
        val contentWidthPx = (constraints.maxWidth - with(density) { 4.dp.roundToPx() })
            .coerceAtLeast(items.size)
            .toFloat()
        val tabWidthPx = contentWidthPx / items.size
        val currentTabWidthPx by rememberUpdatedState(tabWidthPx)
        val trackPaddingPx = with(density) { 2.dp.toPx() }
        val textMeasurer = rememberTextMeasurer()
        val labelStyle = IosTypography.caption
        val labelLayouts = remember(
            textMeasurer,
            items,
            selectedIndex,
            labelStyle,
            colors.content,
            tabWidthPx,
        ) {
            items.mapIndexed { index, item ->
                textMeasurer.measure(
                    text = AnnotatedString(item.second),
                    style = labelStyle.copy(
                        color = colors.content,
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    constraints = Constraints(maxWidth = tabWidthPx.roundToInt().coerceAtLeast(1)),
                )
            }
        }
        val interactiveHighlight = remember(scope, isLtr) {
            InteractiveHighlight(animationScope = scope)
        }
        val animation = remember(scope, items.size, isLtr) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = selectedIndex.coerceAtLeast(0).toFloat(),
                valueRange = 0f..items.lastIndex.coerceAtLeast(1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = SegmentedIndicatorPressedScale,
                dragCoordinateSpace = { dragCoordinateSpace.value },
                horizontalDragOnly = true,
                onDragStarted = { if (currentEnabled) interactiveHighlight.startPress(it) },
                onDragStopped = {
                    interactiveHighlight.endPress()
                    val target = targetValue.roundToInt().coerceIn(0, currentItems.lastIndex)
                    if (currentEnabled) currentOnSelected(currentItems[target].first)
                    val restingIndex = if (currentEnabled) target else currentSelectedIndex
                    if (restingIndex >= 0) animateToValue(restingIndex.toFloat())
                },
                onDragCancelled = {
                    interactiveHighlight.endPress()
                    if (currentSelectedIndex >= 0) {
                        animateToValue(currentSelectedIndex.toFloat(), animatePress = false)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!currentEnabled) return@DampedDragAnimation
                    updateValue(
                        (targetValue + dragAmount.x / currentTabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, currentItems.lastIndex.toFloat()),
                    )
                },
            )
        }
        LaunchedEffect(selectedIndex, enabled, animation) {
            if (selectedIndex >= 0) animation.animateToValue(selectedIndex.toFloat(), animatePress = enabled)
            if (!enabled) interactiveHighlight.endPress()
        }
        val segmentedHighlightModifier = remember(interactiveHighlight, animation, isLtr, trackPaddingPx) {
            interactiveHighlight.highlightModifier(
                positionProvider = { size ->
                    Offset(
                        x = if (isLtr) {
                            trackPaddingPx + (animation.value + 0.5f) * currentTabWidthPx
                        } else {
                            size.width - trackPaddingPx - (animation.value + 0.5f) * currentTabWidthPx
                        },
                        y = size.height / 2f,
                    )
                },
                horizontalRadiusProvider = { currentTabWidthPx * 0.65f },
            )
        }
        val hiddenGlassModifier = remember(backdrop, animation, trackColor, appearance) {
            Modifier.drawBackdrop(
                backdrop = if (appearance.usesBackdrop) backdrop else emptyBackdrop(),
                shape = { Capsule() },
                effects = {
                    if (appearance.usesBackdrop) {
                        val press = animation.pressProgress
                        vibrancy()
                        blur(2.dp.toPx())
                        if (appearance.usesRefraction) {
                            lens(
                                8.dp.toPx() * press,
                                16.dp.toPx() * press,
                                depthEffect = press > 0.01f,
                                chromaticAberration = true,
                            )
                        }
                    }
                },
                highlight = { Highlight.Default.copy(alpha = animation.pressProgress) },
                onDrawSurface = { drawRect(trackColor) },
            )
        }
        val indicatorGlassModifier = remember(
            indicatorBackdrop,
            animation,
            appearance,
            isLight,
            labelLayouts,
            isLtr,
        ) {
            if (!appearance.usesBackdrop) Modifier else Modifier.drawBackdrop(
                backdrop = indicatorBackdrop,
                shape = { Capsule() },
                effects = {
                    if (appearance.usesRefraction) {
                        val press = animation.pressProgress
                        lens(
                            SegmentedIndicatorRefractionHeight.toPx() * press,
                            SegmentedIndicatorRefractionAmount.toPx() * press,
                            chromaticAberration = true,
                        )
                    }
                },
                highlight = { Highlight.Default.copy(alpha = animation.pressProgress) },
                shadow = { Shadow(alpha = 0.72f * animation.pressProgress) },
                innerShadow = {
                    InnerShadow(
                        radius = 6.dp * animation.pressProgress,
                        alpha = 0.72f * animation.pressProgress,
                    )
                },
                onDrawSurface = {
                    val press = animation.pressProgress
                    // 静止底色属于这个表面，不属于它的采样源。
                    drawRect((if (isLight) Color.White else Color(0xFF636366)).copy(alpha = 1f - press))
                    drawRect(Color.White.copy(alpha = 0.10f * press))
                    // 按下时把标签按固定轨道位置画回指示器上。
                    labelLayouts.forEachIndexed { index, label ->
                        drawText(
                            textLayoutResult = label,
                            topLeft = Offset(
                                size.width / 2f + (index - animation.value) * currentTabWidthPx *
                                    (if (isLtr) 1f else -1f) - label.size.width / 2f,
                                (size.height - label.size.height) / 2f,
                            ),
                            alpha = 1f - press,
                        )
                    }
                },
            )
        }

        // 1. 可见灰色轨道与标签。
        Row(
            Modifier
                .fillMaxSize()
                .clip(Capsule())
                .background(trackColor)
                .drawBehind {
                    if (appearance.reduceTransparency && selectedIndex >= 0) {
                        val visualIndex = if (isLtr) animation.value else items.lastIndex - animation.value
                        val pillHeight = indicatorHeight.toPx()
                        drawRoundRect(
                            color = if (isLight) Color.White else Color(0xFF636366),
                            topLeft = Offset(trackPaddingPx + visualIndex * tabWidthPx, trackPaddingPx),
                            size = Size(tabWidthPx.roundToInt().toFloat(), pillHeight),
                            cornerRadius = CornerRadius(pillHeight / 2f),
                        )
                    }
                }
                .then(segmentedHighlightModifier)
                .padding(2.dp),
        ) {
            SegmentedTabContent(
                items = items,
                selectedIndex = selectedIndex,
                onSelected = onSelected,
                enabled = enabled,
            )
        }

        // 2. 不缩放的隐藏源层，折射带才能越过边缘。
        if (appearance.usesBackdrop) Row(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(indicatorHeight)
                .clearAndSetSemantics { }
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .clip(Capsule())
                .background(trackColor)
                .then(hiddenGlassModifier)
                .then(segmentedHighlightModifier)
                .padding(horizontal = 2.dp),
        ) {
            SegmentedTabContent(
                items = items,
                selectedIndex = selectedIndex,
                onSelected = onSelected,
                enabled = enabled,
            )
        }

        // 3. 唯一的拖动面：盖在选中格上，按下时才变大。
        if (selectedIndex >= 0) Box(
            Modifier
                .graphicsLayer {
                    val visualIndex = if (isLtr) animation.value else items.lastIndex - animation.value
                    translationX = trackPaddingPx + visualIndex * tabWidthPx
                    translationY = trackPaddingPx
                    transformOrigin = TransformOrigin.Center
                }
                .then(if (enabled) animation.modifier else Modifier)
                .height(indicatorHeight)
                .clearAndSetSemantics { }
                .layout { measurable, constraints ->
                    val width = tabWidthPx.roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width),
                    )
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layout { measurable, constraints ->
                        val restingSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                        val visualSize = if (appearance.usesBackdrop) {
                            segmentedIndicatorSize(
                                restingSize, animation.scaleX, animation.scaleY,
                                animation.velocity / 10f, SegmentedIndicatorHorizontalExpansion.toPx(),
                            )
                        } else {
                            restingSize
                        }
                        val width = visualSize.width.roundToInt().coerceAtLeast(1)
                        val height = visualSize.height.roundToInt().coerceAtLeast(1)
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(
                                (constraints.maxWidth - width) / 2,
                                (constraints.maxHeight - height) / 2,
                            )
                        }
                    }
                    .then(indicatorGlassModifier),
            )
        }
    }
}

@Composable
private fun <T> RowScope.SegmentedTabContent(
    items: List<Pair<T, String>>,
    selectedIndex: Int,
    onSelected: (T) -> Unit,
    enabled: Boolean,
) {
    val colors = LocalGlassColors.current
    items.forEachIndexed { index, (key, label) ->
        val isSelected = index == selectedIndex
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(Capsule())
                .selectable(
                    selected = isSelected,
                    enabled = enabled,
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = {
                        if (enabled) onSelected(key)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = IosTypography.caption,
                color = colors.content,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
