package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.main.NavigationItem
import com.kyant.backdrop.Backdrop
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
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

/** 白色柔光（Glasmio pressHighlightSpring = spring(0.5, 300)）。 */
private val DockGlowSpring = spring<Float>(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = 0.001f)

/** 回收阈值占整个 range 的比例（MeiloX DampedDragAnimation.release：2.5%）。 */
private const val DockReleaseFraction = 0.025f

/** 点击脉冲的放大峰值随距离收敛的指数：<1 让近距离点击放大更少、到位更早。 */
private const val DockBloomDistancePower = 0.6f

/** 背景模糊（RenderEffect）需要 API 31+。 */
fun supportsBackdropBlur(): Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

/**
 * 悬浮导航胶囊（照搬 MeiloX 关闭「液态玻璃」后的底栏）：
 *
 * - 静止：普通毛玻璃壳 + 10% 黑/白选中指示器，无折射、无描边、无外圈光晕；
 * - 按压：按住当前条目或拖动时壳按 16dp/宽度放大、指示器 bloom（78/56）并叠 8% 提亮 +
 *   15% 柔光，选中图标/名称退成前景色（对齐 MeiloX：按下的命中区就是指示器本身）；
 *   点按其他条目时没有按下反馈，抬手切换才由点击脉冲开始 bloom，与移动同时进行——
 *   近距离在放大到最大前就到位并立即回收，放大峰值随距离变化；
 *   指示器本身不采样背景，拖动时不会出现随页面内容变化的玻璃折射；
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
    // 白色柔光独立于按压脉冲：MeiloX 的 InteractiveHighlight 只响应按住指示器的交互，
    // 点击其他条目切换时不亮
    val highlightProgress = remember { Animatable(0f) }
    val velocity = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }
    val panelOffsetRaw = remember { Animatable(0f) }
    // 点击脉冲（对齐 MeiloX animateToValue → press() + 位置动画 + release()）：
    // 由手势直接触发、单协程独占，不走「改选中 → 重组 → LaunchedEffect」这一跳，
    // 脉冲不会被下一次重组打断在半途。
    var pulseJob by remember { mutableStateOf<Job?>(null) }

    suspend fun runPulse(target: Float, start: Float) {
        dragTarget = target
        // 放大峰值按距离给出：近距到位更早、放大还没到满就开始回落；远距移动更久、放大更充分。
        // 峰值显式算出而不只靠两个弹簧赛跑，否则掉帧时放大弹簧先跳到满格，近距离也变成满放大。
        val travel = abs(target - start)
        val fraction = if (lastIndex > 0) (travel / lastIndex).coerceIn(0f, 1f) else 1f
        val peak = 1f + (DockIndicatorPressScale - 1f) * fraction.pow(DockBloomDistancePower)
        coroutineScope {
            // press()：放大与位移同时开始（对齐 MeiloX animateToValue 的 press + 位置动画）。
            launch { pressProgress.animateTo(1f, DockPressSpring) }
            launch { indicatorScaleX.animateTo(peak, DockScaleXSpring) }
            launch { indicatorScaleY.animateTo(peak, DockScaleYSpring) }
            launch { dragPosition.animateTo(target, DockItemSpring) }

            // release()：照搬 MeiloX DampedDragAnimation.release 的时机——先让出一帧，
            // 位置进入 range 的 2.5% 才回收，回弹整体走完 0.6/0.7 × 250 的弹簧。
            // 唯一偏离的是初速度：MeiloX 回收那一刻放大仍在上升（正向动量），继承速度后
            // 先顺势头收尾再落下；这里点击会触发整页切换、主线程停帧近 200ms，等到位时
            // 放大早已越过峰值转为下降，继承下来是很大的负速度，回弹被压成两帧就没了。
            // 所以以零初速度起算，让回弹拿到与 MeiloX 相同的时长（峰值→静止约 180ms）。
            withFrameNanos { }
            if (dragPosition.value != target) {
                val threshold = lastIndex.coerceAtLeast(1) * DockReleaseFraction
                while (abs(dragPosition.value - target) >= threshold) {
                    withFrameNanos { }
                }
            }
            launch { indicatorScaleX.animateTo(1f, DockScaleXSpring, 0f) }
            launch { indicatorScaleY.animateTo(1f, DockScaleYSpring, 0f) }
            pressProgress.animateTo(0f, DockPressSpring)
        }
    }

    val runPulseLatest = rememberUpdatedState<(Float) -> Unit>({ target ->
        val start = dragPosition.value
        pulseJob?.cancel()
        // UNDISPATCHED：点击会触发整页切换，主线程随后被重组占住上百毫秒；
        // 若在这里排队等调度，脉冲的第一帧会被推迟到停帧之后，弹簧一次性跳完，
        // 看起来就是「瞬间到满格又开始消失」。
        pulseJob = scope.launch(start = CoroutineStart.UNDISPATCHED) { runPulse(target, start) }
    })

    // 拖动结束（对齐 MeiloX animateToValue）：指示器先落位，进入 2.5% 范围再回收放大，
    // 与按下 bloom 共用同一套弹簧；点击选中指示器也走这里，得到一次完整按压脉冲。
    fun settleDrag(target: Float) {
        dragTarget = target
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            dragPosition.animateTo(target, DockItemSpring)
        }
        scope.launch {
            withFrameNanos { }
            if (dragPosition.value != target) {
                val threshold = lastIndex.coerceAtLeast(1) * DockReleaseFraction
                while (abs(dragPosition.value - target) >= threshold) {
                    withFrameNanos { }
                }
            }
            launch { indicatorScaleX.animateTo(1f, DockScaleXSpring, 0f) }
            launch { indicatorScaleY.animateTo(1f, DockScaleYSpring, 0f) }
            highlightProgress.animateTo(0f, DockGlowSpring)
            pressProgress.animateTo(0f, DockPressSpring)
        }
    }

    // 外部选中变化（返回键回首页 / 展开 / 路由恢复）→ 指示器 spring 落定；
    // 点击切换由脉冲自己接管位置，这里不能重复 animateTo，否则会打断脉冲。
    LaunchedEffect(currentPosition, lastIndex) {
        val target = currentPosition.coerceIn(0, lastIndex).toFloat()
        if (isDragging) {
            dragTarget = target
            return@LaunchedEffect
        }
        if (dragPosition.value == target) return@LaunchedEffect
        if (pulseJob?.isActive == true) return@LaunchedEffect
        dragTarget = target
        dragPosition.animateTo(target, DockItemSpring)
    }

    val surfaceTint = glassDockTint(themeColors.isNightMode, blurSupported)
    val press = pressProgress.value.coerceIn(0f, 1f)
    val glow = highlightProgress.value.coerceIn(0f, 1f)
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
        // 只跟随「按住/拖动指示器」，点击切换 tab 的按压脉冲不点亮（对齐 MeiloX 命中区语义）。
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
                    if (glow > 0f) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.08f * glow),
                            blendMode = BlendMode.Plus,
                        )
                        drawRect(
                            brush = Brush.radialGradient(
                                0f to Color.White.copy(alpha = 0.15f * glow),
                                0.5f to Color.White.copy(alpha = 0.15f * glow),
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

        // 条目：名称常显；配色只由选中态决定。
        // 与 MeiloX 一致，每个条目各自是无涟漪 clickable；选中条目由上层指示器热区接管。
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
                        modifier = Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = !compacted,
                            role = Role.Tab,
                        ) {
                            if (index != currentPositionLatest.value) {
                                runPulseLatest.value(index.toFloat())
                                onChangePositionLatest.value(index)
                            } else {
                                onReselectedLatest.value(index)
                            }
                            navigationItemsLatest.value[index].onClick?.invoke()
                        },
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                // 与 MeiloX 的 Row padding(4dp) 一致：上下 4dp 是死区，tab 热区=指示器高度
                .padding(vertical = DockIndicatorVerticalMargin)
                .graphicsLayer {
                    scaleX = panelScale
                    scaleY = panelScale
                },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val placeables = measurables.map {
                it.measure(Constraints.fixed(itemWidthPx.roundToInt(), height))
            }
            layout(width, height) {
                // 下沉态所有条目叠成同一颗圆钮，槽位中心不动（展开时从中心弹出）
                val compactX = (width - itemWidthPx) / 2f
                placeables.forEachIndexed { index, placeable ->
                    val expandedX = innerPaddingPx + index * stepPx
                    val x = expandedX + (compactX - expandedX) * compact
                    placeable.place(x = x.roundToInt(), y = 0)
                }
            }
        }

        // 手势热区（照搬 MeiloX/Glasmio）：只有当前选中指示器那一格能触发按压与拖动，
        // 其他 tab 的按压/滑动完全交给各自的 clickable——从其他 tab 长按滑动不会呼出指示器。
        // 下沉态整颗圆钮即「展开」热区，按下给面板反馈，抬手展开。
        val gestureWidth = with(density) { itemWidthPx.toDp() }
        Box(
            modifier = Modifier
                .then(
                    if (compacted) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .offset {
                                IntOffset(
                                    x = (innerPaddingPx + dragPosition.value * stepPx).roundToInt(),
                                    y = DockIndicatorVerticalMargin.roundToPx(),
                                )
                            }
                            .width(gestureWidth)
                            .height(indicatorHeight)
                    }
                )
                .pointerInput(compacted, itemCount, itemWidthPx, stepPx, innerPaddingPx, viewConfiguration) {
                    var targetIndex = 0f
                    var travel = 0f
                    fun release() {
                        isDragging = false
                        scope.launch { velocity.animateTo(0f, DockVelocitySpring) }
                        scope.launch { panelOffsetRaw.animateTo(0f, DockVelocitySpring) }
                    }
                    inspectDragGestures(
                        onDragStart = {
                            pulseJob?.cancel()
                            targetIndex = dragPosition.value
                            travel = 0f
                            velocityTracker.resetTracking()
                            scope.launch { pressProgress.animateTo(1f, DockPressSpring) }
                            if (!compactedLatest.value) {
                                scope.launch {
                                    indicatorScaleX.animateTo(DockIndicatorPressScale, DockScaleXSpring)
                                }
                                scope.launch {
                                    indicatorScaleY.animateTo(DockIndicatorPressScale, DockScaleYSpring)
                                }
                                scope.launch { highlightProgress.animateTo(1f, DockGlowSpring) }
                            }
                        },
                        onDragEnd = {
                            if (compactedLatest.value) {
                                scope.launch { pressProgress.animateTo(0f, DockPressSpring) }
                                onExpandLatest.value()
                            } else {
                                val last = (itemCount - 1).coerceAtLeast(0)
                                val index = targetIndex.roundToInt().coerceIn(0, last)
                                release()
                                if (index != currentPositionLatest.value) {
                                    onChangePositionLatest.value(index)
                                } else if (travel <= viewConfiguration.touchSlop) {
                                    onReselectedLatest.value(index)
                                }
                                settleDrag(index.toFloat())
                            }
                        },
                        onDragCancel = {
                            if (compactedLatest.value) {
                                scope.launch { pressProgress.animateTo(0f, DockPressSpring) }
                            } else {
                                release()
                                val last = (itemCount - 1).coerceAtLeast(0)
                                val index = currentPositionLatest.value.coerceIn(0, last)
                                dragTarget = index.toFloat()
                                settleDrag(index.toFloat())
                            }
                        },
                    ) { _, dragAmount ->
                        travel += dragAmount.getDistance()
                        if (!compactedLatest.value) {
                            val delta = dragAmount.x
                            if (delta != 0f) {
                                isDragging = true
                                val last = (itemCount - 1).coerceAtLeast(0)
                                val next = (targetIndex + delta / stepPx.coerceAtLeast(1f))
                                    .coerceIn(0f, last.toFloat())
                                targetIndex = next
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
                            }
                        }
                    }
                },
        )
    }
}
