package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.Build
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import dev.chrisbanes.haze.HazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** 导航胶囊高度（展开态）。 */
val DockNavBarHeight = 60.dp

/** 导航胶囊高度（下沉态）：与个人信息条等高。 */
val DockCompactHeight = 44.dp

/** 导航胶囊内左右留白：首末槽位不贴胶囊边缘。 */
internal val DockInnerHorizontalPadding = 6.dp

/** 任意按压时壳整体放大的宽度量（按壳宽比例缩放）。 */
internal val DockPressBloomDp = 16.dp

/** 单击/再点刷新的连点保护窗口。 */
internal const val DockClickGuardMillis = 250L

/** 拖拽到边界后整个面板的 rubber-band 跟手位移上限。 */
private val DockRubberBand = 4.dp

/** 下沉进度阈值：超过即视为收缩态（点击让位给「展开」热区）。 */
private const val DockCompactThreshold = 0.02f

/** 背景模糊（RenderEffect / haze）需要 API 31+。 */
fun supportsBackdropBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * 悬浮玻璃导航胶囊（布局对齐 MeiloX，交互对齐原版 MioTie 底栏手感）：
 *
 * - 按下：壳按宽度比例整体放大 + 外圈光晕增强；按住当前项时指示器 bloom、轮廓光亮起；
 * - 长按后拖动：指示器跟随手指在槽位间滑动，带速度形变（scaleX 拉伸 / scaleY 压扁）
 *   与 4dp rubber-band 跟手；松手弹簧落定并切换；
 * - 单击：切换，再次点击当前项触发刷新；
 * - 下沉态：胶囊收成当前页的圆形按钮（无选中色/指示器/名称），整颗可点；
 *   展开前先把指示器对齐到当前页，展开后高亮即落在当前 tab。
 *
 * @param expandedWidth 展开态胶囊宽度，用于计算固定槽位（下沉动画期间槽位不变）
 */
@Composable
fun FloatingDock(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    onExpand: () -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    hazeState: HazeState,
    compactProgress: Float,
    expandedWidth: Dp,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val blurSupported = supportsBackdropBlur()
    val itemCount = navigationItems.size
    val maxIndex = (itemCount - 1).coerceAtLeast(0)
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val compact = compactProgress.coerceIn(0f, 1f)
    val compacted = compact > DockCompactThreshold
    val capsuleHeight = lerp(DockNavBarHeight, DockCompactHeight, compact)
    val indicatorHeight = (capsuleHeight - DockIndicatorVerticalInset * 2).coerceAtLeast(1.dp)

    // pointerInput 的 block 只在 key 变化时重启，回调与选中值需经 rememberUpdatedState 取最新
    val currentPositionLatest = rememberUpdatedState(currentPosition)
    val onChangePositionLatest = rememberUpdatedState(onChangePosition)
    val hapticFeedbackLatest = rememberUpdatedState(hapticFeedback)
    val navigationItemsLatest = rememberUpdatedState(navigationItems)
    val compactedLatest = rememberUpdatedState(compacted)

    val density = LocalDensity.current
    val innerPaddingPx = with(density) { DockInnerHorizontalPadding.toPx() }
    val slotWidthPx = with(density) {
        ((expandedWidth - DockInnerHorizontalPadding * 2) / itemCount).toPx()
    }
    val bloomPx = with(density) { DockPressBloomDp.toPx() }
    val rubberBandPx = with(density) { DockRubberBand.toPx() }

    // 指示器位置（浮点槽位索引）：拖拽中直接跟随手指，其余 spring 落定。
    val dragPosition = remember { Animatable(currentPosition.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    // 被按压的槽位（-1 = 无）：壳 bloom 由任意按压触发；
    // 指示器 bloom 仅「按下当前选中项」触发；按下的条目图标单独放大。
    var pressedIndex by remember { mutableIntStateOf(-1) }
    val indicatorScaleX = remember { Animatable(1f) }
    val indicatorScaleY = remember { Animatable(1f) }
    val pressProgress = remember { Animatable(0f) }
    // 速度形变（归一化槽位速度）
    val velocity = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }
    // rubber-band 面板跟手位移（累计拖拽量，松手回弹）
    val panelOffsetRaw = remember { Animatable(0f) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // 外部选中变化（点击切换 / 返回键回首页 / 展开）→ 指示器 spring 落定
    LaunchedEffect(currentPosition, maxIndex) {
        dragPosition.animateTo(
            targetValue = currentPosition.coerceIn(0, maxIndex).toFloat(),
            animationSpec = spring(dampingRatio = 1f, stiffness = 1000f),
        )
    }
    // 任意槽位按下 → 壳 bloom（按壳宽比例整体放大）
    LaunchedEffect(pressedIndex >= 0) {
        pressProgress.animateTo(
            targetValue = if (pressedIndex >= 0) 1f else 0f,
            animationSpec = spring(dampingRatio = 1f, stiffness = 1000f),
        )
    }
    // 仅按下当前选中项 → 指示器 bloom
    val indicatorPressed = pressedIndex == currentPosition && !compacted
    LaunchedEffect(indicatorPressed) {
        val target = if (indicatorPressed) DockIndicatorPressScale else 1f
        launch { indicatorScaleX.animateTo(target, spring(0.6f, 250f)) }
        launch { indicatorScaleY.animateTo(target, spring(0.7f, 250f)) }
    }

    val surfaceTint = themeColors.bottomBarSurface.copy(
        alpha = if (blurSupported) GlassSurfaceAlphaBlur else GlassSurfaceAlphaFallback
    )
    // rubber-band 面板偏移：拖拽越远偏移越大（4dp 上限，EaseOut 缓动）
    val panelOffsetPx: () -> Float = {
        val base = slotWidthPx * maxIndex.coerceAtLeast(1)
        if (base <= 0f) {
            0f
        } else {
            val fraction = (panelOffsetRaw.value / base).coerceIn(-1f, 1f)
            rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
        }
    }

    Box(modifier = modifier.height(capsuleHeight)) {
        // 玻璃壳（均匀材质；裁切到胶囊。指示器 bloom 越界由指示器层负责，壳不参与）
        GlassCapsule(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 1f +
                        bloomPx / expandedWidth.toPx().coerceAtLeast(1f) * pressProgress.value
                    scaleX = s
                    scaleY = s
                    translationX = panelOffsetPx()
                },
            shape = CircleShape,
            hazeState = hazeState,
            tint = surfaceTint,
            isNightMode = themeColors.isNightMode,
            blurSupported = blurSupported,
            glowProgress = pressProgress.value,
        ) {}

        // 选中指示器（可越界 bloom，跟随拖拽；视觉居中，几何上移补偿描边不对称）。
        // 下沉态整体淡出，圆形按钮不带任何选中样式。
        DockIndicator(
            positionPx = innerPaddingPx + dragPosition.value * slotWidthPx,
            widthPx = slotWidthPx,
            height = indicatorHeight,
            pressedScaleX = indicatorScaleX.value,
            pressedScaleY = indicatorScaleY.value,
            velocity = velocity.value,
            panelOffsetPx = panelOffsetPx,
            isNightMode = themeColors.isNightMode,
            hazeState = hazeState,
            blurSupported = blurSupported,
            surfaceTint = surfaceTint,
            indicatorAlpha = 1f - compact,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = -DockIndicatorLift),
        )

        // 槽位内容：名称常显，配色由指示器位置驱动（拖拽经过时实时切换）
        Layout(
            content = {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    val itemProgress = (1f - abs(index - dragPosition.value)).coerceIn(0f, 1f)
                    DockTab(
                        navigationItem = navigationItem,
                        selected = index == currentPosition,
                        progress = itemProgress,
                        compact = compact,
                        alpha = if (index == currentPosition) 1f else 1f - compact,
                        accent = MaterialTheme.colors.secondary,
                        baseColor = MaterialTheme.colors.onSurface,
                        pressed = pressedIndex == index,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val placeables = measurables.map {
                it.measure(Constraints.fixedWidth(slotWidthPx.roundToInt()))
            }
            layout(width, height) {
                // 下沉态四个槽位叠成一颗圆钮，槽位中心不动（展开时从中心弹出）
                val compactX = (width - slotWidthPx) / 2f
                placeables.forEachIndexed { index, placeable ->
                    val expandedX = innerPaddingPx + index * slotWidthPx
                    val x = expandedX + (compactX - expandedX) * compact
                    placeable.place(
                        x = x.roundToInt(),
                        y = ((height - placeable.height) / 2f).roundToInt(),
                    )
                }
            }
        }

        // 手势层（覆盖整条胶囊，语义与原版 MioTie 底栏一致）：
        // - 按下即刻进入拖拽（无 touch slop、无长按等待），指示器跟手滑动；
        //   松手按落点弹簧落定并切换；未移动则视为单击（切换 / 再次点击当前项刷新）；
        // - 下沉态：整颗圆钮即「展开」热区，先对齐指示器再展开。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(compacted, itemCount, slotWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // 起点槽位即按压项：壳 bloom + 指示器 bloom + 图标放大同步生效
                        pressedIndex =
                            slotAt(down.position.x, innerPaddingPx, slotWidthPx, itemCount)
                        isDragging = true
                        velocityTracker.resetTracking()
                        val pointerId = down.id
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.fastFirstOrNull { it.id == pointerId }
                            if (change == null) {
                                // 手势被系统中断：复位按压/拖拽状态，不触发切换
                                pressedIndex = -1
                                isDragging = false
                                break
                            }
                            val lastIndex = (itemCount - 1).coerceAtLeast(0)
                            if (change.changedToUpIgnoreConsumed()) {
                                pressedIndex = -1
                                isDragging = false
                                if (compactedLatest.value) {
                                    if (!moved) {
                                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                            dragPosition.snapTo(
                                                currentPositionLatest.value
                                                    .coerceIn(0, lastIndex)
                                                    .toFloat()
                                            )
                                        }
                                        onExpand()
                                    }
                                } else if (moved) {
                                    val target = dragPosition.value
                                        .roundToInt()
                                        .coerceIn(0, lastIndex)
                                    scope.launch {
                                        dragPosition.animateTo(
                                            target.toFloat(),
                                            spring(dampingRatio = 1f, stiffness = 1000f)
                                        )
                                    }
                                    if (target != currentPositionLatest.value) {
                                        hapticFeedbackLatest.value.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )
                                        onChangePositionLatest.value(target)
                                    }
                                } else {
                                    // 未移动＝单击：落点槽位切换 / 再次点击当前项刷新
                                    val index = slotAt(
                                        change.position.x,
                                        innerPaddingPx,
                                        slotWidthPx,
                                        itemCount,
                                    )
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastClickTime >= DockClickGuardMillis) {
                                        hapticFeedbackLatest.value.performHapticFeedback(
                                            HapticFeedbackType.TextHandleMove
                                        )
                                        if (index == currentPositionLatest.value) {
                                            onReselected(index)
                                        } else {
                                            onChangePositionLatest.value(index)
                                        }
                                        navigationItemsLatest.value[index].onClick?.invoke()
                                    }
                                    lastClickTime = currentTime
                                }
                                scope.launch {
                                    velocity.animateTo(
                                        0f,
                                        spring(dampingRatio = 0.5f, stiffness = 300f)
                                    )
                                }
                                scope.launch {
                                    panelOffsetRaw.animateTo(
                                        0f,
                                        spring(dampingRatio = 1f, stiffness = 300f)
                                    )
                                }
                                break
                            }
                            val delta = change.positionChange().x
                            if (compactedLatest.value || delta == 0f) continue
                            moved = true
                            val next = (dragPosition.value + delta / slotWidthPx)
                                .coerceIn(0f, lastIndex.toFloat())
                            // AwaitPointerEventScope 受限，不能直接调 Animatable.snapTo；
                            // UNDISPATCHED 确保位置在本次事件处理内同步落地
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                dragPosition.snapTo(next)
                            }
                            velocityTracker.addPosition(
                                SystemClock.uptimeMillis(),
                                Offset(next, 0f)
                            )
                            val slotVelocity = velocityTracker.calculateVelocity().x /
                                lastIndex.coerceAtLeast(1)
                            scope.launch {
                                velocity.animateTo(
                                    slotVelocity,
                                    spring(dampingRatio = 0.5f, stiffness = 300f)
                                )
                            }
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                panelOffsetRaw.snapTo(panelOffsetRaw.value + delta)
                            }
                            change.consume()
                        }
                    }
                }
                .clearAndSetSemantics {},
        )
    }
}

/** 手指 x 坐标落在哪个槽位（槽位中心归属自己）。 */
private fun slotAt(x: Float, innerPaddingPx: Float, slotWidthPx: Float, itemCount: Int): Int {
    if (slotWidthPx <= 0f) return 0
    val raw = (x - innerPaddingPx) / slotWidthPx
    return raw.toInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))
}