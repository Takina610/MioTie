package com.huanchengfly.tieba.post.ui.page.settings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.glass.GlassToggle
import com.huanchengfly.tieba.post.ui.glass.IosGroupedList
import com.huanchengfly.tieba.post.ui.glass.IosPopUpButton
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.LocalGroupedListIconColor
import com.huanchengfly.tieba.post.ui.glass.LocalMergedGlassCards
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.glass.SfSymbol

/**
 * 设置页统一组件（照抄 MeiloX 1.55.1 的 iOS Lists 规范）：
 * 分组标题 [SectionLabel] + 分组卡片 [AppearanceCard] + 行式条目 [SettingRow] / [SwitchSettingRow]。
 * 全部设置页（主页 / 外观 / 使用习惯 / 屏蔽 / 签到 / 账号 / 关于）统一使用。
 *
 * 行高 52dp（带副标题 62dp）、水平内边距 16dp、行间分隔线左缩进 16dp 且由行自行绘制、
 * 图标取强调色、行尾 chevron 只在可点击且无自定义 trailing 时出现。
 */

@Composable
internal fun SectionLabel(text: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalGlassColors.current.secondaryContent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = IosTypography.subheadline,
            color = LocalGlassColors.current.secondaryContent,
        )
    }
}

/** iOS 分组卡片：26dp 连续圆角，组内行间分隔线由行自己绘制，组外留 8dp 行距。 */
@Composable
internal fun AppearanceCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        IosGroupedList(content = content)
    }
}

/**
 * 兼容旧调用点：分隔线改由行自行绘制（保证首行贴卡片顶边时自动隐藏），
 * 因此这里不再产出任何内容。
 */
@Composable
internal fun CardDivider() {
    // iOS 分组列表的分隔线由 [SettingRow] 绘制
}

@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    IosSettingRow(
        title = title,
        subtitle = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = trailing,
        leading = { SettingRowIcon(icon) },
    )
}

/** 用 SF Symbol 名称指定图标（对齐 MeiloX 设置页的图标语言）。 */
@Composable
internal fun SettingRow(
    systemName: String,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    IosSettingRow(
        title = title,
        subtitle = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = trailing,
        leading = { SfIcon(systemName, null, size = 23.dp) },
    )
}

/** iOS 分组列表行本体，与 `IosListRow` 同几何，额外支持 [enabled]。 */
@Composable
internal fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    summaryOn: String? = null,
    summaryOff: String? = null,
    key: String,
    defaultValue: Boolean,
    enabled: Boolean = true,
    summaryProvider: ((Boolean) -> String)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    var checked by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(key),
        defaultValue = defaultValue
    )
    SwitchRowContent(
        checked = checked,
        title = title,
        summary = summaryProvider?.invoke(checked)
            ?: if (checked) summaryOn ?: summary else summaryOff ?: summary,
        enabled = enabled,
        onToggle = { value ->
            checked = value
            onCheckedChange?.invoke(value)
        },
        leading = { SettingRowIcon(icon) },
    )
}

/** 用 SF Symbol 名称指定图标的开关行。 */
@Composable
internal fun SwitchSettingRow(
    systemName: String,
    title: String,
    key: String,
    defaultValue: Boolean,
    summary: String? = null,
    summaryOn: String? = null,
    summaryOff: String? = null,
    enabled: Boolean = true,
    summaryProvider: ((Boolean) -> String)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    var checked by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(key),
        defaultValue = defaultValue
    )
    SwitchRowContent(
        checked = checked,
        title = title,
        summary = summaryProvider?.invoke(checked)
            ?: if (checked) summaryOn ?: summary else summaryOff ?: summary,
        enabled = enabled,
        onToggle = { value ->
            checked = value
            onCheckedChange?.invoke(value)
        },
        leading = { SfIcon(systemName, null, size = 23.dp) },
    )
}

@Composable
private fun SwitchRowContent(
    checked: Boolean,
    title: String,
    summary: String?,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    leading: @Composable () -> Unit,
) {
    val currentOnToggle = remember(enabled, onToggle) { onToggle }
    IosSettingRow(
        title = title,
        subtitle = summary,
        enabled = enabled,
        onClick = { if (enabled) currentOnToggle(!checked) },
        leading = leading,
        trailing = {
            GlassToggle(
                checked = checked,
                onCheckedChange = { if (enabled) currentOnToggle(it) },
                enabled = enabled,
            )
        },
    )
}

/**
 * 行尾下拉选择（照搬 MeiloX「设置 - 通用 - 外观」`GeneralChoice`）：
 * 图标 + 标题占满一行，右侧是强调色文字 + `chevron.up.chevron.down` 的弹出菜单。
 */
@Composable
internal fun ChoiceSettingRow(
    systemName: String,
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val selectedOption = options.firstOrNull { it.first == selected } ?: options.first()
    IosSettingRow(
        title = title,
        subtitle = summary,
        enabled = enabled,
        leading = { SfIcon(systemName, null, size = 23.dp) },
        trailing = {
            IosPopUpButton(
                selected = selectedOption,
                items = options,
                onSelected = { onSelected(it.first) },
                label = { it.second },
                enabled = enabled,
            )
        },
    )
}

/** 行首图标：统一强调色 23dp，供 [IosSettingRow] 的 leading 槽使用。 */

/**
 * 用 SF Symbol 名称指定图标的开关行（外部持有状态）。
 * 用于「该开关会影响同页其他行」的场景：调用方只保留一份状态，避免同一 key 多处读取。
 */
@Composable
internal fun SwitchSettingRow(
    systemName: String,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SwitchRowContent(
        checked = checked,
        title = title,
        summary = summary,
        enabled = enabled,
        onToggle = onCheckedChange,
        leading = { SfIcon(systemName, null, size = 23.dp) },
    )
}
