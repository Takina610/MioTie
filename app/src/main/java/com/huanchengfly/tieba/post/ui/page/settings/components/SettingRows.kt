package com.huanchengfly.tieba.post.ui.page.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.TimePickerDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

/**
 * 卡片式设置行组件补充：
 * 单选 [ListSettingRow]、文本输入 [TextInputSettingRow]、时间选择 [TimePickerSettingRow]、
 * 提示块 [TipCard]，与 [SettingRow]/[SwitchSettingRow] 同一 iOS 列表几何。
 */

/**
 * 单选行（替代旧 PrefsScreen 的 ListPref）：点击弹单选对话框。
 * [key] 传 null 时不落盘（如切换账号），选中值经 [onValueChange] 回调处理。
 */
@Composable
internal fun ListSettingRow(
    title: String,
    icon: ImageVector? = null,
    systemName: String? = null,
    entries: Map<String, String>,   // value -> 显示文本
    defaultValue: String,
    key: String? = null,
    summary: String? = null,
    enabled: Boolean = true,
    dialogTitle: String = title,
    onValueChange: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    // 只读收集当前值（key 为 null 时不收集，如切换账号场景）
    val current: String? = if (key != null) {
        val prefs by remember {
            context.dataStore.data.map { it[stringPreferencesKey(key)] ?: defaultValue }
        }.collectAsState(initial = defaultValue)
        prefs
    } else {
        null
    }

    IosSettingRow(
        title = title,
        subtitle = summary ?: entries[current] ?: stringResource(id = R.string.summary_not_set),
        enabled = enabled,
        onClick = { dialogState.show() },
        leading = { SettingRowLeading(icon = icon, systemName = systemName) },
    )

    Dialog(
        dialogState = dialogState,
        title = { Text(text = dialogTitle) },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        ListSinglePicker(
            itemTitles = entries.values.toImmutableList(),
            itemValues = entries.keys.toImmutableList(),
            selectedPosition = entries.keys.indexOf(current),
            onItemSelected = { _, _, value, _ ->
                key?.let { k ->
                    context.dataStore.putString(k, value)
                }
                onValueChange?.invoke(value)
                dialogState.show = false
            },
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

/** 文本输入行（替代旧 EditTextPref）：点击弹输入对话框，确认后写回；[emptySummary] 为值为空时的占位摘要 */
@Composable
internal fun TextInputSettingRow(
    title: String,
    icon: ImageVector? = null,
    systemName: String? = null,
    key: String,
    defaultValue: String = "",
    summary: String? = null,
    @StringRes emptySummary: Int? = null,
    dialogTitle: String? = null,
    enabled: Boolean = true,
    onValueSaved: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    val current by context.dataStore.collectPreferenceAsState(
        key = stringPreferencesKey(key),
        defaultValue = defaultValue
    )

    IosSettingRow(
        title = title,
        subtitle = summary ?: current.ifBlank { emptySummary?.let { context.getString(it) } },
        enabled = enabled,
        onClick = { dialogState.show() },
        leading = { SettingRowLeading(icon = icon, systemName = systemName) },
    )

    PromptDialog(
        dialogState = dialogState,
        title = { Text(text = dialogTitle ?: title) },
        initialValue = current,
        onConfirm = { value ->
            context.dataStore.putString(key, value)
            onValueSaved?.invoke(value)
        },
    )
}

/** 时间选择行（替代旧 TimePickerPerf）：点击弹时间选择对话框 */
@Composable
internal fun TimePickerSettingRow(
    title: String,
    icon: ImageVector? = null,
    systemName: String? = null,
    key: String,
    defaultValue: String = "09:00",
    summary: (String) -> String,
    dialogTitle: String? = null,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    val current by context.dataStore.collectPreferenceAsState(
        key = stringPreferencesKey(key),
        defaultValue = defaultValue
    )

    IosSettingRow(
        title = title,
        subtitle = summary(current),
        enabled = enabled,
        onClick = { dialogState.show() },
        leading = { SettingRowLeading(icon = icon, systemName = systemName) },
    )

    TimePickerDialog(
        dialogState = dialogState,
        title = { Text(text = dialogTitle ?: title) },
        currentTime = current,
        onConfirm = { time ->
            context.dataStore.putString(key, time)
        },
    )
}

/** 卡片外提示块：iOS 分组列表里直接作为 footer 使用。 */
@Composable
internal fun TipCard(text: AnnotatedString, modifier: Modifier = Modifier) {
    GroupFooter(text = text, modifier = modifier)
}

/**
 * 分组下方说明文字（对齐 iOS `List { Section { ... } footer: { Text(...) } }`）：
 * 与行一样左缩进 16dp，不再用独立 chip 底色。
 */
@Composable
internal fun GroupFooter(text: AnnotatedString, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = LocalGlassColors.current.secondaryContent,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 6.dp),
    )
}
