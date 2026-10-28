package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.settings.SettingsContent
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar

/**
 * 主界面「设置」标签页：与设置页共享同一份设置列表，省去一进一出。
 */
@Composable
fun SettingsTabPage() {
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(title = stringResource(id = R.string.title_settings))
        },
    ) { paddingValues ->
        SettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}