package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.DockCompactHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.DockNavBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.DockSearchButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DockSearchSize
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingDock
import com.huanchengfly.tieba.post.ui.widgets.compose.GlassCapsule
import com.huanchengfly.tieba.post.ui.widgets.compose.glassDockTint
import com.huanchengfly.tieba.post.ui.widgets.compose.supportsBackdropBlur
import com.huanchengfly.tieba.post.utils.StringUtil
import com.kyant.backdrop.Backdrop
import kotlinx.collections.immutable.ImmutableList

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

/**
 * 底部悬浮区（对齐 MeiloX）：
 * - 展开态：上方个人信息条，下方导航胶囊 + 搜索圆钮；
 * - 下沉态（滚动后）：三者并成一行——左侧当前页圆形按钮、中间个人信息条、右侧搜索圆钮，
 *   宽度/高度/透明度按 [compact] 以 spring 过渡。
 *
 * @param onExpand 点击下沉态胶囊展开回完整导航栏
 * @param onUserClick 个人信息条点击（进入我的页面）
 */
@Composable
fun MainBottomBars(
    account: Account?,
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    onExpand: () -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    backdrop: Backdrop,
    onUserClick: () -> Unit,
    onSearchClick: () -> Unit,
    compact: Boolean,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val compactProgress by animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MainBottomBarsCompact",
    )
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
        val progress = compactProgress.coerceIn(0f, 1f)
        val usableWidth = maxWidth
        val gap = MainBottomBarGap
        val expandedDockWidth = usableWidth - DockSearchSize - gap
        // 展开: 44 + 8 + 64；下沉: 44（三者同排）
        val overlayHeight = lerp(UserInfoBarHeight + gap + DockNavBarHeight, DockCompactHeight, progress)
        val infoWidth = lerp(usableWidth, usableWidth - (DockCompactHeight + gap) * 2, progress)
        val compactInfoOffset = (DockCompactHeight + gap) * progress
        val dockWidth = lerp(expandedDockWidth, DockCompactHeight, progress)

        Layout(
            content = {
                UserInfoBar(
                    account = account,
                    backdrop = backdrop,
                    themeColors = themeColors,
                    onClick = onUserClick,
                )
                FloatingDock(
                    currentPosition = currentPosition,
                    onChangePosition = onChangePosition,
                    onReselected = onReselected,
                    onExpand = onExpand,
                    navigationItems = navigationItems,
                    backdrop = backdrop,
                    compactProgress = progress,
                    expandedWidth = expandedDockWidth,
                    themeColors = themeColors,
                )
                DockSearchButton(
                    backdrop = backdrop,
                    compactProgress = progress,
                    onClick = onSearchClick,
                    themeColors = themeColors,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(overlayHeight),
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val infoW = infoWidth.roundToPx()
            val dockW = dockWidth.roundToPx()
            val searchSize = lerp(DockSearchSize, DockCompactHeight, progress).roundToPx()
            val infoH = UserInfoBarHeight.roundToPx()
            val dockHeight = lerp(DockNavBarHeight, DockCompactHeight, progress).roundToPx()

            val infoPlaceable = measurables[0].measure(Constraints.fixed(infoW, infoH))
            val dockPlaceable = measurables[1].measure(Constraints.fixed(dockW, dockHeight))
            val searchPlaceable = measurables[2].measure(Constraints.fixed(searchSize, searchSize))
            layout(width, height) {
                infoPlaceable.place(compactInfoOffset.roundToPx(), 0)
                dockPlaceable.place(0, height - dockHeight)
                // 展开态圆钮在 64dp 胶囊带内垂直居中（对齐 MeiloX Row CenterVertically）
                searchPlaceable.place(
                    width - searchSize,
                    height - dockHeight + (dockHeight - searchSize) / 2,
                )
            }
        }
    }
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