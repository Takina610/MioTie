package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.huanchengfly.tieba.post.ui.liquidglass.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.shapes.Capsule
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LocalIosPopupMenuInteractive = staticCompositionLocalOf { true }

// AnimationKit MorphAnimationSettings.liquidMorph，响应 0.3s、阻尼 0.8。
private val DropdownMorphSpring = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = 438.64908f,
    visibilityThreshold = 0.001f,
)

/**
 * 弹出窗四周的溢出余量：弹簧过冲、速度形变、模糊溢出和投影都要画在静止菜单的边界之外，
 * 但不能被 Popup 窗口裁掉。可见菜单会再偏移回锚点边缘，位置与物理参数与原生一致。
 */
private val PopupMenuShadowMargin = NavigationGlassMaterial.RegularMedium.let {
    it.shadowRadius * 1.5f + it.shadowOffsetY + 1.dp
}
private val PopupMenuOvershootMarginStart = maxOf(64.dp, PopupMenuShadowMargin)
private val PopupMenuOvershootMarginVertical = maxOf(32.dp, PopupMenuShadowMargin)
private val PopupMenuWidth = 238.dp
private val PopupMenuItemHeight = 44.dp

/** 稳定外壳比静止菜单大一圈，弹簧过冲（最高约 1.12 倍）才能在约束内真正画出来。 */
private const val PopupMenuOvershootScale = 1.15f

private class IosPopupPositionProvider(
    private val targetMenuHeightPx: Int,
    private val targetMenuWidthPx: Int,
    private val horizontalInsetPx: Int,
    private val horizontalOvershootMarginPx: Int,
    private val visualHostWidthPx: Int,
    private val visualHostHeightPx: Int,
    private val anchorOvershootVerticalPx: Int,
    private val onDirectionResolved: (opensAbove: Boolean) -> Unit,
) : PopupPositionProvider {
    private var opensAbove: Boolean? = null

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val resolvedDirection = opensAbove ?: run {
            val roomBelow = windowSize.height - anchorBounds.bottom
            val roomAbove = anchorBounds.top
            (roomBelow < targetMenuHeightPx && roomAbove > roomBelow).also {
                opensAbove = it
                onDirectionResolved(it)
            }
        }
        // 约束的是可见菜单，而不是透明的动画外壳。外壳可以超出屏幕，不能被夹住。
        val inset = horizontalInsetPx.coerceAtMost(
            ((windowSize.width - targetMenuWidthPx) / 2).coerceAtLeast(0),
        )
        val requestedLeft = anchorBounds.right - targetMenuWidthPx
        val menuLeft = requestedLeft.coerceIn(
            inset,
            (windowSize.width - targetMenuWidthPx - inset).coerceAtLeast(inset),
        )
        val menuLeftInHost = if (layoutDirection == LayoutDirection.Rtl) {
            horizontalOvershootMarginPx
        } else {
            visualHostWidthPx - targetMenuWidthPx
        }
        val x = menuLeft - menuLeftInHost
        val menuTopInHost = if (resolvedDirection) {
            visualHostHeightPx - targetMenuHeightPx
        } else {
            anchorOvershootVerticalPx
        }
        val requestedTop = if (resolvedDirection) {
            anchorBounds.bottom - targetMenuHeightPx
        } else {
            anchorBounds.top
        }
        val menuTop = requestedTop.coerceIn(0, (windowSize.height - targetMenuHeightPx).coerceAtLeast(0))
        val y = menuTop - menuTopInHost
        return IntOffset(x, y)
    }
}

/**
 * 行尾下拉按钮（逐行照搬 Glasmio `GlasmioPopUpButton` + `GlasmioMenu` 紧凑路径）：
 * 强调色文字 + chevron 作为锚点，打开时锚点随 menuAlpha 淡出；菜单是 RegularMedium 玻璃材质，
 * 从锚点角落 0.2→1 弹簧展开，关闭时收回后再移除。
 */
@Composable
fun <T> IosPopUpButton(
    selected: T,
    items: List<T>,
    onSelected: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: Backdrop = LocalGlassBackdrop.current,
) {
    val colors = LocalGlassColors.current
    var expanded by remember { mutableStateOf(false) }
    IosDropdownMenu(
        expanded = expanded,
        onExpandedChange = { if (enabled || !it) expanded = it },
        itemCount = items.size,
        modifier = modifier,
        backdrop = backdrop,
        anchor = { openMenu ->
            Row(
                Modifier
                    .height(PopupMenuItemHeight)
                    .clickable(
                        enabled = enabled,
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = openMenu,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = label(selected),
                    style = IosTypography.body,
                    color = if (enabled) colors.accent else colors.secondaryContent,
                )
                SfIcon(
                    "chevron.up.chevron.down",
                    contentDescription = null,
                    size = 18.dp,
                    fontSize = 18.sp,
                    tint = if (enabled) colors.accent else colors.secondaryContent,
                    modifier = Modifier.size(width = 14.dp, height = 18.dp),
                    weight = FontWeight.Normal,
                )
            }
        },
    ) { childBackdrop, close ->
        items.forEach { item ->
            IosMenuItem(
                title = label(item),
                onClick = {
                    onSelected(item)
                    close()
                },
                systemName = if (item == selected) "checkmark" else null,
                backdrop = childBackdrop,
            )
        }
    }
}

@Composable
private fun IosDropdownMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    itemCount: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    anchor: @Composable (onClick: () -> Unit) -> Unit,
    content: @Composable ColumnScope.(LayerBackdrop, close: () -> Unit) -> Unit,
) {
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    var popupAlive by remember { mutableStateOf(false) }
    var opensAbove by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val menuAlpha = remember { Animatable(0f) }

    LaunchedEffect(expanded) {
        if (expanded) {
            popupAlive = true
            coroutineScope {
                launch { progress.animateTo(1f, DropdownMorphSpring) }
                launch { menuAlpha.animateTo(1f, DropdownMorphSpring) }
            }
        } else if (popupAlive) {
            coroutineScope {
                launch { progress.animateTo(0f, DropdownMorphSpring) }
                launch { menuAlpha.animateTo(0f, DropdownMorphSpring) }
            }
            popupAlive = false
        }
    }

    val anchorFadePaint = remember { Paint() }

    Box(modifier.onSizeChanged { anchorSize = it }) {
        Box(
            Modifier.drawWithContent {
                val alpha = 1f - menuAlpha.value.coerceIn(0f, 1f)
                when {
                    alpha <= 0f -> Unit
                    alpha >= 1f -> drawContent()
                    else -> {
                        anchorFadePaint.alpha = alpha
                        drawContext.canvas.saveLayer(Rect(Offset.Zero, size), anchorFadePaint)
                        drawContent()
                        drawContext.canvas.restore()
                    }
                }
            },
        ) {
            anchor { onExpandedChange(!expanded) }
        }

        if (popupAlive && anchorSize != IntSize.Zero) {
            val density = LocalDensity.current
            val menuHeight = 20.dp + PopupMenuItemHeight * itemCount
            val targetMenuHeightPx = with(density) { menuHeight.roundToPx() }
            val targetMenuWidthPx = with(density) { PopupMenuWidth.roundToPx() }
            val horizontalInsetPx = with(density) { 12.dp.roundToPx() }
            val visualHostWidthPx = with(density) {
                (PopupMenuOvershootMarginStart + PopupMenuWidth * PopupMenuOvershootScale).roundToPx()
            }
            val visualHostHeightPx = with(density) {
                (PopupMenuOvershootMarginVertical + menuHeight * PopupMenuOvershootScale).roundToPx()
            }
            val positionProvider = remember(
                targetMenuHeightPx,
                targetMenuWidthPx,
                horizontalInsetPx,
                visualHostWidthPx,
                visualHostHeightPx,
            ) {
                IosPopupPositionProvider(
                    targetMenuHeightPx = targetMenuHeightPx,
                    targetMenuWidthPx = targetMenuWidthPx,
                    horizontalInsetPx = horizontalInsetPx,
                    horizontalOvershootMarginPx = with(density) {
                        PopupMenuOvershootMarginStart.roundToPx()
                    },
                    visualHostWidthPx = visualHostWidthPx,
                    visualHostHeightPx = visualHostHeightPx,
                    anchorOvershootVerticalPx = with(density) {
                        PopupMenuOvershootMarginVertical.roundToPx()
                    },
                ) { resolved -> opensAbove = resolved }
            }
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(
                    focusable = expanded,
                    dismissOnBackPress = expanded,
                    dismissOnClickOutside = expanded,
                    clippingEnabled = false,
                ),
            ) {
                // 溢出余量是透明窗口区域，点在这里也按“点击外部”处理。
                Box(Modifier, contentAlignment = AbsoluteAlignment.TopLeft) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable(interactionSource = null, indication = null) {
                            onExpandedChange(false)
                        },
                    )
                    CompositionLocalProvider(LocalIosPopupMenuInteractive provides expanded) {
                        IosContextMenu(
                            visible = true,
                            backdrop = backdrop,
                            animationProgress = progress.value,
                            menuAlpha = menuAlpha.value,
                            opensAbove = opensAbove,
                            itemCount = itemCount,
                            heightOverride = menuHeight,
                        ) { childBackdrop ->
                            content(childBackdrop) { onExpandedChange(false) }
                        }
                    }
                }
            }
        }
    }
}

/** RegularMedium 材质的上下文菜单表面；紧凑路径下阴影不单独做动画。 */
@Composable
private fun IosContextMenu(
    visible: Boolean,
    backdrop: Backdrop,
    animationProgress: Float,
    menuAlpha: Float,
    opensAbove: Boolean,
    itemCount: Int,
    heightOverride: Dp?,
    menuWidth: Dp = PopupMenuWidth,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(LayerBackdrop) -> Unit,
) {
    if (!visible) return
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val pressProgress = remember(interactiveHighlight) {
        derivedStateOf { interactiveHighlight.pressProgress }
    }
    val progress = animationProgress.coerceIn(0f, 1f)
    val menuHeight = heightOverride ?: (20.dp + PopupMenuItemHeight * itemCount)
    val radius = 34.dp
    val menuShape = remember(radius) { ContinuousRoundedRectangle(radius) }
    val transitionScale = 0.2f + 0.8f * animationProgress
    val startOrigin = TransformOrigin(1f, if (opensAbove) 1f else 0f)
    val childBackdrop = rememberLayerBackdrop()
    var samplingCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val samplingCoordinatesProvider = remember { { samplingCoordinates } }
    // Popup 是独立窗口，必须跨窗口采样；采样点固定在未缩放的宿主上，再显式补偿动画变换。
    val samplingBackdrop = if (appearance.usesBackdrop) {
        rememberStationaryBackdrop(backdrop, samplingCoordinatesProvider)
    } else {
        emptyBackdrop()
    }

    val menuLayerBlock: GraphicsLayerScope.() -> Unit = {
        transformOrigin = startOrigin
        applyGlassDragScale(
            pressProgress = interactiveHighlight.pressProgress,
            offset = interactiveHighlight.offset,
        )
        scaleX = transitionScale * scaleX
        scaleY = transitionScale * scaleY
        translationX *= transitionScale
        translationY *= transitionScale
    }

    val popupHostWidth = PopupMenuOvershootMarginStart * 2 + menuWidth * PopupMenuOvershootScale
    val popupHostHeight = PopupMenuOvershootMarginVertical * 2 + menuHeight * PopupMenuOvershootScale
    Box(
        modifier
            // 保留完整的阴影壳，即使它超出 Popup 窗口。
            .wrapContentSize(AbsoluteAlignment.TopLeft, unbounded = true)
            .requiredSize(width = popupHostWidth, height = popupHostHeight)
            // 入场模糊固定在稳定 host 上，否则会露出一个菜单大小的矩形。
            .blur(
                (8.dp * (1f - progress)).coerceAtLeast(0.dp),
                BlurredEdgeTreatment.Unbounded,
            )
            .graphicsLayer { alpha = menuAlpha.coerceIn(0f, 1f) },
    ) {
        Box(
            Modifier
                .align(if (opensAbove) Alignment.TopStart else Alignment.BottomStart)
                .size(
                    width = PopupMenuOvershootMarginStart + menuWidth * PopupMenuOvershootScale,
                    height = PopupMenuOvershootMarginVertical + menuHeight * PopupMenuOvershootScale,
                ),
        ) {
            Box(
                Modifier
                    .align(if (opensAbove) Alignment.BottomEnd else Alignment.TopEnd)
                    .size(width = menuWidth, height = menuHeight)
                    .onGloballyPositioned { coordinates -> samplingCoordinates = coordinates },
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .navigationGlassBackground(
                            backdrop = samplingBackdrop,
                            exportedBackdrop = childBackdrop.takeIf { appearance.usesBackdrop },
                            shape = { menuShape },
                            containerColor = colors.container,
                            navigationMaterial = true,
                            isDark = colors.isDark,
                            blurRadius = NavigationGlassMaterial.RegularMedium.frostRadius,
                            material = NavigationGlassMaterial.RegularMedium,
                            appearance = appearance,
                            pressProgressState = pressProgress,
                            highlightAlphaMultiplier = progress,
                            shadowAlpha = 1f,
                            layerBlock = menuLayerBlock,
                        )
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                        .clip(menuShape)
                        .padding(10.dp),
                ) {
                    content(childBackdrop)
                }
            }
        }
    }
}

/** 菜单行：固定 20dp 勾选槽保证标题对齐；按下是 15% 黑胶囊高亮。 */
@Composable
private fun IosMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    systemName: String? = null,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    enabled: Boolean = true,
) {
    val colors = LocalGlassColors.current
    val scope = rememberCoroutineScope()
    val interactive = LocalIosPopupMenuInteractive.current
    val highlight = remember(scope) { InteractiveHighlight(scope) }
    Row(
        modifier
            .fillMaxWidth()
            .height(PopupMenuItemHeight)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .semantics(mergeDescendants = true) {
                if (!enabled) disabled()
            }
            .background(
                color = Color.Black.copy(alpha = 0.15f * highlight.pressProgress),
                shape = Capsule(),
            )
            .then(
                if (interactive && enabled) {
                    Modifier
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                        .then(highlight.gestureModifier)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) {
            if (systemName != null) {
                SfIcon(systemName, null, size = 20.dp, tint = colors.content)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = IosTypography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.content,
            )
        }
    }
}
