package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * iOS 分组列表（照抄 MeiloX 1.55.1 `IosGroupedList`）：
 * 26dp 连续圆角卡片包住若干 [IosListRow]，行间分隔线由行自己画（左缩进 16dp）。
 */
@Composable
fun IosGroupedList(
    modifier: Modifier = Modifier,
    // 弹层已经提供玻璃底板，内部列表不再包一层卡片，否则会出现第二层圆角
    framed: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val background = colors.elevatedBackground.copy(
        alpha = LocalGroupedListBackgroundAlpha.current.coerceIn(0f, 1f),
    )
    val shape = ContinuousRoundedRectangle(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (framed) {
                    Modifier
                        .clip(shape)
                        .background(background, shape)
                        .drawWithContent {
                            drawContent()
                            // 只抹掉首行贴着卡片顶边的那段分隔线，避免它从上方圆角漏出
                            val inset = 16.dp.toPx()
                            drawRect(
                                color = background,
                                topLeft = Offset(inset, 0f),
                                size = Size(
                                    (size.width - inset * 2f).coerceAtLeast(0f),
                                    1.dp.toPx()
                                ),
                            )
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        CompositionLocalProvider(
            LocalMergedGlassCards provides true,
            LocalGroupedListIconColor provides colors.accent,
        ) {
            content()
        }
    }
}

/**
 * 单行（照抄 MeiloX 1.55.1 `IosListRow`）：
 * 52dp（带副标题 62dp）行高、16dp 水平内边距、SF Symbol 强调色图标、
 * 行尾 chevron 只在可点击且没有自定义 trailing 时出现。
 */
@Composable
fun IosListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    systemName: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showTopSeparator: Boolean = true,
    showDisclosure: Boolean = onClick != null && trailing == null,
) {
    val colors = LocalGlassColors.current
    Row(
        modifier
            .fillMaxWidth()
            // 副标题可换行完整展示，行高改为最小值
            .heightIn(min = if (subtitle == null) 52.dp else 62.dp)
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
            .then(
                if (onClick != null) {
                    Modifier.iosRowClickable(onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            it()
            Spacer(Modifier.width(12.dp))
        }
        systemName?.let {
            SfIcon(it, null, size = 23.dp, tint = colors.accent)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = IosTypography.body,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    it,
                    style = IosTypography.subheadline,
                    color = colors.secondaryContent,
                    // 设置项说明必须完整可读，不截断
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        detail?.let {
            Text(it, style = IosTypography.subheadline, color = colors.secondaryContent)
        }
        trailing?.let { trailingContent ->
            CompositionLocalProvider(LocalGroupedListIconColor provides colors.content) {
                trailingContent()
            }
        }
        if (showDisclosure) {
            Spacer(Modifier.width(8.dp))
            IosDisclosureIndicator()
        }
    }
}

/**
 * iOS 列表的 disclosure 指示器（照搬 MeiloX 1.55.1 `IosListRow` 的行尾箭头）：
 * SF Symbol `chevron.forward` 17dp、tertiaryContent 灰、8×22dp 槽位。
 * 此前用 Canvas 自绘细线「>」，字形与 MeiloX 的 SF Symbol 不一致。
 */
@Composable
fun IosDisclosureIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 17.dp,
    tint: Color = LocalGlassColors.current.tertiaryContent,
) {
    Box(
        modifier.size(width = 8.dp, height = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        SfIcon("chevron.forward", null, size = size, tint = tint)
    }
}

/**
 * iOS 列表行的点击反馈：没有 Material 涟漪。
 *
 * iOS 的表格行按下时不会给整行蒙一层灰底；默认的 `Modifier.clickable` 会走
 * Material 的 ripple indication，在本项目（M2 主题）上尤其明显，观感与 iOS 不符。
 * 需要按压反馈时由行内控件（如玻璃开关）自己提供，行本身保持静止。
 */
internal fun Modifier.iosRowClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = null,
        indication = null,
        role = Role.Button,
        onClick = onClick,
    )
