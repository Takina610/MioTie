package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.glass.GlassToggle
import com.huanchengfly.tieba.post.ui.glass.IosListRow
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.CustomSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HabitSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.MemeEmoticonPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.OKSignSettingsPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置列表内容（对齐 MeiloX 1.55.1 `SettingScreen` 的分组结构）：
 * 账号 / 个性化 / 内容与浏览 / 功能 / 通用，每组一张 26dp 连续圆角卡片，
 * 行高 52dp、强调色 SF Symbol 图标、行尾 chevron。
 * 设置页（独立页面）与主界面「设置」标签页共用同一份内容。
 */
internal fun LazyListScope.settingsContent() {
    item { SettingsAccountGroup() }
    item {
        SettingsGroup(stringResource(id = R.string.title_settings_group_personal)) {
            SettingsPersonalRows()
        }
    }
    item {
        SettingsGroup(stringResource(id = R.string.title_settings_group_content)) {
            SettingsContentRows()
        }
    }
    item {
        SettingsGroup(stringResource(id = R.string.title_settings_group_feature)) {
            SettingsFeatureRows()
        }
    }
    item {
        SettingsGroup(stringResource(id = R.string.title_settings_group_general)) {
            SettingsGeneralRows()
        }
    }
}

@Composable
private fun SettingsAccountGroup() {
    val account = LocalAccount.current
    SettingsGroup(title = stringResource(id = R.string.title_settings_group_account)) {
        AccountRow(account = account)
    }
}

@Composable
private fun SettingsPersonalRows() {
    val navigator = LocalNavigator.current
    IosListRow(
        title = stringResource(id = R.string.title_settings_custom),
        systemName = "paintbrush",
        onClick = { navigator.navigate(CustomSettingsPageDestination) },
        showTopSeparator = false,
    )
    IosListRow(
        title = stringResource(id = R.string.title_settings_read_habit),
        systemName = "slider.vertical.3",
        onClick = { navigator.navigate(HabitSettingsPageDestination) },
    )
    IosListRow(
        title = stringResource(id = R.string.title_meme_emoticon),
        systemName = "photo.stack",
        onClick = { navigator.navigate(MemeEmoticonPageDestination) },
    )
}

@Composable
private fun SettingsContentRows() {
    val navigator = LocalNavigator.current
    // 同一个 key 只保留一份状态源：「Custom Tabs」的可用性直接由这一份状态派生，
    // 否则会多出一个只读订阅，开关切换后它仍是旧值，行状态就对不上了。
    var useWebView by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey("use_webview"),
        defaultValue = true,
    )
    IosListRow(
        title = stringResource(id = R.string.title_block_settings),
        systemName = "shield",
        onClick = { navigator.navigate(BlockSettingsPageDestination) },
        showTopSeparator = false,
    )
    SettingsToggleRow(
        title = stringResource(id = R.string.title_use_webview),
        systemName = "globe",
        checked = useWebView,
        onCheckedChange = { useWebView = it },
    )
    SettingsToggleRow(
        title = stringResource(id = R.string.title_use_custom_tabs),
        systemName = "safari",
        key = "use_custom_tabs",
        defaultValue = true,
        enabled = !useWebView,
    )
}

/**
 * 行内开关（外部持有状态）。用于「一个开关的状态会影响同一页其他行」的场景：
 * 状态只在该页保留一份，避免同一 key 被多处读取而出现不一致。
 */
@Composable
private fun SettingsToggleRow(
    title: String,
    systemName: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    IosListRow(
        title = title,
        systemName = systemName,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = {
            GlassToggle(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun SettingsFeatureRows() {
    val navigator = LocalNavigator.current
    IosListRow(
        title = stringResource(id = R.string.title_oksign),
        systemName = "checkmark.circle",
        onClick = { navigator.navigate(OKSignSettingsPageDestination) },
        showTopSeparator = false,
    )
}

@Composable
private fun SettingsGeneralRows() {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf("0.0B") }
    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) { ImageCacheUtil.getCacheSize(context) }
    }
    IosListRow(
        title = stringResource(id = R.string.title_clear_picture_cache),
        systemName = "trash",
        detail = cacheSize,
        showTopSeparator = false,
        onClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    ImageCacheUtil.clearImageAllCache(context)
                }
                cacheSize = "0.0B"
                snackbarHostState.showSnackbar(
                    context.getString(R.string.toast_clear_picture_cache_success)
                )
            }
        },
    )
    IosListRow(
        title = stringResource(id = R.string.title_open_by_default),
        systemName = "circle.grid.2x2.fill",
        onClick = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
            )
        },
    )
    SettingsToggleRow(
        title = stringResource(id = R.string.title_auto_check_update),
        systemName = "arrow.clockwise",
        key = "auto_check_update",
        defaultValue = true,
    )
    if (context.appPreferences.showExperimentalFeatures) {
        SettingsToggleRow(
            title = stringResource(id = R.string.title_check_ci_update),
            systemName = "hammer",
            key = "checkCIUpdate",
            defaultValue = false,
        )
    }
}

/**
 * 行内开关：整行可点（对齐 MeiloX 设置页里 `GlassToggle` 与行点击同源的交互），
 * 开关本身也支持拖动。
 */
@Composable
private fun SettingsToggleRow(
    title: String,
    systemName: String,
    key: String,
    defaultValue: Boolean,
    enabled: Boolean = true,
) {
    var checked by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(key),
        defaultValue = defaultValue,
    )
    IosListRow(
        title = title,
        systemName = systemName,
        onClick = { if (enabled) checked = !checked },
        trailing = {
            GlassToggle(
                checked = checked,
                onCheckedChange = { if (enabled) checked = it },
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun AccountRow(account: Account?) {
    val navigator = LocalNavigator.current
    val colors = LocalGlassColors.current
    val onAccountClick = {
        if (account != null) {
            navigator.navigate(AccountManagePageDestination)
        } else {
            navigator.navigate(LoginPageDestination)
        }
    }
    if (account != null) {
        IosListRow(
            title = account.nameShow ?: account.name,
            subtitle = stringResource(id = R.string.title_account_manage),
            showTopSeparator = false,
            leading = {
                Avatar(
                    data = StringUtil.getAvatarUrl(account.portrait),
                    size = Sizes.Small,
                    contentDescription = null
                )
            },
            onClick = onAccountClick,
        )
    } else {
        IosListRow(
            title = stringResource(id = R.string.button_login),
            subtitle = stringResource(id = R.string.summary_not_logged_in),
            systemName = "person.crop.circle",
            showTopSeparator = false,
            onClick = onAccountClick,
        )
    }
}
