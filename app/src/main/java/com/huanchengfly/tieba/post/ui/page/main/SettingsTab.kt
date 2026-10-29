package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.settings.IosSettingsScaffold
import com.huanchengfly.tieba.post.ui.page.settings.settingsContent

/**
 * 主界面「设置」标签页：与设置页共享同一份 iOS 分组列表，顶栏不带返回键
 * （对齐 MeiloX 中设置作为主 tab 时的形态）。
 */
@Composable
fun SettingsTabPage() {
    IosSettingsScaffold(
        title = stringResource(id = R.string.title_settings),
        modifier = Modifier,
        onNavigateBack = null,
        bottomPadding = LocalMainBottomInsets.current,
    ) {
        settingsContent()
    }
}
