package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.navigation.compose.currentBackStackEntryAsState
import com.huanchengfly.tieba.post.LocalDevicePosture
import com.huanchengfly.tieba.post.LocalNavController
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowHeightSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.glass.recordBackdropSource
import com.huanchengfly.tieba.post.ui.glass.rememberRecordedBackdrop
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.MyPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomeUiIntent
import com.huanchengfly.tieba.post.ui.page.main.home.HomeViewModel
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SplashState
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
private fun NavigationWrapper(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationType: MainNavigationType,
    navigationContentPosition: MainNavigationContentPosition,
    content: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == MainNavigationType.PERMANENT_NAVIGATION_DRAWER) {
            NavigationDrawerContent(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        AnimatedVisibility(visible = navigationType == MainNavigationType.NAVIGATION_RAIL) {
            NavigationRail(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationGraphicsApi::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun MainPage(
    navigator: DestinationsNavigator,
    viewModel: MainViewModel = pageViewModel<MainUiIntent, MainViewModel>(emptyList()),
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val account = LocalAccount.current
    val windowHeightSizeClass by rememberUpdatedState(newValue = windowSizeClass.heightSizeClass)
    val windowWidthSizeClass by rememberUpdatedState(newValue = windowSizeClass.widthSizeClass)
    val foldingDevicePosture by LocalDevicePosture.current

    val messageCount by viewModel.uiState.collectPartialAsState(
        prop1 = MainUiState::messageCount,
        initial = 0
    )

    val notificationCountFlow = LocalNotificationCountFlow.current
    LaunchedEffect(null) {
        notificationCountFlow.collect {
            viewModel.send(MainUiIntent.NewMessage.Receive(it))
        }
    }

    val hideExplore by rememberPreferenceAsState(
        key = booleanPreferencesKey("hideExplore"),
        defaultValue = LocalContext.current.appPreferences.hideExplore
    )
    val defaultStart by rememberPreferenceAsState(
        key = intPreferencesKey("defaultStart"),
        defaultValue = LocalContext.current.appPreferences.defaultStart
    )
    val pageCount by remember {
        derivedStateOf {
            if (hideExplore) 3 else 4
        }
    }
    var currentPosition by rememberSaveable {
        mutableIntStateOf(if (hideExplore && defaultStart > 0) defaultStart - 1 else defaultStart)
    }

    val onChangePosition: (Int) -> Unit = { currentPosition = it }

    // 启动屏就绪兜底：起始页非首页时（动态/消息/我的），
    // HomePage 不会组合、无法触发 markReady，此处补位避免启动屏只能等超时退场
    LaunchedEffect(Unit) {
        if (currentPosition != 0) {
            SplashState.markReady()
        }
    }

    LaunchedEffect(hideExplore) {
        if (currentPosition == 3 && hideExplore) {
            currentPosition = 2
        }
    }

    val homeViewModel = if (account != null) {
        pageViewModel<HomeUiIntent, HomeViewModel>(listOf(HomeUiIntent.Refresh))
    } else pageViewModel()

    val coroutineScope = rememberCoroutineScope()
    val themeColors = ExtendedTheme.colors
    val navigationItems by remember {
        derivedStateOf {
            listOfNotNull(
                NavigationItem(
                    id = "home",
                    icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_inventory_2) },
                    title = { stringResource(id = R.string.title_main) },
                    content = {
                        HomePage(
                            viewModel = homeViewModel,
                            canOpenExplore = !LocalContext.current.appPreferences.hideExplore
                        ) {
                            onChangePosition(1)
                        }
                    }
                ),
                if (hideExplore) null
                else NavigationItem(
                    id = "explore",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_toy_fans)
                    },
                    title = { stringResource(id = R.string.title_explore) },
                    content = {
                        ExplorePage()
                    }
                ),
                NavigationItem(
                    id = "notification",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_notifications)
                    },
                    title = { stringResource(id = R.string.title_notifications) },
                    badge = messageCount > 0,
                    badgeText = "$messageCount",
                    onClick = {
                        viewModel.send(MainUiIntent.NewMessage.Clear)
                    },
                    content = {
                        NotificationsPage()
                    }
                ),
                NavigationItem(
                    id = "settings",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_settings)
                    },
                    title = { stringResource(id = R.string.title_settings) },
                    content = {
                        SettingsTabPage()
                    }
                ),
            ).toImmutableList()
        }
    }
    val navigationType by remember {
        derivedStateOf {
            when (windowWidthSizeClass) {
                WindowWidthSizeClass.Compact -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }

                WindowWidthSizeClass.Medium -> {
                    MainNavigationType.NAVIGATION_RAIL
                }

                WindowWidthSizeClass.Expanded -> {
                    if (foldingDevicePosture is DevicePosture.BookPosture) {
                        MainNavigationType.NAVIGATION_RAIL
                    } else {
                        MainNavigationType.PERMANENT_NAVIGATION_DRAWER
                    }
                }

                else -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }
            }
        }
    }

    /**
     * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
     * ergonomics and reachability depending upon the height of the device.
     */
    val navigationContentPosition by remember {
        derivedStateOf {
            when (windowHeightSizeClass) {
                WindowHeightSizeClass.Compact -> {
                    MainNavigationContentPosition.TOP
                }

                WindowHeightSizeClass.Medium,
                WindowHeightSizeClass.Expanded -> {
                    MainNavigationContentPosition.CENTER
                }

                else -> {
                    MainNavigationContentPosition.TOP
                }
            }
        }
    }
    val onReselected: (Int) -> Unit = {
        coroutineScope.emitGlobalEvent(
            GlobalEvent.Refresh(key = navigationItems[it].id)
        )
    }
    val floatingBottomNav by rememberPreferenceAsState(
        key = booleanPreferencesKey("floatingBottomNav"),
        defaultValue = LocalContext.current.appPreferences.floatingBottomNav
    )
    val keepBottomBarExpanded by rememberPreferenceAsState(
        key = booleanPreferencesKey("keepBottomBarExpanded"),
        defaultValue = LocalContext.current.appPreferences.keepBottomBarExpanded
    )
    // 玻璃底栏的背景源：内容层录制为 backdrop，悬浮 Dock 用 backdrop 库采样（MeiloX 同款）
    val dockBackdrop = rememberRecordedBackdrop()
    val saveableStateHolder = rememberSaveableStateHolder()
    // 真悬浮布局:胶囊叠于内容之上,内容 edge-to-edge;经典贴底/Rail 模式沿用 Scaffold 避让
    val floatingLayout = navigationType == MainNavigationType.BOTTOM_NAVIGATION && floatingBottomNav
    // 滚动下沉（对齐 MeiloX 1.55.1 NavigationBar）：10f 阈值；固定展开开启时不收放；
    // 切 tab / 路由变化都不重置形态，只有点击下沉钮或打开固定开关才重新展开。
    var bottomBarsExpanded by rememberSaveable { mutableStateOf(true) }
    val keepBottomBarExpandedState = rememberUpdatedState(keepBottomBarExpanded)
    val nestedScrollConnection = remember(floatingLayout) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!floatingLayout) return Offset.Zero
                // 「固定底部标签栏」开启时滚动不再收放底栏（对齐 MeiloX）
                if (keepBottomBarExpandedState.value) return Offset.Zero
                if (available.y < -10f) bottomBarsExpanded = false
                if (available.y > 10f) bottomBarsExpanded = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(keepBottomBarExpanded) {
        if (keepBottomBarExpanded) bottomBarsExpanded = true
    }
    // M2 Scaffold 不做系统 insets 避让(contentWindowInsets 传的是全 0),悬浮模式下底部
    // 悬浮区(个人信息条 + 导航胶囊)需在此统一补足:呼吸位 = 悬浮区总高 + 呼吸(8dp) + 系统导航栏,
    // 列表末项才能完整滚出胶囊,加载提示胶囊也停在这条线上。
    val navigationBarsInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 个人信息条始终进入「我」页面（未登录也在页面内引导登录）
    val onUserClick: () -> Unit = { navigator.navigate(MyPageDestination) }
    // 对齐 MeiloX：搜索钮只负责把搜索页入栈一次并请求聚焦
    val navController = LocalNavController.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val isSearchDestination = currentBackStackEntry?.destination?.route == SearchPageDestination.route
    val onSearchClick: () -> Unit = {
        if (!isSearchDestination) {
            navigator.navigate(SearchPageDestination(autoFocus = true))
        }
    }
    ProvideNavigator(navigator = navigator) {
        NavigationWrapper(
            currentPosition = currentPosition,
            onChangePosition = onChangePosition,
            onReselected = onReselected,
            navigationItems = navigationItems,
            navigationType = navigationType,
            navigationContentPosition = navigationContentPosition
        ) {
            // 收缩态只露出 44dp 圆钮；提示胶囊贴着当前可见的底栏上沿停靠
            val bottomBarsCompact = !bottomBarsExpanded && !keepBottomBarExpanded
            // 系统导航栏 inset 由 IosToastHost 的 navigationBarsPadding 补，避免重复叠加
            val toastBottomInset = when {
                !floatingLayout -> 0.dp
                bottomBarsCompact -> MainBottomCompactOverlayHeight + 8.dp
                else -> MainBottomBreathing
            }
            CompositionLocalProvider(
                LocalMainBottomInsets provides if (floatingLayout) {
                    MainBottomBreathing + navigationBarsInset
                } else 0.dp,
                LocalMainBottomToastInset provides toastBottomInset,
            ) {
                MyScaffold(
                    backgroundColor = Color.Transparent,
                    modifier = Modifier.fillMaxSize(),
                    // 悬浮模式:bottomBar 槽留空,M2 Scaffold 内容自然 edge-to-edge,
                    // 底部由各列表的 LocalMainBottomInsets 呼吸位接管
                    bottomBar = {
                        AnimatedVisibility(
                            visible = navigationType == MainNavigationType.BOTTOM_NAVIGATION && !floatingBottomNav
                        ) {
                            Column {
                                MainUserInfoRow(
                                    account = account,
                                    onClick = onUserClick,
                                    themeColors = themeColors,
                                )
                                BottomNavigation(
                                    currentPosition = currentPosition,
                                    onChangePosition = onChangePosition,
                                    onReselected = onReselected,
                                    navigationItems = navigationItems,
                                    themeColors = themeColors,
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        KeepAliveTabHost(
                            currentId = navigationItems.getOrElse(currentPosition) {
                                navigationItems.last()
                            }.id,
                            pageIds = navigationItems.map { it.id },
                            saveableStateHolder = saveableStateHolder,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (floatingLayout) {
                                        Modifier.recordBackdropSource(dockBackdrop)
                                    } else Modifier
                                ),
                        ) { id ->
                            Box(modifier = Modifier.padding(paddingValues)) {
                                navigationItems.firstOrNull { it.id == id }?.content?.invoke()
                            }
                        }
                        if (floatingLayout) {
                            // 底栏随主页一起走 MeiloX 的 homeAlphaTransition 交叉淡化；
                            // 下沉形态不因路由切换重置。
                            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                MainBottomBars(
                                    account = account,
                                    currentPosition = currentPosition,
                                    onChangePosition = onChangePosition,
                                    onExpand = { bottomBarsExpanded = true },
                                    navigationItems = navigationItems,
                                    backdrop = dockBackdrop,
                                    onUserClick = onUserClick,
                                    onSearchClick = onSearchClick,
                                    compact = !bottomBarsExpanded && !keepBottomBarExpanded,
                                    themeColors = themeColors,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    BackHandler(enabled = currentPosition != 0) {
        currentPosition = 0
    }
}

/**
 * 顶部 tab 的保活宿主：页面首次访问后保留组合，切换时只把旧页透明度降到 0。
 * 页面只在测量/放置阶段判断可见性，透明度动画不会触发重组。
 */
@Composable
private fun KeepAliveTabHost(
    currentId: String,
    pageIds: List<String>,
    saveableStateHolder: SaveableStateHolder,
    modifier: Modifier = Modifier,
    content: @Composable (id: String) -> Unit,
) {
    val visitedIds = remember { mutableStateListOf(currentId) }
    val pageAlphas = remember { mutableMapOf<String, Animatable<Float, AnimationVector1D>>() }
    LaunchedEffect(currentId, pageIds) {
        val available = pageIds.toSet()
        // 关闭/开启「动态」等 tab 列表变化时，丢弃已不存在的页面。
        visitedIds.retainAll(available)
        pageAlphas.keys.retainAll(available)
        if (currentId !in visitedIds) visitedIds.add(currentId)
    }
    val pages = visitedIds.map { id ->
        val alpha = pageAlphas.getOrPut(id) { Animatable(1f) }
        LaunchedEffect(id, currentId) {
            if (id == currentId) {
                alpha.snapTo(1f)
            } else if (alpha.value > 0f) {
                alpha.animateTo(0f, tween(durationMillis = 200, easing = EaseInCubic))
            }
        }
        id to alpha
    }
    Layout(
        content = {
            pages.forEach { (id, alpha) ->
                key(id) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.alpha = alpha.value }
                            .then(
                                if (id != currentId) {
                                    Modifier.clearAndSetSemantics {}
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        saveableStateHolder.SaveableStateProvider(id) {
                            content(id)
                        }
                    }
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val current = pages.indexOfFirst { (id, _) -> id == currentId }
        layout(constraints.maxWidth, constraints.maxHeight) {
            // 当前页在底层；淡出中的旧页盖在上面，透明度降低时露出新页。
            placeables.getOrNull(current)?.placeRelative(0, 0)
            placeables.forEachIndexed { index, placeable ->
                if (index != current && pages[index].second.value > 0f) {
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}

