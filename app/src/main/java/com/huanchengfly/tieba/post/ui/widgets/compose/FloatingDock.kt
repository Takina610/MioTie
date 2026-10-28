package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.main.NavigationItem
import com.kyant.backdrop.Backdrop
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** 导航胶囊高度（展开态，对齐 Glasmio NavigationBarSurfaceHeight = 64dp）。 */
val DockNavBarHeight = 64.dp

/** 搜索圆钮直径（展开态，对齐 MeiloX NavigationBarSurfaceHeight 覆盖值 = 60dp）。 */
val DockSearchSize = 60.dp

/** 导航胶囊高度（下沉态）：与个人信息条等高。 */
val DockCompactHeight = 44.dp

/** 导航胶囊内左右留白（对齐 MeiloX 4dp padding）。 */
internal val DockInnerHorizontalPadding = 4.dp

/** 指示器高度 = 56dp（Glasmio ExpandedNavigationItemHeight = 64dp - 8dp）。 */
private val DockIndicatorVerticalMargin = 4.dp

/** 按压时壳整体放大宽度（对齐 MeiloX navigationPressScale 的 16dp expansion）。 */
internal val DockPressBloomDp = 16.dp

/** 拖拽到边界后整个面板的 rubber-band 跟手位移上限（= 内边距）。 */
private val DockRubberBand = DockInnerHorizontalPadding

/** 下沉进度阈值：超过即视为收缩态（点击让位给「展开」热区）。 */
private const val DockCompactThreshold = 0.02f

private val DockItemSpring = spring<Float>(dampingRatio = 1f, stiffness = 1000f, visibilityThreshold = 0.001f)
private val DockPressSpring = spring<Float>(dampingRatio = 1f, stiffness = 1000f, visibilityThreshold = 0.001f)
private val DockScaleXSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 250f, visibilityThreshold = 0.001f)
private val DockScaleYSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 250f, visibilityThreshold = 0.001f)
private val DockVelocitySpring = spring<Float>(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = 0.001f)

/** 背景模糊（RenderEffect）需要 API 31+。 */
fun supportsBackdropBlur(): Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

/**
 * 悬浮导航胶囊（照搬 MeiloX 关闭「液态玻璃」后的底栏）：
 *
 * - 静止：普通毛玻璃壳 + 10% 黑/白选中指示器，无折射、无描边、无外圈光晕；
 * - 按压：壳按 16dp/宽度轻微放大、指示器 bloom（78/56）、整条壳出现 8% 提亮 +
 *   指示器位置 15% 光斑，选中图标/名称退成前景色；壳上任意位置按下都触发
 *   （对齐 Glasmio：整条胶囊都是指示器的命中区）；指示器本身不采样背景，
 *   拖动时不会出现随页面内容变化的玻璃折射；
 * - 拖动：超过触摸 slop 且为水平意图才开始跟手，带速度形变与 4dp rubber-band；
 * - 点击：切换后指示器以 spring(1, 1000) 落位，并在移动前先做一次按压脉冲；
 *   进入目标 2.5% 范围即回收放大（对齐 MeiloX DampedDragAnimation.release）；
 * - 下沉态：胶囊收成当前页的圆形按钮，整颗可点，点击展开。
 *
 * @param expandedWidth 展开态胶囊宽度，用于计算固定条目宽度（下沉动画期间条目不变）
 */
@Composable
fun FloatingDock(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    onExpand: () -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    backdrop: Backdrop,
    compactProgress: Float,
    expandedWidth: Dp,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val blurSupported = supportsBackdropBlur()
    val itemCount = navigationItems.size
    val lastIndex = (itemCount - 1).coerceAtLeast(0)
    val scope = rememberCoroutineScope()
    val compact = compactProgress.coerceIn(0f, 1f)
    val compacted = compact > DockCompactThreshold
    val capsuleHeight = lerp(DockNavBarHeight, DockCompactHeight, compact)
    val indicatorHeight = (capsuleHeight - DockIndicatorVerticalMargin * 2).coerceAtLeast(1.dp)

    // pointerInput 的 block 只在 key 变化时重启，回调与选中值需经 rememberUpdatedState 取最新
    val currentPositionLatest = rememberUpdatedState(currentPosition)
    val onChangePositionLatest = rememberUpdatedState(onChangePosition)
    val onReselectedLatest = rememberUpdatedState(onReselected)
    val onExpandLatest = rememberUpdatedState(onExpand)
    val navigationItemsLatest = rememberUpdatedState(navigationItems)
    val compactedLatest = rememberUpdatedState(compacted)

    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val innerPaddingPx = with(density) { DockInnerHorizontalPadding.toPx() }
    val contentWidthPx = with(density) {
        (expandedWidth - DockInnerHorizontalPadding * 2).toPx()
    }.coerceAtLeast(1f)
    // MeiloX navigationTabGeometry：条目均分胶囊宽度，步长与条目宽度一致
    // （指示器左右边缘正好落在相邻条目中心）。
    val itemWidthPx = contentWidthPx / itemCount.coerceAtLeast(1)
    val stepPx = if (itemCount > 1) (contentWidthPx - itemWidthPx) / (itemCount - 1) else 0f
    val bloomPx = with(density) { DockPressBloomDp.toPx() }
    val rubberBandPx = with(density) { DockRubberBand.toPx() }

    // 指示器位置（浮点条目索引）：拖拽中直接跟随手指，其余 spring 落定。
    val dragPosition = remember { Animatable(currentPosition.toFloat()) }
    var dragTarget by remember { mutableFloatStateOf(currentPosition.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    val pressProgress = remember { Animatable(0f) }
    val indicatorScaleX = remember { Animatable(1f) }
    val indicatorScaleY = remember { Animatable(1f) }
    val velocity = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }
    val panelOffsetRaw = remember { Animatable(0f) }
    // 点击切换时先做一次按压脉冲（MeiloX animateToValue(animatePress = true)）
    var pulseSelection by remember { mutableStateOf(false) }

    // 外部选中变化（点击切换 / 返回键回首页 / 展开）→ 指示器 spring 落定
    LaunchedEffect(currentPosition, lastIndex) {
        val target = currentPosition.coerceIn(0, lastIndex).toFloat()
        if (isDragging) {
            dragTarget = target
            return@LaunchedEffect
        }
        if (dragPosition.value == target && !pulseSelection) return@LaunchedEffect
        dragTarget = target
        if (pulseSelection && !compacted) {
            pulseSelection = false
            launch { pressProgress.animateTo(1f, DockPressSpring) }
            launch { indicatorScaleX.animateTo(DockIndicatorPressScale, DockScaleXSpring) }
            launch { indicatorScaleY.animateTo(DockIndicatorPressScale, DockScaleYSpring) }
            launch { dragPosition.animateTo(target, DockItemSpring) }
            // 对齐 MeiloX DampedDragAnimation.release()：进入目标 2.5% 范围就回收放大，
            // 不等 spring 完全落定，否则指示器会在目标位置多保持放大一百多毫秒。
            val releaseThreshold = lastIndex.coerceAtLeast(1) * 0.025f
            snapshotFlow { dragPosition.value }
                .first { abs(it - target) < releaseThreshold }
            launch { indicatorScaleX.animateTo(1f, DockScaleXSpring) }
            launch { indicatorScaleY.animateTo(1f, DockScaleYSpring) }
            pressProgress.animateTo(0f, DockPressSpring)
        } else {
            dragPosition.animateTo(target, DockItemSpring)
        }
    }

    val surfaceTint = glassDockTint(themeColors.isNightMode, blurSupported)
    val press = pressProgress.value.coerceIn(0f, 1f)
    // rubber-band 面板偏移：拖拽越远偏移越大（4dp 上限，EaseOut 缓动）
    val panelOffsetPx: () -> Float = {
        val base = stepPx * lastIndex.coerceAtLeast(1)
        if (base <= 0f) {
            0f
        } else {
            val fraction = (panelOffsetRaw.value / base).coerceIn(-1f, 1f)
            rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
        }
    }
    // 壳按压放大（对齐 MeiloX navigationPressScale，仅展开态）
    val panelScale = 1f + bloomPx / with(density) { expandedWidth.toPx() }.coerceAtLeast(1f) *
        press * (1f - compact)
    // 壳以中心为轴放大，指示器要跟着平移，否则按压两端 tab 时胶囊与图标错位
    // （对齐 Glasmio navigationPanelCenterOffset）。
    val panelCenterPx = with(density) { expandedWidth.toPx() }.coerceAtLeast(1f) / 2f
    val indicatorPanelShiftPx =
        (innerPaddingPx + dragPosition.value * stepPx + itemWidthPx / 2f - panelCenterPx) *
            (panelScale - 1f)

    Box(modifier = modifier.height(capsuleHeight)) {
        // 玻璃壳（均匀材质；无描边、无外圈光晕）
        GlassCapsule(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = panelScale
                    scaleY = panelScale
                    translationX = panelOffsetPx()
                },
            shape = CircleShape,
            backdrop = backdrop,
            tint = surfaceTint,
            isNightMode = themeColors.isNightMode,
            blurSupported = blurSupported,
        ) {}

        // 按压光斑（MeiloX InteractiveHighlight）：整条壳 8% 提亮，以指示器为圆心叠 15% 柔光。
        // 指示器只做 10% 纯色填充，光斑可透过它显示。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = panelScale
                    scaleY = panelScale
                    translationX = panelOffsetPx()
                }
                .clip(CircleShape)
                .drawWithContent {
                    if (press > 0f) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.08f * press),
                            blendMode = BlendMode.Plus,
                        )
                        drawRect(
                            brush = Brush.radialGradient(
                                0f to Color.White.copy(alpha = 0.15f * press),
                                0.5f to Color.White.copy(alpha = 0.15f * press),
                                1f to Color.Transparent,
                                center = Offset(
                                    x = innerPaddingPx + dragPosition.value * stepPx + itemWidthPx / 2f,
                                    y = size.height / 2f,
                                ),
                                radius = size.minDimension * 1.5f,
                            ),
                            blendMode = BlendMode.Plus,
                        )
                    }
                },
        )

        // 选中指示器（MeiloX：纯 10% 填充，可越界 bloom；下沉态淡出）
        DockIndicator(
            positionPx = innerPaddingPx + dragPosition.value * stepPx + indicatorPanelShiftPx,
            widthPx = itemWidthPx,
            height = indicatorHeight,
            pressedScaleX = indicatorScaleX.value,
            pressedScaleY = indicatorScaleY.value,
            velocity = velocity.value,
            pressProgress = press,
            panelOffsetPx = panelOffsetPx,
            isNightMode = themeColors.isNightMode,
            indicatorAlpha = 1f - compact,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = -DockIndicatorLift),
        )

        // 条目：名称常显；配色只由选中态决定
        Layout(
            content = {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    DockTab(
                        navigationItem = navigationItem,
                        selected = index == currentPosition,
                        compact = compact,
                        alpha = if (index == currentPosition) 1f else 1f - compact,
                        accent = MaterialTheme.colors.secondary,
                        baseColor = MaterialTheme.colors.onSurface,
                        pressProgress = if (index == currentPosition) press else 0f,
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = panelScale
                    scaleY = panelScale
                },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val placeables = measurables.map {
                it.measure(Constraints.fixedWidth(itemWidthPx.roundToInt()))
            }
            layout(width, height) {
                // 下沉态所有条目叠成同一颗圆钮，槽位中心不动（展开时从中心弹出）
                val compactX = (width - itemWidthPx) / 2f
                placeables.forEachIndexed { index, placeable ->
                    val expandedX = innerPaddingPx + index * stepPx
                    val x = expandedX + (compactX - expandedX) * compact
                    placeable.place(
                        x = x.roundToInt(),
                        y = ((height - placeable.height) / 2f).roundToInt(),
                    )
                }
            }
        }

        // 手势层（对齐 MeiloX）：
        // - 按下选中透镜：壳放大 + 透镜 bloom + 按压高光；超过触摸 slop 后水平拖动，
        //   松手按落点弹簧落定并切换；未移动则视为点击（再次点击当前项刷新）；
        // - 按下其他条目：没有任何按压动效，抬手即切换；
        // - 下沉态：整颗圆钮即「展开」热区，先对齐指示器再展开。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(compacted, itemCount, itemWidthPx, stepPx, innerPaddingPx, viewConfiguration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val last = (itemCount - 1).coerceAtLeast(0)

                        if (compactedLatest.value) {
                            var moved = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.fastFirstOrNull { it.id == down.id }
                                    ?: break
                                if (change.positionChange().getDistance() > viewConfiguration.touchSlop) {
                                    moved = true
                                }
                                if (change.changedToUpIgnoreConsumed()) {
                                    if (!moved) {
                                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                            dragPosition.snapTo(
                                                currentPositionLatest.value.coerceIn(0, last).toFloat()
                                            )
                                        }
                                        onExpandLatest.value()
                                    }
                                    break
                                }
                            }
                            return@awaitEachGesture
                        }

                        // 整条胶囊都是指示器命中区（对齐 Glasmio indicatorHitBoundsModifier）：
                        // 任意位置按下都进入同一套按压/拖动状态。
                        scope.launch { pressProgress.animateTo(1f, DockPressSpring) }
                        scope.launch {
                            indicatorScaleX.animateTo(DockIndicatorPressScale, DockScaleXSpring)
                        }
                        scope.launch {
                            indicatorScaleY.animateTo(DockIndicatorPressScale, DockScaleYSpring)
                        }

                        var moved = false
                        var dragging = false
                        var cancelled = false
                        var totalDx = 0f
                        var totalDy = 0f
                        var upX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.fastFirstOrNull { it.id == down.id }
                            if (change == null) {
                                cancelled = true
                                break
                            }
                            if (change.changedToUpIgnoreConsumed()) {
                                upX = change.position.x
                                break
                            }
                            val delta = change.positionChange().x
                            if (!moved && !cancelled) {
                                totalDx += delta
                                totalDy += change.positionChange().y
                                if (totalDx * totalDx + totalDy * totalDy >
                                    viewConfiguration.touchSlop * viewConfiguration.touchSlop
                                ) {
                                    moved = true
                                    // 只有水平意图才接管拖动；否则让位给内容滚动
                                    dragging = abs(totalDx) > abs(totalDy)
                                    cancelled = !dragging
                                    if (dragging) {
                                        isDragging = true
                                        velocityTracker.resetTracking()
                                    } else {
                                        scope.launch { pressProgress.animateTo(0f, DockPressSpring) }
                                        scope.launch { indicatorScaleX.animateTo(1f, DockScaleXSpring) }
                                        scope.launch { indicatorScaleY.animateTo(1f, DockScaleYSpring) }
                                    }
                                }
                            }
                            if (dragging) {
                                val next = (dragTarget + delta / stepPx.coerceAtLeast(1f))
                                    .coerceIn(0f, last.toFloat())
                                dragTarget = next
                                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    dragPosition.animateTo(next, DockItemSpring)
                                }
                                velocityTracker.addPosition(
                                    SystemClock.uptimeMillis(),
                                    Offset(dragPosition.value, 0f),
                                )
                                val slotVelocity = velocityTracker.calculateVelocity().x /
                                    last.coerceAtLeast(1)
                                scope.launch {
                                    velocity.animateTo(slotVelocity, DockVelocitySpring)
                                }
                                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    panelOffsetRaw.snapTo(panelOffsetRaw.value + delta)
                                }
                                change.consume()
                            }
                        }

                        scope.launch { pressProgress.animateTo(0f, DockPressSpring) }
                        scope.launch { indicatorScaleX.animateTo(1f, DockScaleXSpring) }
                        scope.launch { indicatorScaleY.animateTo(1f, DockScaleYSpring) }

                        if (!dragging) {
                            if (!moved && !cancelled) {
                                val index = slotAt(upX, innerPaddingPx, itemWidthPx, stepPx, itemCount)
                                if (index == currentPositionLatest.value) {
                                    onReselectedLatest.value(index)
                                } else {
                                    pulseSelection = true
                                    onChangePositionLatest.value(index)
                                }
                                navigationItemsLatest.value[index].onClick?.invoke()
                            }
                        } else {
                            val target = dragTarget.roundToInt().coerceIn(0, last)
                            isDragging = false
                            dragTarget = target.toFloat()
                            scope.launch {
                                dragPosition.animateTo(target.toFloat(), DockItemSpring)
                            }
                            if (target != currentPositionLatest.value) {
                                onChangePositionLatest.value(target)
                            }
                            scope.launch { velocity.animateTo(0f, DockVelocitySpring) }
                            scope.launch {
                                panelOffsetRaw.animateTo(0f, DockVelocitySpring)
                            }
                        }
                    }
                }
                .clearAndSetSemantics {},
        )
    }
}

/** 手指 x 坐标落在哪个条目（按条目中心归属）。 */
private fun slotAt(
    x: Float,
    innerPaddingPx: Float,
    itemWidthPx: Float,
    stepPx: Float,
    itemCount: Int,
): Int {
    if (itemCount <= 1) return 0
    val raw = if (stepPx <= 0f) 0f else (x - innerPaddingPx - itemWidthPx / 2f) / stepPx
    return raw.roundToInt().coerceIn(0, itemCount - 1)
}
