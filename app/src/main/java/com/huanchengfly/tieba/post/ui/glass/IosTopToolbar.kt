package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.kyant.shapes.Capsule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * iOS 顶栏（照抄 MeiloX 1.55.1 `Ios27Components.IosTopToolbar`）：
 * 标题按左右按钮宽度光学居中，大标题折叠时随 [collapseProgress] 缩放淡出。
 */
enum class IosTopBarStyle { Default, CompactLargeTitle, LargeTitle, TwoLine, TwoLineLeading }

enum class IosBottomToolbarStyle { Text, Symbols, Search }

private val IosTopToolbarActionGap = 8.dp

/** The five top-toolbar variants from Figma node 5661:41970. */
@Composable
fun IosTopToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    style: IosTopBarStyle = IosTopBarStyle.Default,
    collapseProgress: () -> Float = { 1f },
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CompositionLocalProvider(
        LocalGlassSurfaceBrightness provides 1f,
        LocalGlassSurfaceStyle provides GlassSurfaceStyle.Navigation,
        LocalContentColor provides LocalGlassColors.current.content,
    ) {
        when (style) {
            IosTopBarStyle.LargeTitle -> Column(modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { navigation?.invoke() }
                    Row(horizontalArrangement = Arrangement.spacedBy(IosTopToolbarActionGap), content = actions)
                }
                Column(Modifier.offset(y = (-10).dp)) {
                    Text(title, style = IosTypography.largeTitle, color = LocalGlassColors.current.content)
                    subtitle?.let { Text(it, style = IosTypography.subheadline, color = LocalGlassColors.current.secondaryContent) }
                }
            }
            IosTopBarStyle.CompactLargeTitle -> Row(
                modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = IosTypography.largeTitle,
                    color = LocalGlassColors.current.content,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(IosTopToolbarActionGap), content = actions)
            }
            else -> {
                var leadingWidth by remember { mutableIntStateOf(0) }
                var trailingWidth by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                val leadingPad = with(density) { leadingWidth.toDp() }
                val trailingPad = with(density) { trailingWidth.toDp() }
                val centeredTitleSidePadding = maxOf(leadingPad, trailingPad) +
                    if (leadingWidth > 0 || trailingWidth > 0) IosTopToolbarActionGap else 0.dp
                Box(modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp)) {
                    Row(
                        Modifier.align(Alignment.CenterStart).onSizeChanged { leadingWidth = it.width },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { navigation?.invoke() }
                    Column(
                        Modifier
                            .align(if (style == IosTopBarStyle.TwoLineLeading) Alignment.CenterStart else Alignment.Center)
                            .fillMaxWidth()
                            .padding(
                                // Centered styles stay optically centered: reserve the wider
                                // side on both edges, plus the same gap used between actions.
                                // The leading style just clears the sides.
                                start = if (style == IosTopBarStyle.TwoLineLeading) leadingPad
                                else centeredTitleSidePadding,
                                end = if (style == IosTopBarStyle.TwoLineLeading) trailingPad
                                else centeredTitleSidePadding,
                            )
                            .graphicsLayer {
                                val progress = collapseProgress().coerceIn(0f, 1f)
                                alpha = progress
                                val scale = 0.92f + 0.08f * progress
                                scaleX = scale
                                scaleY = scale
                            }
                            .blurBy { 8.dp * (1f - collapseProgress().coerceIn(0f, 1f)) },
                        horizontalAlignment = if (style == IosTopBarStyle.TwoLineLeading) Alignment.Start else Alignment.CenterHorizontally,
                    ) {
                        Text(
                            title,
                            style = if (subtitle == null) IosTypography.headline
                            else IosTypography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                            color = LocalGlassColors.current.content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        subtitle?.let {
                            Text(
                                it,
                                style = IosTypography.caption,
                                color = LocalGlassColors.current.secondaryContent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Row(
                        Modifier.align(Alignment.CenterEnd).onSizeChanged { trailingWidth = it.width },
                        horizontalArrangement = Arrangement.spacedBy(IosTopToolbarActionGap),
                        content = actions,
                    )
                }
            }
        }
    }
}

/**
 * 胶囊分段选择（照抄 MeiloX 1.55.1 `IosScrollableTabRow`）：
 * 选中项用强调色填充、白字，其余用 iOS 分段控件底色。
 */
@Composable
fun <T> IosScrollableTabRow(
    items: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGlassColors.current
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.first.toString() }) { (value, label) ->
            val isSelected = value == selected
            Text(
                text = label,
                color = if (isSelected) Color.White else colors.content,
                style = IosTypography.subheadline,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier
                    .background(
                        if (isSelected) colors.prominentContainer.copy(alpha = 1f)
                        else colors.segmentedControlBackground,
                        Capsule(),
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                    ) { onSelected(value) }
                    .padding(horizontal = 15.dp, vertical = 7.dp),
            )
        }
    }
}
