package com.huanchengfly.tieba.post.ui.page.login

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.glass.IosMenuItem
import com.huanchengfly.tieba.post.ui.glass.IosPopMenuButton
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.page.settings.IosTopBarScaffold
import com.huanchengfly.tieba.post.ui.page.webview.MyWebChromeClient
import com.huanchengfly.tieba.post.ui.page.webview.MyWebViewClient
import com.huanchengfly.tieba.post.ui.page.webview.isInternalHost
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingState
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.WebView
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSaveableWebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberWebViewNavigator
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.parseCookie
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

const val LOGIN_URL =
    "https://wappass.baidu.com/passport?login&u=https%3A%2F%2Ftieba.baidu.com%2Findex%2Ftbwise%2Fmine"

@SuppressLint("SetJavaScriptEnabled")
@Destination
@Composable
fun LoginPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webViewState = rememberSaveableWebViewState()
    val webViewNavigator = rememberWebViewNavigator()
    var loaded by rememberSaveable {
        mutableStateOf(false)
    }
    var pageTitle by rememberSaveable {
        mutableStateOf("")
    }
    val displayPageTitle by remember {
        derivedStateOf {
            pageTitle.ifEmpty {
                context.getString(R.string.title_default)
            }
        }
    }
    val currentHost by remember {
        derivedStateOf {
            webViewState.lastLoadedUrl?.toUri()?.host.orEmpty().lowercase()
        }
    }
    val isExternalHost by remember {
        derivedStateOf {
            currentHost.isNotEmpty() && !isInternalHost(currentHost)
        }
    }

    DisposableEffect(Unit) {
        val job = coroutineScope.launch {
            snapshotFlow { webViewState.pageTitle }
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .cancellable()
                .collect {
                    pageTitle = it
                }
        }
        onDispose {
            job.cancel()
        }
    }

    LazyLoad(loaded = loaded) {
        webViewNavigator.loadUrl(LOGIN_URL)
        loaded = true
    }

    val isLoading by remember {
        derivedStateOf {
            webViewState.loadingState is LoadingState.Loading
        }
    }

    val progress by remember {
        derivedStateOf {
            webViewState.loadingState.let {
                if (it is LoadingState.Loading) {
                    it.progress
                } else {
                    0f
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    IosTopBarScaffold(
        title = displayPageTitle,
        subtitle = currentHost.takeIf { isExternalHost },
        // 与「我」页一致：标题放在顶栏居中，不使用下方大标题
        showsLargeTitle = false,
        // 网页内容自刷新，不能被玻璃顶栏逐帧采样（否则整页持续闪烁）
        sampleContentBackdrop = false,
        onNavigateBack = { navigator.navigateUp() },
        actions = {
            // 顶栏行尾按 MeiloX 的做法：玻璃圆钮做锚点，菜单是本项目的 IosPopMenuButton
            IosPopMenuButton(
                itemCount = 1,
                anchor = { openMenu ->
                    GlassIconButton(onClick = openMenu) {
                        SfIcon(
                            "ellipsis",
                            stringResource(id = R.string.btn_more),
                            size = 20.dp,
                            tint = LocalGlassColors.current.content,
                        )
                    }
                },
            ) { childBackdrop, close ->
                IosMenuItem(
                    title = stringResource(id = R.string.title_refresh),
                    onClick = {
                        webViewNavigator.reload()
                        close()
                    },
                    backdrop = childBackdrop,
                )
            }
        },
    ) { paddingValues ->
        Box {
            val snackbarHostState = LocalSnackbarHostState.current
            WebView(
                state = webViewState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                navigator = webViewNavigator,
                onCreated = {
                    it.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                },
                client = remember(navigator) {
                    LoginWebViewClient(
                        navigator,
                        coroutineScope,
                        snackbarHostState
                    )
                },
                chromeClient = remember { MyWebChromeClient(context, coroutineScope) }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
