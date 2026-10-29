package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun SettingsPage(
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        IosSettingsScaffold(
            title = stringResource(id = R.string.title_settings),
            modifier = Modifier,
            onNavigateBack = { navigator.navigateUp() },
        ) {
            settingsContent()
        }
    }
}
