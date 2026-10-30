package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text as MaterialText
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.glass.GlassButton
import com.huanchengfly.tieba.post.ui.glass.GlassEmphasis
import com.huanchengfly.tieba.post.ui.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.glass.IosPinnedPage
import com.huanchengfly.tieba.post.ui.glass.IosToastHost
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.glass.blurBy
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.main.LocalMainBottomInsets
import com.huanchengfly.tieba.post.ui.page.main.LocalMainBottomToastInset
import com.huanchengfly.tieba.post.ui.page.settings.IosGlassPageScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefreshLogoIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.kyant.shapes.Capsule
import kotlinx.collections.immutable.persistentListOf

/** 单列是通栏大卡，双列是并排小卡；两种都用固定列数，卡片宽度才与 iOS 网格一致。 */
private fun getGridCells(listSingle: Boolean): GridCells =
    if (listSingle) GridCells.Fixed(1) else GridCells.Fixed(2)

private val CollapseDistance = 56.dp

private fun Modifier.fadeOutBy(progress: () -> Float): Modifier = graphicsLayer {
    val value = progress().coerceIn(0f, 1f)
    alpha = 1f - value
    val scale = 1f - 0.04f * value
    scaleX = scale
    scaleY = scale
    translationX = 0f
}

/**
 * 首页（对齐 MeiloX 1.55.1 首页的 iOS 形态）：
 * 固定顶栏 + 渐进模糊 + 可折叠大标题，内容是吧卡片网格；
 * 签到与单双列切换是顶栏的玻璃圆钮，页面本身不再有搜索框（搜索入口在底栏）。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = pageViewModel(),
    canOpenExplore: Boolean = false,
    onOpenExplore: () -> Unit = {},
) {
    val account = LocalAccount.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::isLoading,
        initial = false
    )
    val forums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::forums,
        initial = persistentListOf()
    )
    val topForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::topForums,
        initial = persistentListOf()
    )
    val historyForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::historyForums,
        initial = persistentListOf()
    )
    val expandHistoryForum by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::expandHistoryForum,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::error,
        initial = null
    )
    val hasLoaded by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::hasLoaded,
        initial = false
    )
    val isLoggedIn = remember(account) { account != null }
    val isEmpty by remember { derivedStateOf { forums.isEmpty() } }
    val showEmptyState by remember {
        derivedStateOf { isEmpty && (!isLoggedIn || hasLoaded) }
    }
    val hasTopForum by remember { derivedStateOf { topForums.isNotEmpty() } }
    val showHistoryForum by remember {
        derivedStateOf {
            context.appPreferences.homePageShowHistoryForum && historyForums.isNotEmpty()
        }
    }
    var listSingle by remember { mutableStateOf(context.appPreferences.listSingle) }
    val isError by remember { derivedStateOf { error != null } }

    LaunchedEffect(isLoading, hasLoaded) {
        // 登录用户等首屏数据真正就绪再让启动屏退场(冷启动 isLoading 初始为 false,勿按首帧误判);
        // 未登录无加载流程,直接就绪
        if (!isLoading && (hasLoaded || !isLoggedIn)) {
            com.huanchengfly.tieba.post.ui.widgets.compose.SplashState.markReady()
        }
    }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "home" }
    ) {
        if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh)
    }

    var unfollowForum by remember { mutableStateOf<HomeUiState.Forum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    ConfirmDialog(
        dialogState = confirmUnfollowDialog,
        onConfirm = {
            unfollowForum?.let {
                viewModel.send(HomeUiIntent.Unfollow(it.forumId, it.forumName))
            }
        },
    ) {
        MaterialText(
            text = stringResource(
                id = R.string.title_dialog_unfollow_forum,
                unfollowForum?.forumName.orEmpty()
            )
        )
    }

    LaunchedEffect(Unit) {
        if (isLoggedIn && !viewModel.initialized) {
            viewModel.send(HomeUiIntent.RefreshHistory)
            viewModel.send(HomeUiIntent.Refresh)
        }
    }

    val gridState = rememberLazyGridState()
    val layoutDirection = LocalLayoutDirection.current
    // 折叠进度用派生状态持有，只在 graphicsLayer / layout 里读取，滚动时不触发整页重组
    val collapseDistancePx = with(LocalDensity.current) { CollapseDistance.toPx() }
    val collapseProgress = remember(gridState, collapseDistancePx) {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (gridState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
            }
        }
    }

    IosGlassPageScaffold(modifier = Modifier.fillMaxSize()) { background, snackbar ->
        IosPinnedPage(
            title = stringResource(id = R.string.title_main),
            bottomPadding = LocalMainBottomInsets.current,
            modifier = Modifier.fillMaxSize(),
            collapseProgress = { collapseProgress.value },
            backgroundColor = background,
            actions = {
                GlassIconButton(onClick = { TiebaUtil.startSign(context) }) {
                    SfIcon(
                        systemName = "checkmark.circle",
                        contentDescription = stringResource(id = R.string.title_oksign),
                        size = 21.dp,
                        tint = LocalGlassColors.current.content,
                    )
                }
                GlassIconButton(onClick = {
                    context.appPreferences.listSingle = !listSingle
                    listSingle = !listSingle
                }) {
                    SfIcon(
                        systemName = if (listSingle) "circle.grid.2x2.fill" else "rectangle.grid.1x2",
                        contentDescription = stringResource(
                            id = if (listSingle) R.string.title_switch_list_double
                            else R.string.title_switch_list_single
                        ),
                        size = 20.dp,
                        tint = LocalGlassColors.current.content,
                    )
                }
            },
        ) { contentPadding ->
            val pullRefreshState = rememberPullRefreshState(
                refreshing = isLoading,
                onRefresh = { if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh) }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                StateScreen(
                    isEmpty = showEmptyState,
                    isError = isError,
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxSize(),
                    onReload = {
                        if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh)
                    },
                    emptyScreen = {
                        // 空态没有可滚动的列表，大标题与水平内边距由这里补齐，
                        // 否则标题会压到状态栏、起点也比设置页更靠左，中间内容也不居中。
                        Column(Modifier.fillMaxSize()) {
                            HomeLargeTitle(
                                modifier = Modifier
                                    .padding(
                                        start = contentPadding.calculateStartPadding(layoutDirection),
                                        top = contentPadding.calculateTopPadding(),
                                    )
                                    .offset(y = (-10).dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(
                                        start = contentPadding.calculateStartPadding(layoutDirection),
                                        end = contentPadding.calculateEndPadding(layoutDirection),
                                        bottom = contentPadding.calculateBottomPadding(),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                EmptyScreen(
                                    loggedIn = isLoggedIn,
                                    canOpenExplore = canOpenExplore,
                                    onOpenExplore = onOpenExplore
                                )
                            }
                        }
                    },
                    errorScreen = {
                        error?.let {
                            Column(Modifier.fillMaxSize()) {
                                HomeLargeTitle(
                                    modifier = Modifier
                                        .padding(
                                            start = contentPadding.calculateStartPadding(layoutDirection),
                                            top = contentPadding.calculateTopPadding(),
                                        )
                                        .offset(y = (-10).dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(
                                            start = contentPadding.calculateStartPadding(layoutDirection),
                                            end = contentPadding.calculateEndPadding(layoutDirection),
                                            bottom = contentPadding.calculateBottomPadding(),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ErrorScreen(error = it)
                                }
                            }
                        }
                    },
                ) {
                    MyLazyVerticalGrid(
                        columns = getGridCells(listSingle),
                        state = gridState,
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(key = "LargeTitle", span = { GridItemSpan(maxLineSpan) }) {
                            HomeLargeTitle(
                                modifier = Modifier
                                    .fadeOutBy { collapseProgress.value }
                                    .blurBy { 6.dp * collapseProgress.value.coerceIn(0f, 1f) }
                                    .offset(y = (-10).dp),
                            )
                        }
                        if (showHistoryForum) {
                            item(key = "HistoryForums", span = { GridItemSpan(maxLineSpan) }) {
                                HistoryForumSection(
                                    forums = historyForums,
                                    expanded = expandHistoryForum,
                                    onToggle = {
                                        viewModel.send(
                                            HomeUiIntent.ToggleHistory(expandHistoryForum)
                                        )
                                    },
                                    onForumClick = {
                                        navigator.navigate(ForumPageDestination(it.data))
                                    },
                                )
                            }
                        }
                        if (hasTopForum) {
                            item(key = "TopForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                                IosSectionTitle(
                                    text = stringResource(id = R.string.title_top_forum)
                                )
                            }
                            items(
                                items = topForums,
                                key = { "Top${it.forumId}" }
                            ) { item ->
                                ForumItem(
                                    item,
                                    listSingle,
                                    onClick = {
                                        navigator.navigate(ForumPageDestination(it.forumName))
                                    },
                                    onUnfollow = {
                                        unfollowForum = it
                                        confirmUnfollowDialog.show()
                                    },
                                    onAddTopForum = {
                                        viewModel.send(HomeUiIntent.TopForums.Add(it))
                                    },
                                    onDeleteTopForum = {
                                        viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                    },
                                    isTopForum = true
                                )
                            }
                        }
                        if (showHistoryForum || hasTopForum) {
                            item(key = "ForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                                IosSectionTitle(
                                    text = stringResource(id = R.string.forum_list_title)
                                )
                            }
                        }
                        items(
                            items = forums,
                            key = { it.forumId }
                        ) { item ->
                            ForumItem(
                                item,
                                listSingle,
                                onClick = {
                                    navigator.navigate(ForumPageDestination(it.forumName))
                                },
                                onUnfollow = {
                                    unfollowForum = it
                                    confirmUnfollowDialog.show()
                                },
                                onAddTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Add(it))
                                },
                                onDeleteTopForum = {
                                    viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                }
                            )
                        }
                    }
                }

                PullRefreshLogoIndicator(
                    refreshing = isLoading,
                    state = pullRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentPadding.calculateTopPadding()),
                    backgroundColor = LocalGlassColors.current.elevatedBackground,
                    contentColor = LocalGlassColors.current.content,
                )
            }
        }
        IosToastHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
            bottomInset = LocalMainBottomToastInset.current,
        )
    }
}

/** 首页大标题：与 [IosPinnedPage] 内的大标题同款排版，空态和列表态共用一份。 */
@Composable
private fun HomeLargeTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = R.string.title_main),
        style = IosTypography.largeTitle,
        fontWeight = FontWeight.Bold,
        color = LocalGlassColors.current.content,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

/**
 * 「经过贴吧」横向胶囊列表（对齐 MeiloX 首页的横向卡片行）。
 * 标题与列表左缘同一缩进，展开箭头用 SF Symbol 并随展开状态旋转。
 */
@Composable
private fun HistoryForumSection(
    forums: List<History>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onForumClick: (History) -> Unit,
) {
    val colors = LocalGlassColors.current
    val rotate by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "rotate"
    )
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                )
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
        ) {
            IosSectionTitle(
                text = stringResource(id = R.string.title_history_forum),
                modifier = Modifier.weight(1f),
            )
            SfIcon(
                systemName = "chevron.forward",
                contentDescription = stringResource(id = R.string.desc_show),
                tint = colors.tertiaryContent,
                size = 15.dp,
                weight = FontWeight.SemiBold,
                modifier = Modifier.rotate(rotate),
            )
        }
        AnimatedVisibility(visible = expanded) {
            LazyRow(
                contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 4.dp),
            ) {
                items(
                    forums,
                    key = { it.data }
                ) { item ->
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(Capsule())
                            .background(colors.elevatedBackground.copy(alpha = 0.92f))
                            .clickable(onClick = { onForumClick(item) })
                            .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Avatar(
                            data = item.avatar,
                            contentDescription = null,
                            size = 24.dp,
                            shape = CircleShape
                        )
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** 空态：iOS 大标题 + 说明 + 玻璃按钮（登录、前往发现）。 */
@Composable
fun EmptyScreen(
    loggedIn: Boolean,
    canOpenExplore: Boolean,
    onOpenExplore: () -> Unit
) {
    val navigator = LocalNavigator.current
    val colors = LocalGlassColors.current
    TipScreen(
        title = {
            Text(
                text = stringResource(
                    id = if (!loggedIn) R.string.title_empty_login else R.string.title_empty
                ),
                style = IosTypography.title2,
                color = colors.content,
                textAlign = TextAlign.Center,
            )
        },
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            if (!loggedIn) {
                Text(
                    text = stringResource(id = R.string.home_empty_login),
                    style = IosTypography.subheadline,
                    color = colors.secondaryContent,
                    textAlign = TextAlign.Center
                )
            }
        },
        actions = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!loggedIn) {
                    GlassButton(
                        onClick = { navigator.navigate(LoginPageDestination) },
                        modifier = Modifier.fillMaxWidth(),
                        emphasis = GlassEmphasis.Prominent,
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_login),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (canOpenExplore) {
                    GlassButton(
                        onClick = onOpenExplore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(id = R.string.button_go_to_explore),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    )
}
