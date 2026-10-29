package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.glass.IosGroupedList
import com.huanchengfly.tieba.post.ui.glass.IosPinnedListPage
import com.huanchengfly.tieba.post.ui.glass.IosPinnedPage
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassBackdrop
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.defaultGlassColors
import com.huanchengfly.tieba.post.ui.page.main.LocalMainBottomInsets
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

/**
 * iOS 设置页外壳（对齐 MeiloX 1.55.1 `SettingScreen` 的 `IosPinnedListPage`）：
 * 固定顶栏 + 渐进模糊 + 可折叠大标题的分组列表。设置根页、各设置子页与主界面「设置」标签页共用。
 *
 * @param title 顶栏与大标题文案
 * @param onNavigateBack 非空时顶栏显示返回键；标签页形态传 null
 * @param bottomPadding 列表底部呼吸位；默认取悬浮底栏避让位
 */
@Composable
fun IosSettingsScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    showsLargeTitle: Boolean = true,
    bottomPadding: Dp = LocalMainBottomInsets.current,
    content: LazyListScope.() -> Unit,
) {
    IosGlassPageScaffold(modifier = modifier) { background, snackbar ->
        IosPinnedListPage(
            title = title,
            bottomPadding = bottomPadding,
            modifier = Modifier.fillMaxSize(),
            listState = rememberLazyListState(),
            showsLargeTitle = showsLargeTitle,
            onNavigateBack = onNavigateBack,
            backgroundColor = background,
            content = content,
        )
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding),
        )
    }
}

/**
 * 非列表页的 iOS 外壳：固定顶栏 + 渐进模糊，内容自行决定布局（如表情包的分页网格、网页）。
 * 与 [IosSettingsScaffold] 共用同一套玻璃配色与页面底色。
 *
 * [showsLargeTitle] 为 true 时，标题按 iOS 规范以 **返回键下方的大标题** 呈现，
 * 顶栏只保留返回键与操作按钮（与设置页等列表页一致的视觉层级）。
 */
@Composable
fun IosTopBarScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    bottomPadding: Dp = 0.dp,
    showsLargeTitle: Boolean = true,
    /** WebView 等自刷新内容必须传 false，否则逐帧录制会与玻璃顶栏形成反馈闪烁。 */
    sampleContentBackdrop: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    IosGlassPageScaffold(modifier = modifier) { background, snackbar ->
        IosPinnedPage(
            // 大标题在内容区渲染时，顶栏不再重复显示小标题
            title = if (showsLargeTitle) "" else title,
            subtitle = subtitle.takeIf { !showsLargeTitle },
            bottomPadding = bottomPadding,
            modifier = Modifier.fillMaxSize(),
            onNavigateBack = onNavigateBack,
            actions = actions,
            backgroundColor = background,
            sampleContentBackdrop = sampleContentBackdrop,
        ) { contentPadding ->
            if (showsLargeTitle) {
                // 大标题要落在顶栏下方：沿用外壳给的顶部内边距（状态栏 + 顶栏 + 呼吸位），
                // 内容本身不再重复留顶部间距；水平方向交给内容自己决定（网页需要通栏）。
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(top = contentPadding.calculateTopPadding())
                ) {
                    IosLargeTitleHeader(title = title, subtitle = subtitle)
                    content(
                        PaddingValues(bottom = contentPadding.calculateBottomPadding())
                    )
                }
            } else {
                // 非大标题形态：内容自己接管水平方向。
                // 网页类内容（登录/内置浏览器）必须通栏，否则视口变窄会把 H5 表单挤出屏幕。
                content(
                    PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding(),
                    )
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}

/** iOS 页面大标题（与 [com.huanchengfly.tieba.post.ui.glass.IosPinnedListPage] 内的大标题同款排版）。 */
@Composable
private fun IosLargeTitleHeader(title: String, subtitle: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = IosTypography.largeTitle,
            fontWeight = FontWeight.Bold,
            color = LocalGlassColors.current.content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            Text(
                text = it,
                style = IosTypography.subheadline,
                color = LocalGlassColors.current.secondaryContent,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 两种 iOS 外壳共用的玻璃环境：
 * 配色取主题色相 + iOS 分组底色；内容不参与玻璃采样（自身即被采样的那一层），
 * 所以给行内 [com.huanchengfly.tieba.post.ui.glass.GlassToggle] 提供一张纯色 backdrop 而不会递归；
 * 并自行提供 snackbar 宿主（iOS 外壳没有 Material Scaffold）。
 */
@Composable
internal fun IosGlassPageScaffold(
    modifier: Modifier,
    content: @Composable BoxScope.(pageBackground: Color, snackbar: SnackbarHostState) -> Unit,
) {
    val themeColors = ExtendedTheme.colors
    val accentColor = MaterialTheme.colors.secondary
    val glassColors = remember(themeColors.isNightMode, accentColor) {
        defaultGlassColors(isDark = themeColors.isNightMode, accent = accentColor)
    }
    // 半透明主题要让壁纸透出来，其余主题直接用 iOS 分组底色
    val pageBackground = remember(glassColors, themeColors.theme) {
        if (ThemeUtil.isTranslucentTheme(themeColors.theme)) {
            glassColors.groupedBackground.copy(alpha = 0.72f)
        } else {
            glassColors.groupedBackground
        }
    }
    val contentBackdrop = rememberCanvasBackdrop {
        drawRect(pageBackground)
    }
    val scaffoldState = rememberScaffoldState()
    CompositionLocalProvider(
        LocalGlassColors provides glassColors,
        LocalGlassBackdrop provides contentBackdrop,
        LocalSnackbarHostState provides scaffoldState.snackbarHostState,
    ) {
        Box(modifier.fillMaxSize()) {
            content(pageBackground, scaffoldState.snackbarHostState)
        }
    }
}

/** 分组标题（对齐 MeiloX `SettingsSectionTitle`）。 */
@Composable
internal fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = IosTypography.subheadline,
        color = LocalGlassColors.current.secondaryContent,
        modifier = Modifier.padding(top = 12.dp, start = 16.dp),
    )
}

/** 分组卡片 + 标题（对齐 MeiloX `SettingsGroup`）。 */
@Composable
internal fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SettingsSectionTitle(title)
        IosGroupedList(content = content)
    }
}
