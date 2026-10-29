package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eygraber.compose.placeholder.material.placeholder
import com.github.panpf.sketch.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.utils.StringUtil
import com.kyant.capsule.ContinuousRoundedRectangle

/** 「我的」页的账号头部、统计卡与占位（与页面主体同一套 iOS 列表规范）。 */
@Composable
internal fun UserAccountHeader(account: Account, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.nameShow ?: account.name,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = LocalGlassColors.current.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = account.intro ?: stringResource(id = R.string.tip_no_intro),
                style = IosTypography.subheadline,
                color = LocalGlassColors.current.secondaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        AsyncImage(
            imageUri = StringUtil.getAvatarUrl(account.portrait),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        )
    }
}

/** 未登录头部：整块与右侧图标都可点，进入登录页。默认头像用 SF Symbol，与设置页一致。 */
@Composable
internal fun LoginHeader(onClick: () -> Unit) {
    val colors = LocalGlassColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.tip_login),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = colors.content,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        SfIcon(
            systemName = "person.crop.circle",
            contentDescription = null,
            size = 56.dp,
            tint = colors.secondaryContent,
        )
    }
}

@Composable
internal fun UserStatsCard(account: Account, onFollowClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ContinuousRoundedRectangle(26.dp))
            .background(LocalGlassColors.current.elevatedBackground)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserStatItem(
            statNum = account.concernNum?.toIntOrNull() ?: 0,
            statText = stringResource(id = R.string.text_stat_follow),
            modifier = Modifier.clickable(onClick = onFollowClick),
        )
        UserStatDivider()
        UserStatItem(
            statNum = account.fansNum?.toIntOrNull() ?: 0,
            statText = stringResource(id = R.string.text_stat_fans),
        )
        UserStatDivider()
        UserStatItem(
            statNum = account.postNum?.toIntOrNull() ?: 0,
            statText = stringResource(id = R.string.title_stat_posts_num),
        )
    }
}

@Composable
private fun RowScope.UserStatItem(
    statNum: Int,
    statText: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGlassColors.current
    Column(
        modifier = modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$statNum",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.content,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            style = IosTypography.caption,
            color = colors.secondaryContent
        )
    }
}

@Composable
private fun UserStatDivider() {
    Box(
        Modifier
            .size(width = 1.dp, height = 28.dp)
            .background(LocalGlassColors.current.separator)
    )
}

@Composable
internal fun UserAccountPlaceholder() {
    val placeholderColor = ExtendedTheme.colors.chip
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(width = 140.dp, height = 28.dp)
                    .placeholder(visible = true, color = placeholderColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 14.dp)
                    .placeholder(visible = true, color = placeholderColor)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .placeholder(visible = true, color = placeholderColor)
        )
    }
}
