package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlin.math.abs

private val IosWheelItemHeight = 36.dp
private val IosWheelHeight = 180.dp
private val IosWheelColumnWidth = 72.dp

/**
 * iOS 时间选择 sheet（照搬 Glasmio 的 sheet 结构）：
 * 顶部居中标题 + 右上角圆形 X 关闭按钮，中间时/分两列滚轮，
 * 底部一枚 50dp 胶囊主色「完成」按钮。
 * 滚轮按 36dp 行高吸顶，中央高亮条指示当前选中项。
 */
@Composable
fun IosTimePickerSheet(
    visible: Boolean,
    title: String,
    initialTime: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val (initialHour, initialMinute) = parseTime(initialTime)
    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = initialHour)
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinute)
    val colors = LocalGlassColors.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { IosWheelItemHeight.roundToPx() }
    val hour by remember(hourState, itemHeightPx) {
        derivedStateOf { hourState.currentIndex(itemHeightPx, 24) }
    }
    val minute by remember(minuteState, itemHeightPx) {
        derivedStateOf { minuteState.currentIndex(itemHeightPx, 60) }
    }

    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(44.dp)) {
                Text(
                    title,
                    style = IosTypography.headline,
                    color = colors.content,
                    modifier = Modifier.align(Alignment.Center),
                )
                IosSheetCloseButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(IosWheelHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.62f)
                        .height(IosWheelItemHeight)
                        .background(
                            colors.segmentedControlBackground,
                            ContinuousRoundedRectangle(10.dp),
                        )
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IosWheelColumn(state = hourState, count = 24, itemHeightPx = itemHeightPx)
                    Text(
                        ":",
                        style = IosTypography.headline,
                        color = colors.content,
                    )
                    IosWheelColumn(state = minuteState, count = 60, itemHeightPx = itemHeightPx)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "%02d:%02d".format(hour, minute),
                style = IosTypography.subheadline,
                color = colors.secondaryContent,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
            GlassButton(
                onClick = {
                    onConfirm(
                        "%02d:%02d".format(
                            hourState.currentIndex(itemHeightPx, 24),
                            minuteState.currentIndex(itemHeightPx, 60),
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                emphasis = GlassEmphasis.Prominent,
                height = 50.dp,
            ) {
                Text(
                    confirmLabel,
                    style = IosTypography.body,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** sheet 右上角的圆形关闭按钮（照搬 Glasmio `GlasmioSheetToolbarButton`）：44dp 玻璃圆钮，带按压物理。 */
@Composable
fun IosSheetCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGlassColors.current
    GlassSurface(
        modifier = modifier.size(LocalGlassDimensions.current.iconButtonSize),
        shape = CircleShape,
        navigationSurfaceColor = colors.elevatedBackground.copy(
            alpha = LocalGroupedListBackgroundAlpha.current.coerceIn(0f, 1f),
        ),
        navigationSurfaceAlphaMultiplier = 1f,
        sampleBackdrop = false,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        SfIcon("xmark", null, size = 24.dp, tint = colors.content)
    }
}

@Composable
private fun IosWheelColumn(
    state: LazyListState,
    count: Int,
    itemHeightPx: Int,
) {
    val colors = LocalGlassColors.current
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    val fractionalIndex by remember(state, itemHeightPx) {
        derivedStateOf {
            state.firstVisibleItemIndex +
                state.firstVisibleItemScrollOffset.toFloat() / itemHeightPx
        }
    }
    LazyColumn(
        state = state,
        flingBehavior = flingBehavior,
        modifier = Modifier
            .width(IosWheelColumnWidth)
            .height(IosWheelHeight),
        contentPadding = PaddingValues(vertical = (IosWheelHeight - IosWheelItemHeight) / 2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(count) { index ->
            val distance = abs(index - fractionalIndex)
            val emphasis = (1f - distance.coerceAtMost(2f) / 2f).coerceIn(0f, 1f)
            Box(
                Modifier
                    .height(IosWheelItemHeight)
                    .graphicsLayer {
                        alpha = 0.32f + 0.68f * emphasis
                        scaleX = 0.92f + 0.16f * emphasis
                        scaleY = 0.92f + 0.16f * emphasis
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%02d".format(index),
                    style = IosTypography.headline,
                    color = colors.content,
                )
            }
        }
    }
}

private fun LazyListState.currentIndex(itemHeightPx: Int, count: Int): Int {
    if (itemHeightPx <= 0) return 0
    val fractional = firstVisibleItemIndex + firstVisibleItemScrollOffset.toFloat() / itemHeightPx
    // 用截断而非 roundToInt：滚轮静止时 offset 恰为 0，roundToInt 会把 13.5 进位成 14。
    return (fractional + 0.5f).toInt().coerceIn(0, count - 1)
}

private fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour to minute
}
