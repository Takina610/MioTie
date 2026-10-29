package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.glass.IosListRow
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.FollowListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HistoryPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadStorePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.WebViewPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.IosGlassPageScaffold
import com.huanchengfly.tieba.post.ui.page.settings.SettingsGroup
import com.huanchengfly.tieba.post.utils.CuidUtils

/**
 * 「我的」页面（对齐 MeiloX 的 iOS Lists 与设置页同一套规范）：
 * 固定顶栏 + 可折叠大标题，账号头部 + 分组列表。
 * 主界面「我的」标签页与底栏个人信息条进入的独立页共用同一份内容。
 *
 * @param title 顶栏与大标题文案
 * @param onNavigateBack 非空时顶栏显示返回键；标签页形态传 null
 */
@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun UserPage(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: UserViewModel = pageViewModel<UserUiIntent, UserViewModel>(
        listOf(
            UserUiIntent.Refresh
        )
    )
) {
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = UserUiState::isLoading,
        initial = false
    )
    val account by viewModel.uiState.collectPartialAsState(
        prop1 = UserUiState::account,
        initial = null
    )
    val context = androidx.compose.ui.platform.LocalContext.current

    IosGlassPageScaffold(modifier = modifier) { background, snackbar ->
        com.huanchengfly.tieba.post.ui.glass.IosPinnedListPage(
            title = title,
            bottomPadding = com.huanchengfly.tieba.post.ui.page.main.LocalMainBottomInsets.current,
            showsLargeTitle = false,
            onNavigateBack = onNavigateBack,
            backgroundColor = background,
        ) {
            item {
                if (account != null) {
                    UserAccountHeader(
                        account = account!!,
                        onClick = {
                            navigator.navigate(UserProfilePageDestination(account!!.uid.toLong()))
                        },
                    )
                    UserStatsCard(
                        account = account!!,
                        onFollowClick = { navigator.navigate(FollowListPageDestination()) },
                    )
                } else if (isLoading) {
                    UserAccountPlaceholder()
                } else {
                    LoginHeader(onClick = { navigator.navigate(LoginPageDestination) })
                }
            }
            item {
                SettingsGroup(stringResource(id = R.string.title_user_content)) {
                    if (account != null) {
                        IosListRow(
                            title = stringResource(id = R.string.title_my_collect),
                            systemName = "book.closed",
                            showTopSeparator = false,
                            onClick = { navigator.navigate(ThreadStorePageDestination) },
                        )
                    }
                    IosListRow(
                        title = stringResource(id = R.string.title_history),
                        systemName = "clock",
                        showTopSeparator = account != null,
                        onClick = { navigator.navigate(HistoryPageDestination) },
                    )
                }
            }
            item {
                SettingsGroup(stringResource(id = R.string.title_user_more)) {
                    if (account != null) {
                        IosListRow(
                            title = stringResource(id = R.string.my_info_service_center),
                            systemName = "bubble.left.and.bubble.right",
                            showTopSeparator = false,
                            onClick = {
                                navigator.navigate(
                                    WebViewPageDestination(
                                        initialUrl = "https://tieba.baidu.com/mo/q/hybrid-main-service/uegServiceCenter?cuid=${'$'}{CuidUtils.getNewCuid()}&cuid_galaxy2=${'$'}{CuidUtils.getNewCuid()}&cuid_gid=&timestamp=${'$'}{System.currentTimeMillis()}&_client_version=${ClientVersion.TIEBA_V12.version}&nohead=1"
                                    )
                                )
                            },
                        )
                    }
                    IosListRow(
                        title = stringResource(id = R.string.my_info_about),
                        systemName = "info.circle",
                        showTopSeparator = account != null,
                        onClick = { navigator.navigate(AboutPageDestination) },
                    )
                }
            }
        }
        androidx.compose.material.SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}
