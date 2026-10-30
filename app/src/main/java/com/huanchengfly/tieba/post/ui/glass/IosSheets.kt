package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.huanchengfly.tieba.post.ui.liquidglass.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.shapes.Capsule

/** Figma 10525:1632：大 sheet 的顶角 34dp、底角 58dp。 */
val IosModalSheetShape: Shape = ContinuousRoundedRectangle(
    topStart = 34.dp,
    topEnd = 34.dp,
    bottomStart = 58.dp,
    bottomEnd = 58.dp,
)

/**
 * iOS 风格的玻璃底部 sheet（照搬 Glasmio `GlasmioBottomSheet`）：
 * 透明的 M3 sheet 宿主 + 自绘玻璃表面，顶部带抓握条，底部让到系统导航栏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // 宿主不裁剪，拖动缩放的溢出交给外层轮廓；圆角由 IosSheetSurface 自己画。
        modifier = modifier.graphicsLayer {
            clip = false
            applyGlassDragScale(
                pressProgress = interactiveHighlight.pressProgress,
                offset = interactiveHighlight.offset,
            )
        },
        containerColor = Color.Transparent,
        contentColor = colors.content,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        shape = RectangleShape,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.statusBars },
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.apply {
                navigationBarColor = android.graphics.Color.TRANSPARENT
                navigationBarDividerColor = android.graphics.Color.TRANSPARENT
                isNavigationBarContrastEnforced = false
            }
        }
        IosSheetSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = IosModalSheetShape,
            interactiveHighlight = interactiveHighlight,
            applyDragScale = false,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                IosSheetHandle()
                content()
            }
        }
    }
}

/** sheet 顶部的抓握条（58×4dp，tertiaryContent 55%）。 */
@Composable
fun IosSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(width = 58.dp, height = 4.dp)
                .background(
                    LocalGlassColors.current.tertiaryContent.copy(alpha = 0.55f),
                    Capsule(),
                ),
        )
    }
}

/** sheet 的玻璃底板（照搬 Glasmio `GlasmioSheetSurface`：RegularLarge 材质 + 四分之一分辨率场景模糊）。 */
@Composable
fun IosSheetSurface(
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    shape: Shape = ContinuousRoundedRectangle(38.dp),
    interactiveHighlight: InteractiveHighlight? = null,
    applyDragScale: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val animationScope = rememberCoroutineScope()
    val activeInteractiveHighlight = interactiveHighlight ?: remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val pressProgress = remember(activeInteractiveHighlight) {
        derivedStateOf { activeInteractiveHighlight.pressProgress }
    }
    val sheetLayerBlock: (GraphicsLayerScope.() -> Unit)? = if (applyDragScale) {
        {
            applyGlassDragScale(
                pressProgress = activeInteractiveHighlight.pressProgress,
                offset = activeInteractiveHighlight.offset,
            )
        }
    } else {
        null
    }
    // Sheet 渲染在 Dialog 窗口里，共享 backdrop 在应用窗口，必须用屏幕坐标跨窗口采样；
    // 再把整幅场景降采样模糊一次，玻璃透镜才有真实内容可折射。
    val samplingBackdrop = rememberCrossWindowBackdrop(backdrop)
    val backdropRenderer = if (appearance.usesBackdrop) {
        rememberSheetBackdropRenderer(samplingBackdrop, NavigationGlassMaterial.RegularLarge.frostRadius)
    } else {
        emptyBackdrop()
    }
    Box(
        modifier
            .fillMaxWidth()
            .navigationGlassBackground(
                backdrop = backdropRenderer,
                shape = { shape },
                containerColor = colors.container,
                navigationMaterial = true,
                isDark = colors.isDark,
                // 场景模糊已由 SheetBackdropRenderer 完成。
                blurRadius = 0.dp,
                appearance = appearance,
                material = NavigationGlassMaterial.RegularLarge,
                pressProgressState = pressProgress,
                layerBlock = sheetLayerBlock,
            )
            .then(activeInteractiveHighlight.modifier)
            .then(activeInteractiveHighlight.gestureModifier),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides colors.content,
                LocalGlassContentColor provides colors.content,
                LocalGroupedListBackgroundAlpha provides
                    if (appearance.usesBackdrop) SheetGroupedListBackgroundAlpha else 1f,
            ) {
                content()
            }
        },
    )
}

/**
 * iOS 单选 action sheet（照搬 Glasmio `GlasmioActionSheet` 的结构）：
 * 居中的分组标题 + 一张选项卡片 + 独立的取消胶囊。
 */
@Composable
fun IosChoiceSheet(
    visible: Boolean,
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String,
) {
    if (!visible) return
    val colors = LocalGlassColors.current
    IosBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
        ) {
            Text(
                title,
                style = IosTypography.subheadline,
                color = colors.secondaryContent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
            IosGroupedList {
                options.forEachIndexed { index, (value, label) ->
                    IosSheetOptionRow(
                        title = label,
                        selected = value == selected,
                        showTopSeparator = index != 0,
                        onClick = {
                            onSelect(value)
                            onDismiss()
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            IosSheetActionButton(
                text = cancelLabel,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IosSheetOptionRow(
    title: String,
    selected: Boolean,
    showTopSeparator: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalGlassColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .drawBehind {
                if (showTopSeparator) {
                    drawLine(
                        colors.separator,
                        Offset(16.dp.toPx(), 0f),
                        Offset(size.width - 16.dp.toPx(), 0f),
                        1.dp.toPx(),
                    )
                }
            }
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = IosTypography.body,
            color = colors.content,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            SfIcon("checkmark", null, size = 20.dp, tint = colors.accent)
        }
    }
}

/** sheet 底部的独立动作胶囊（取消/完成），几何对齐 Glasmio 的 48dp alert 按钮。 */
@Composable
fun IosSheetActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalGlassColors.current
    val base = when {
        prominent -> colors.prominentContainer
        colors.isDark -> Color(0x52787880)
        else -> Color(0x28787880)
    }
    // 底色本身带 alpha，缩放时乘上去而不是覆盖（覆盖会变成不透明灰，像 MD3 按钮）。
    val fill = base.copy(alpha = base.alpha * if (enabled) 1f else 0.45f)
    Box(
        modifier
            .height(48.dp)
            .clip(Capsule())
            .background(fill)
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = IosTypography.headline,
            color = if (prominent) Color.White else colors.content,
        )
    }
}

/** 圆角背景块：供 sheet 内的输入框/表单复用。 */
@Composable
fun IosFieldGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(26.dp))
            .background(
                if (colors.isDark) Color.White.copy(alpha = 0.16f) else Color(0x28787880)
            ),
        content = content,
    )
}

