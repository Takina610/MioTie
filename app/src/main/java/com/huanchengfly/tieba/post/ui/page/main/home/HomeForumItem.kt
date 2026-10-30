package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text as MaterialText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.shapes.Capsule

private val ForumCardShape = ContinuousRoundedRectangle(18.dp)

/** 首页分组标题：20sp 粗体（对齐 MeiloX 首页 `Title`），左缘与列表卡片对齐。 */
@Composable
internal fun IosSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = LocalGlassColors.current.content,
        maxLines = 1,
        modifier = modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
    )
}

/**
 * 贴吧等级配色：1-3 绿牌、4-9 蓝牌、10-15 黄牌、16 以上红牌。
 * 与贴吧客户端一致，等级一眼可辨。
 */
internal fun tiebaLevelColor(level: Int): Color = when {
    level <= 3 -> Color(0xFF57C22D)
    level <= 9 -> Color(0xFF2F8FFF)
    level <= 15 -> Color(0xFFF2A31B)
    else -> Color(0xFFF0493E)
}

@Composable
private fun ForumItemMenuContent(
    menuState: MenuState,
    isTopForum: Boolean,
    onDeleteTopForum: () -> Unit,
    onAddTopForum: () -> Unit,
    onCopyName: () -> Unit,
    onUnfollow: () -> Unit,
) {
    DropdownMenuItem(
        onClick = {
            if (isTopForum) {
                onDeleteTopForum()
            } else {
                onAddTopForum()
            }
            menuState.expanded = false
        }
    ) {
        if (isTopForum) {
            MaterialText(text = stringResource(id = R.string.menu_top_del))
        } else {
            MaterialText(text = stringResource(id = R.string.menu_top))
        }
    }
    DropdownMenuItem(
        onClick = {
            onCopyName()
            menuState.expanded = false
        }
    ) {
        MaterialText(text = stringResource(id = R.string.title_copy_forum_name))
    }
    DropdownMenuItem(
        onClick = {
            onUnfollow()
            menuState.expanded = false
        }
    ) {
        MaterialText(text = stringResource(id = R.string.button_unfollow))
    }
}

/**
 * 等级胶囊：底色取等级配色的淡化版，文字用同一色，深浅主题下都保持可读。
 * 已签到的勾选不放进胶囊，避免与等级数字挤在一起。
 */
@Composable
private fun LevelPill(
    levelId: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val level = levelId.toIntOrNull() ?: 0
    val color = tiebaLevelColor(level)
    Box(
        modifier = modifier
            .clip(Capsule())
            .background(color.copy(alpha = 0.18f))
            .padding(
                horizontal = if (compact) 5.dp else 7.dp,
                vertical = if (compact) 1.dp else 2.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Lv.$levelId",
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

/**
 * 吧卡片：双列时是并排的小卡，单列时是通栏大卡，都是同一套圆角与底色。
 *
 * 单列宽度充裕：等级胶囊跟在热度后面、签到勾放行尾。
 * 双列只有半屏宽：等级胶囊与签到勾都留在副标题行，吧名独占整行，
 * 两者各自占位，勾不会挤进等级胶囊里。
 */
@Composable
private fun ForumItemContent(
    item: HomeUiState.Forum,
    listSingle: Boolean,
) {
    val colors = LocalGlassColors.current
    val hotText = stringResource(R.string.hot_num, item.hotNum.getShortNumString())
    val avatarSize = if (listSingle) 44.dp else 40.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ForumCardShape)
            .background(colors.elevatedBackground.copy(alpha = 0.92f))
            .padding(
                horizontal = if (listSingle) 14.dp else 12.dp,
                vertical = if (listSingle) 12.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            data = item.avatar,
            size = avatarSize,
            contentDescription = stringResource(R.string.forum_portrait)
        )
        Spacer(modifier = Modifier.width(if (listSingle) 12.dp else 9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.forumName,
                style = IosTypography.body,
                fontWeight = FontWeight.SemiBold,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = hotText,
                    style = IosTypography.caption,
                    color = colors.secondaryContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                LevelPill(levelId = item.levelId, compact = !listSingle)
                if (!listSingle && item.isSign) {
                    SfIcon(
                        systemName = "checkmark.circle.fill",
                        contentDescription = stringResource(id = R.string.tip_signed),
                        size = 13.dp,
                        tint = colors.accent,
                    )
                }
            }
        }
        if (listSingle && item.isSign) {
            Spacer(modifier = Modifier.width(8.dp))
            SfIcon(
                systemName = "checkmark.circle.fill",
                contentDescription = stringResource(id = R.string.tip_signed),
                size = 18.dp,
                tint = colors.accent,
            )
        }
    }
}

@Composable
internal fun ForumItem(
    item: HomeUiState.Forum,
    listSingle: Boolean,
    onClick: (HomeUiState.Forum) -> Unit,
    onUnfollow: (HomeUiState.Forum) -> Unit,
    onAddTopForum: (HomeUiState.Forum) -> Unit,
    onDeleteTopForum: (HomeUiState.Forum) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            ForumItemMenuContent(
                menuState = menuState,
                isTopForum = isTopForum,
                onDeleteTopForum = { onDeleteTopForum(item) },
                onAddTopForum = { onAddTopForum(item) },
                onCopyName = {
                    TiebaUtil.copyText(context, item.forumName)
                },
                onUnfollow = { onUnfollow(item) }
            )
        },
        menuState = menuState,
        shape = ForumCardShape,
        onClick = {
            onClick(item)
        }
    ) {
        ForumItemContent(item = item, listSingle = listSingle)
    }
}
