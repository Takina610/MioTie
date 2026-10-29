package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.glass.CompactBottomControlIconSize
import com.huanchengfly.tieba.post.ui.glass.GlassBottomBar
import com.huanchengfly.tieba.post.ui.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.glass.GlassSurfaceStyle
import com.huanchengfly.tieba.post.ui.glass.GlassTabItem
import com.huanchengfly.tieba.post.ui.glass.LocalGlassBackdrop
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.defaultGlassColors
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.GlassCapsule
import com.huanchengfly.tieba.post.ui.widgets.compose.glassDockTint
import com.huanchengfly.tieba.post.ui.widgets.compose.supportsBackdropBlur
import com.huanchengfly.tieba.post.utils.StringUtil
import com.kyant.backdrop.Backdrop
import kotlinx.collections.immutable.ImmutableList

/** 导航胶囊高度（对齐 MeiloX 1.55.1 NavigationBarSurfaceHeight = 64dp）。 */
private val DockNavBarHeight = 64.dp

/** 下沉态圆钮直径（对齐 MeiloX MiniPlayerHeight = 44dp）。 */
private val DockCompactHeight = 44.dp

/** 个人信息条高度（对齐 MeiloX 迷你播放条）。 */
private val UserInfoBarHeight = 44.dp

/** 个人信息条与导航胶囊的间距（展开态）。 */
private val MainBottomBarGap = 8.dp

/** 悬浮底栏距屏幕底部的边距（与系统导航栏 inset 取较大值，对齐 MeiloX NavigationBarBottomMargin = 8dp）。 */
private val MainBottomBarMargin = 8.dp

/** 悬浮底栏水平外边距。 */
private val MainBottomBarHorizontalMargin = 20.dp

/** 个人信息条内头像尺寸。 */
private val UserInfoBarAvatarSize = 28.dp

/**
 * 底部悬浮区总高度（信息条 + 间距 + 导航胶囊 + 底边距），不含系统导航栏 inset。
 * 列表滚动时按此高度预留呼吸位，避免最后一条内容被悬浮栏遮挡。
 */
val MainBottomOverlayHeight =
    UserInfoBarHeight + MainBottomBarGap + DockNavBarHeight + MainBottomBarMargin

/** 搜索钮宿主边长（对齐 MeiloX 1.55.1 的 64dp Box）。 */
private val SearchHostSize = 64.dp

/** 搜索钮下沉态直径（64 - 16dp，对齐 GlassIconButton morph）。 */
private val CompactSearchSize = 48.dp

/** 个人信息条下沉时在 64dp 行内的垂直内缩：居中对齐 44dp 圆钮。 */
private val UserInfoCompactInset = (DockNavBarHeight - UserInfoBarHeight) / 2f

/**
 * 底部悬浮区（照搬 MeiloX 1.55.1 AnimatedBottomNavigationRow）：
 * - 展开态：上方个人信息条，下方 GlassBottomBar + 搜索圆钮；
 * - 下沉态：导航栏在 64dp 宿主内缩成 44dp 圆钮，个人信息条与搜索钮同排居中。
 *
 * @param onExpand 点击下沉态圆钮展开回完整导航栏
 * @param onUserClick 个人信息条点击（进入我的页面）
 */
@Composable
fun MainBottomBars(
    account: Account?,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onExpand: () -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    backdrop: Backdrop,
    onUserClick: () -> Unit,
    onSearchClick: () -> Unit,
    compact: Boolean,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val compactProgress = animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MainBottomBarsCompact",
    )
    val accentColor = MaterialTheme.colors.secondary
    val glassColors = remember(themeColors.isNightMode, accentColor) {
        defaultGlassColors(
            isDark = themeColors.isNightMode,
            accent = accentColor,
        )
    }
    val tabLabels = ArrayList<String>(navigationItems.size)
    var tabIndex = 0
    while (tabIndex < navigationItems.size) {
        tabLabels += navigationItems[tabIndex].title(tabIndex == currentPosition)
        tabIndex++
    }
    val tabItems = rememberTabItems(navigationItems, currentPosition, tabLabels)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    .union(WindowInsets(bottom = MainBottomBarMargin))
            )
            .padding(
                start = MainBottomBarHorizontalMargin,
                end = MainBottomBarHorizontalMargin,
            ),
    ) {
        val usableWidth = maxWidth
        val gap = MainBottomBarGap
        val expandedDockWidth = usableWidth - SearchHostSize - gap

        // 进度只在 measure/place 阶段读取，弹簧每一帧只触发重新测量，不重组整条底栏。
        Layout(
            content = {
                UserInfoBar(
                    account = account,
                    backdrop = backdrop,
                    themeColors = themeColors,
                    onClick = onUserClick,
                )
                CompositionLocalProvider(
                    LocalGlassColors provides glassColors,
                    LocalGlassBackdrop provides backdrop,
                ) {
                    GlassBottomBar(
                        items = tabItems,
                        selectedKey = currentPosition,
                        onSelected = onChangePosition,
                        onExpand = onExpand,
                        compactProgress = compactProgress,
                        compactSize = DockCompactHeight,
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        GlassSearchButton(
                            backdrop = backdrop,
                            compactProgress = compactProgress,
                            onClick = onSearchClick,
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val progress = compactProgress.value.coerceIn(0f, 1f)
            // 展开: 44 + 8 + 64；下沉: 64（GlassBottomBar 宿主保持 64，内部缩到 44）
            val height = lerp(
                UserInfoBarHeight + gap + DockNavBarHeight,
                DockNavBarHeight,
                progress,
            ).roundToPx()
            val infoW = lerp(
                usableWidth,
                usableWidth - (DockCompactHeight + gap) - (CompactSearchSize + gap),
                progress,
            ).roundToPx()
            val compactInfoOffset = ((DockCompactHeight + gap) * progress).roundToPx()
            val infoCompactInset = (UserInfoCompactInset * progress).roundToPx()
            val dockW = expandedDockWidth.roundToPx()
            val dockH = DockNavBarHeight.roundToPx()
            val searchHost = SearchHostSize.roundToPx()
            val infoH = UserInfoBarHeight.roundToPx()

            val infoPlaceable = measurables[0].measure(Constraints.fixed(infoW, infoH))
            val dockPlaceable = measurables[1].measure(Constraints.fixed(dockW, dockH))
            val searchPlaceable = measurables[2].measure(Constraints.fixed(searchHost, dockH))
            layout(width, height) {
                infoPlaceable.place(
                    x = compactInfoOffset,
                    y = infoCompactInset,
                )
                dockPlaceable.place(0, height - dockH)
                searchPlaceable.place(width - searchHost, height - dockH)
            }
        }
    }
}

/** 搜索钮（对齐 MeiloX 1.55.1 AnimatedHomeSearchButton）。 */
@Composable
private fun GlassSearchButton(
    backdrop: Backdrop,
    compactProgress: State<Float>,
    onClick: () -> Unit,
) {
    // 尺寸与图标缩放都放在 layout/draw 阶段读取进度，避免弹簧期间每帧重组。
    val sizeModifier = remember(compactProgress) {
        Modifier.layout { measurable, constraints ->
            val progress = compactProgress.value.coerceIn(0f, 1f)
            val size = lerp(SearchHostSize, CompactSearchSize, progress).roundToPx()
            val placeable = measurable.measure(Constraints.fixed(size, size))
            layout(size, size) { placeable.placeRelative(0, 0) }
        }
    }
    val iconScaleModifier = remember(compactProgress) {
        Modifier.graphicsLayer {
            val progress = compactProgress.value.coerceIn(0f, 1f)
            val scale = 1f + (CompactBottomControlIconSize.value / 26f - 1f) * progress
            scaleX = scale
            scaleY = scale
        }
    }
    GlassIconButton(
        onClick = onClick,
        backdrop = backdrop,
        style = GlassSurfaceStyle.Navigation,
        modifier = sizeModifier,
        morphProgress = compactProgress,
        morphCaptureWidth = SearchHostSize,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = LocalGlassColors.current.content,
            modifier = Modifier
                .size(26.dp)
                .then(iconScaleModifier),
        )
    }
}

@Composable
private fun rememberTabItems(
    items: ImmutableList<NavigationItem>,
    currentPosition: Int,
    labels: List<String>,
): List<GlassTabItem<Int>> = remember(items, currentPosition, labels) {
    items.mapIndexed { index, item ->
        GlassTabItem(
            key = index,
            label = labels[index],
            icon = { active, tint, size -> MioTieTabIcon(item, active, tint, size) },
            contentDescription = labels[index],
            badgeText = if (item.badge) item.badgeText ?: "" else null,
        )
    }
}

/** 还原 MioTie 原有的 AnimatedImageVector tab 图标。 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun MioTieTabIcon(
    item: NavigationItem,
    active: Boolean,
    tint: Color,
    size: Dp,
) {
    Icon(
        painter = rememberAnimatedVectorPainter(
            animatedImageVector = item.icon(),
            atEnd = active,
        ),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun UserInfoBar(
    account: Account?,
    backdrop: Backdrop,
    themeColors: ExtendedColors,
    onClick: () -> Unit,
) {
    val blurSupported = supportsBackdropBlur()
    val tint = glassDockTint(themeColors.isNightMode, blurSupported)
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
        label = "UserInfoBarPress",
    )
    GlassCapsule(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1f + 0.015f * press
                scaleY = 1f + 0.05f * press
            },
        shape = CircleShape,
        backdrop = backdrop,
        tint = tint,
        isNightMode = themeColors.isNightMode,
        blurSupported = blurSupported,
    ) {
        UserInfoContent(
            account = account,
            onClick = onClick,
            onPressChange = { pressed = it },
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .padding(start = 8.dp, end = 14.dp),
        )
    }
}

/**
 * 传统贴底栏模式下的个人信息行：与悬浮胶囊同内容，平铺在底栏上方。
 */
@Composable
fun MainUserInfoRow(
    account: Account?,
    onClick: () -> Unit,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    UserInfoContent(
        account = account,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(UserInfoBarHeight)
            .background(themeColors.bottomBar)
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun UserInfoContent(
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    onPressChange(true)
                    tryAwaitRelease()
                    onPressChange(false)
                },
                onTap = { onClick() },
            )
        },
    ) {
        if (account != null) {
            Avatar(
                data = StringUtil.getAvatarUrl(account.portrait),
                size = UserInfoBarAvatarSize,
                contentDescription = null,
            )
        } else {
            AvatarPlaceholder(size = UserInfoBarAvatarSize)
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = account?.let { it.nameShow ?: it.name }
                    ?: stringResource(id = R.string.title_empty_login),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (account != null) {
                Text(
                    text = stringResource(id = R.string.text_profile_user_id, account.uid),
                    fontSize = 11.sp,
                    color = ExtendedTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}