package com.huanchengfly.tieba.post.ui.page.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.iosRowClickable
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.LocalGroupedListIconColor
import com.huanchengfly.tieba.post.ui.glass.LocalMergedGlassCards
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.glass.IosDisclosureIndicator

/**
 * iOS 分组列表的行本体（照抄 MeiloX 1.55.1 的 `IosListRow` 几何）：
 * 52dp（带副标题 62dp）行高、16dp 水平内边距、强调色图标、
 * 行间分隔线由行自行绘制（左缩进 16dp），行尾 chevron 只在可点击且无自定义 trailing 时出现。
 */
@Composable
internal fun IosSettingRow(
    title: String,
    subtitle: String? = null,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = LocalGlassColors.current
    val merged = LocalMergedGlassCards.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 副标题可换行完整展示，行高改为最小值
            .heightIn(min = if (subtitle == null) 52.dp else 62.dp)
            .drawBehind {
                if (merged) {
                    drawLine(
                        colors.separator,
                        Offset(16.dp.toPx(), 0f),
                        Offset(size.width - 16.dp.toPx(), 0f),
                        1.dp.toPx(),
                    )
                }
            }
            .alpha(if (enabled) 1f else 0.38f)
            .then(
                // 有 trailing 时不整行可点：开关自己处理手势，行再响应一次会 toggle 两回，
                // 表现为「值变了但界面没变」、要多点几次才回到正确状态。
                if (onClick != null && enabled && trailing == null) {
                    Modifier.iosRowClickable(onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 内容区（图标 + 文案）：带 trailing 时由它承接行点击，点击范围不盖住开关
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onClick != null && enabled && trailing != null) {
                        Modifier.iosRowClickable(onClick)
                    } else {
                        Modifier
                    }
                ),
        ) {
            leading?.let {
                it()
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
                        // 设置项的说明文字必须完整可读，不截断
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }
        detail?.let {
            Text(it, style = IosTypography.subheadline, color = colors.secondaryContent)
        }
        trailing?.let { trailingContent ->
            Spacer(Modifier.width(12.dp))
            CompositionLocalProvider(LocalGroupedListIconColor provides colors.content) {
                trailingContent()
            }
        }
        if (onClick != null && trailing == null) {
            Spacer(Modifier.width(8.dp))
            IosDisclosureIndicator(tint = colors.tertiaryContent)
        }
    }
}

@Composable
internal fun SettingRowIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = LocalGroupedListIconColor.current ?: LocalGlassColors.current.accent,
        modifier = Modifier.size(23.dp)
    )
}

/**
 * 行首图标：优先用 SF Symbol（对齐 MeiloX 设置页），否则回退到 Material 矢量图标。
 * 两者都为空时不占位。
 */
@Composable
internal fun SettingRowLeading(icon: ImageVector?, systemName: String?) {
    when {
        systemName != null -> SfIcon(systemName, null, size = 23.dp)
        icon != null -> SettingRowIcon(icon)
    }
}
