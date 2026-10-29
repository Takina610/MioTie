package com.huanchengfly.tieba.post.ui.page.settings.oksign

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.page.settings.IosSettingsScaffold
import com.huanchengfly.tieba.post.ui.page.settings.SettingsGroup
import com.huanchengfly.tieba.post.ui.page.settings.components.GroupFooter
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SwitchSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.TimePickerSettingRow
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.powerManager
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

/** 一键签到设置：分组列表（签到行为 / 后台运行） */
@Destination
@Composable
fun OKSignSettingsPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 自动签到开启后时间选择行才可用（替代旧 PrefsScreen 的 depend）。
    // 这个状态同时供开关与时间行使用，必须是唯一来源，不能再用只读订阅叠加一份。
    var autoSignEnabled by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("auto_sign"),
        defaultValue = false
    )
    IosSettingsScaffold(
        title = stringResource(id = R.string.title_oksign),
        onNavigateBack = { navigator.navigateUp() },
    ) {
        item {
            SettingsGroup(stringResource(id = R.string.title_oksign_group_sign)) {
                SwitchSettingRow(
                    systemName = "photo.stack",
                    title = stringResource(id = R.string.title_oksign_slow_mode),
                    summaryOn = stringResource(id = R.string.summary_oksign_slow_mode_on),
                    summaryOff = stringResource(id = R.string.summary_oksign_slow_mode),
                    key = "oksign_slow_mode",
                    defaultValue = true,
                )
                SwitchSettingRow(
                    systemName = "gauge.with.dots.needle.33percent",
                    title = stringResource(id = R.string.title_oksign_use_official_oksign),
                    summary = stringResource(id = R.string.summary_oksign_use_official_oksign),
                    key = "oksign_use_official_oksign",
                    defaultValue = true,
                )
                SwitchSettingRow(
                    systemName = "xmark.circle",
                    title = stringResource(id = R.string.title_oksign_fail_auto_stop),
                    summary = stringResource(id = R.string.summary_oksign_fail_auto_stop),
                    key = "oksign_fail_auto_stop",
                    defaultValue = true,
                )
                SwitchSettingRow(
                    systemName = "seal",
                    title = stringResource(id = R.string.title_auto_sign),
                    summary = if (autoSignEnabled) stringResource(id = R.string.summary_auto_sign_on)
                    else stringResource(id = R.string.summary_auto_sign),
                    checked = autoSignEnabled,
                    onCheckedChange = { autoSignEnabled = it },
                )
                TimePickerSettingRow(
                    systemName = "clock",
                    title = stringResource(id = R.string.title_auto_sign_time),
                    key = "auto_sign_time",
                    defaultValue = "09:00",
                    summary = { context.getString(R.string.summary_auto_sign_time, it) },
                    dialogTitle = stringResource(id = R.string.title_auto_sign_time),
                    enabled = autoSignEnabled
                )
            }
        }
        item {
            // LocalSnackbarHostState 由 IosSettingsScaffold 提供，只能在壳内读取
            val snackbarHostState = LocalSnackbarHostState.current
            SettingsGroup(stringResource(id = R.string.title_oksign_group_background)) {
                SettingRow(
                    systemName = "bolt.fill",
                    title = stringResource(id = R.string.title_ignore_battery_optimization),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !context.isIgnoringBatteryOptimizations(),
                    summary =
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) stringResource(id = R.string.summary_battery_optimization_old_android_version)
                    else if (context.isIgnoringBatteryOptimizations()) stringResource(id = R.string.summary_battery_optimization_ignored)
                    else stringResource(id = R.string.summary_ignore_battery_optimization),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!context.powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                context.requestIgnoreBatteryOptimizations()
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.toast_ignore_battery_optimization_already))
                                }
                            }
                        }
                    }
                )
            }
        }
        item {
            GroupFooter(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.tip_start))
                    }
                    append(stringResource(id = R.string.tip_auto_sign))
                }
            )
        }
    }
}
