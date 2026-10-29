package com.huanchengfly.tieba.post.ui.page.settings.account

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SupervisedUserCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.IosSettingsScaffold
import com.huanchengfly.tieba.post.ui.page.settings.SettingsGroup
import com.huanchengfly.tieba.post.ui.page.settings.components.GroupFooter
import com.huanchengfly.tieba.post.ui.page.settings.components.ListSettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.SettingRow
import com.huanchengfly.tieba.post.ui.page.settings.components.TextInputSettingRow
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.AllAccounts
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.launchUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 账号管理：分组列表（账号操作 / 个性化） */
@Destination
@Composable
fun AccountManagePage(
    navigator: DestinationsNavigator,
) {
    val account = LocalAccount.current
    val context = LocalContext.current
    IosSettingsScaffold(
        title = stringResource(id = R.string.title_account_manage),
        onNavigateBack = { navigator.navigateUp() },
    ) {
        item {
            SettingsGroup(stringResource(id = R.string.title_account_group_manage)) {
                if (account != null) {
                    ListSettingRow(
                        systemName = "person.crop.circle",
                        title = stringResource(id = R.string.title_switch_account),
                        entries = AllAccounts.current.associate {
                            it.id.toString() to (it.nameShow ?: it.name)
                        },
                        defaultValue = account.id.toString(),
                        key = null,
                        summary = stringResource(
                            id = R.string.summary_now_account,
                            account.nameShow ?: account.name
                        ),
                        onValueChange = { AccountUtil.switchAccount(context, it.toInt()) }
                    )
                } else {
                    SettingRow(
                        systemName = "person.crop.circle",
                        title = stringResource(id = R.string.title_switch_account),
                        enabled = false
                    )
                }
                SettingRow(
                    systemName = "person.crop.circle.badge.plus",
                    title = stringResource(id = R.string.title_new_account),
                    onClick = { navigator.navigate(LoginPageDestination) }
                )
                SettingRow(
                    systemName = "rectangle.portrait.and.arrow.forward",
                    title = stringResource(id = R.string.title_exit_account),
                    onClick = { AccountUtil.exit(context) }
                )
                SettingRow(
                    systemName = "person.2",
                    title = stringResource(id = R.string.title_modify_username),
                    enabled = account != null,
                    onClick = {
                        launchUrl(
                            context,
                            navigator,
                            "https://wappass.baidu.com/static/manage-chunk/change-username.html#/showUsername"
                        )
                    }
                )
                SettingRow(
                    systemName = "square.on.square",
                    title = stringResource(id = R.string.title_copy_bduss),
                    summary = stringResource(id = R.string.summary_copy_bduss),
                    enabled = account != null,
                    onClick = { TiebaUtil.copyText(context, account?.bduss, isSensitive = true) }
                )
            }
        }
        item {
            SettingsGroup(stringResource(id = R.string.title_account_group_personal)) {
                TextInputSettingRow(
                    systemName = "square.and.pencil",
                    title = stringResource(id = R.string.title_my_tail),
                    key = "little_tail",
                    emptySummary = R.string.tip_no_little_tail,
                    dialogTitle = stringResource(id = R.string.title_dialog_modify_little_tail),
                )
            }
        }
        item {
            GroupFooter(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(stringResource(id = R.string.tip_start))
                    }
                    append(stringResource(id = R.string.tip_account_error))
                }
            )
        }
    }
}
