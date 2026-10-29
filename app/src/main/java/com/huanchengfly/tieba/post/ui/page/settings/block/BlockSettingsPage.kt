package com.huanchengfly.tieba.post.ui.page.settings.block

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.destinations.BlockListPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.IosSettingsScaffold
import com.huanchengfly.tieba.post.ui.page.settings.SettingsGroup
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SwitchSettingRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 屏蔽设置：分组列表（屏蔽列表入口 + 内容过滤开关） */
@Destination
@Composable
fun BlockSettingsPage(
    navigator: DestinationsNavigator
) {
    IosSettingsScaffold(
        title = stringResource(id = R.string.title_block_settings),
        onNavigateBack = { navigator.navigateUp() },
    ) {
        item {
            // 大标题已是「屏蔽设置」，分组用概括名而非与首行同名的「屏蔽词管理」
            SettingsGroup(stringResource(id = R.string.title_settings_group_content)) {
                SettingRow(
                    icon = Icons.Outlined.Block,
                    title = stringResource(id = R.string.title_block_list),
                    onClick = { navigator.navigate(BlockListPageDestination) }
                )
                SwitchSettingRow(
                    icon = Icons.Outlined.HideSource,
                    title = stringResource(id = R.string.settings_hide_blocked_content),
                    key = "hideBlockedContent",
                    defaultValue = false,
                )
                SwitchSettingRow(
                    icon = Icons.Outlined.VideocamOff,
                    title = stringResource(id = R.string.settings_block_video),
                    summary = stringResource(id = R.string.settings_block_video_summary),
                    key = "blockVideo",
                    defaultValue = false,
                )
                SwitchSettingRow(
                    icon = Icons.Outlined.Block,
                    title = stringResource(id = R.string.settings_show_followed_only),
                    summary = stringResource(id = R.string.settings_show_followed_only_summary),
                    key = "showFollowedOnly",
                    defaultValue = false,
                )
            }
        }
    }
}
