package com.huanchengfly.tieba.post.ui.page.settings.habit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.intPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.page.settings.IosSettingsScaffold
import com.huanchengfly.tieba.post.ui.page.settings.components.AppearanceCard
import com.huanchengfly.tieba.post.ui.page.settings.components.CardDivider
import com.huanchengfly.tieba.post.ui.page.settings.components.ChoiceSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.ListSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SectionLabel
import com.huanchengfly.tieba.post.ui.page.settings.components.SwitchSettingRow
import com.huanchengfly.tieba.post.utils.isPhotoPickerAvailable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 使用习惯设置：卡片式分组（图片与媒体 / 浏览与导航 / 收藏 / 隐私与互动） */
@Destination
@Composable
fun HabitSettingsPage(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    IosSettingsScaffold(
        title = stringResource(id = R.string.title_settings_read_habit),
        onNavigateBack = { navigator.navigateUp() },
    ) {
        // ── 图片与媒体
        item {
            SectionLabel(stringResource(id = R.string.title_habit_group_image))
        }
        item {
            AppearanceCard {
                ListSettingRow(
                    systemName = "photo",
                    title = stringResource(id = R.string.title_settings_image_load_type),
                    entries = mapOf(
                        "0" to stringResource(id = R.string.title_image_load_type_smart_origin),
                        "1" to stringResource(id = R.string.title_image_load_type_smart_load),
                        "2" to stringResource(id = R.string.title_image_load_type_all_origin),
                        "3" to stringResource(id = R.string.title_image_load_type_all_no)
                    ),
                    defaultValue = "0",
                    key = "image_load_type",
                )
                CardDivider()
                ListSettingRow(
                    systemName = "square.on.square",
                    title = stringResource(id = R.string.title_settings_image_watermark),
                    entries = mapOf(
                        "0" to stringResource(id = R.string.title_image_watermark_none),
                        "1" to stringResource(id = R.string.title_image_watermark_user_name),
                        "2" to stringResource(id = R.string.title_image_watermark_forum_name)
                    ),
                    defaultValue = "2",
                    key = "pic_watermark_type",
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "moon.fill",
                    title = stringResource(id = R.string.settings_image_darken_when_night_mode),
                    key = "imageDarkenWhenNightMode",
                    defaultValue = true,
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "chevron.up.chevron.down",
                    title = stringResource(id = R.string.title_hide_media),
                    key = "hideMedia",
                    defaultValue = false,
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "magnifyingglass",
                    title = stringResource(id = R.string.title_do_not_use_photo_picker),
                    key = "doNotUsePhotoPicker",
                    defaultValue = false,
                    enabled = isPhotoPickerAvailable(),
                    summaryProvider = { checked ->
                        if (!isPhotoPickerAvailable()) {
                            context.getString(R.string.summary_photo_picker_not_supported)
                        } else if (checked) {
                            context.getString(R.string.summary_do_not_use_photo_picker)
                        } else {
                            context.getString(R.string.summary_use_photo_picker)
                        }
                    },
                )
            }
        }

        // ── 浏览与导航
        item {
            SectionLabel(stringResource(id = R.string.title_habit_group_browse))
        }
        item {
            AppearanceCard {
                StartPageSettingRow()
                CardDivider()
                ListSettingRow(
                    systemName = "calendar",
                    title = stringResource(id = R.string.title_settings_default_sort_type),
                    entries = mapOf(
                        "0" to stringResource(id = R.string.title_sort_by_reply),
                        "1" to stringResource(id = R.string.title_sort_by_send),
                    ),
                    defaultValue = "0",
                    key = "default_sort_type",
                )
                CardDivider()
                ListSettingRow(
                    systemName = "arrowshape.turn.up.right",
                    title = stringResource(id = R.string.settings_forum_fab_function),
                    entries = mapOf(
                        "post" to stringResource(id = R.string.btn_post),
                        "refresh" to stringResource(id = R.string.btn_refresh),
                        "back_to_top" to stringResource(id = R.string.btn_back_to_top),
                        "hide" to stringResource(id = R.string.btn_hide)
                    ),
                    defaultValue = "post",
                    key = "forumFabFunction",
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "arrow.uturn.backward.circle",
                    title = stringResource(id = R.string.title_predictive_back),
                    summaryProvider = { context.getString(R.string.summary_predictive_back) },
                    key = "predictive_back",
                    defaultValue = true,
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "clock",
                    title = stringResource(id = R.string.settings_home_page_show_history_forum),
                    key = "homePageShowHistoryForum",
                    defaultValue = true,
                )
            }
        }

        // ── 收藏
        item {
            SectionLabel(stringResource(id = R.string.title_habit_group_collect))
        }
        item {
            AppearanceCard {
                SwitchSettingRow(
                    systemName = "star",
                    title = stringResource(id = R.string.settings_collect_thread_see_lz),
                    summaryOn = stringResource(id = R.string.tip_collect_thread_see_lz_on),
                    summaryOff = stringResource(id = R.string.tip_collect_thread_see_lz),
                    key = "collect_thread_see_lz",
                    defaultValue = true,
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "arrow.up.arrow.down",
                    title = stringResource(id = R.string.settings_collect_thread_desc_sort),
                    summaryOn = stringResource(id = R.string.tip_collect_thread_desc_sort_on),
                    summaryOff = stringResource(id = R.string.tip_collect_thread_desc_sort),
                    key = "collect_thread_desc_sort",
                    defaultValue = false,
                )
            }
        }

        // ── 隐私与互动
        item {
            SectionLabel(stringResource(id = R.string.title_habit_group_privacy))
        }
        item {
            AppearanceCard {
                SwitchSettingRow(
                    systemName = "eye",
                    title = stringResource(id = R.string.settings_incognito_mode),
                    key = "incognitoMode",
                    defaultValue = false,
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "bubble.left",
                    title = stringResource(id = R.string.title_hide_reply),
                    key = "hideReply",
                    defaultValue = false,
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "checkmark.circle",
                    title = stringResource(id = R.string.title_show_both_username_and_nickname),
                    key = "show_both_username_and_nickname",
                    defaultValue = false,
                )
                CardDivider()
                SwitchSettingRow(
                    systemName = "exclamationmark.triangle",
                    title = stringResource(id = R.string.title_post_or_reply_warning),
                    key = "postOrReplyWarning",
                    defaultValue = true,
                )
            }
        }
    }
}

/**
 * 启动首选项：与「外观设置 - 主题模式」同一套行尾下拉组件（MeiloX `GeneralChoice`）。
 * 写入 defaultStart（0=首页，1=动态页）。
 */
@Composable
private fun StartPageSettingRow() {
    var startPage by rememberPreferenceAsMutableState(
        key = intPreferencesKey("defaultStart"),
        defaultValue = 0
    )
    ChoiceSettingRow(
        systemName = "house.fill",
        title = stringResource(id = R.string.settings_default_start),
        options = listOf(
            "0" to stringResource(id = R.string.title_main),
            "1" to stringResource(id = R.string.title_explore),
        ),
        selected = startPage.toString(),
        onSelected = { startPage = it.toIntOrNull() ?: 0 },
    )
}
