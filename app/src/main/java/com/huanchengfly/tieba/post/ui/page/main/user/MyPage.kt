package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 「我的」独立页面：由底部个人信息条进入，复用标签页时期的完整内容
 * （账号信息、收藏、历史、服务与设置入口）。
 */
@Destination
@Composable
fun MyPage(
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        UserPage(
            topBar = {
                Toolbar(
                    title = stringResource(id = R.string.title_user),
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                    }
                )
            }
        )
    }
}