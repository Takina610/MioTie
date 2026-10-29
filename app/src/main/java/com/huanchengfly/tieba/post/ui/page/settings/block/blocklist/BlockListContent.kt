package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.material.placeholder
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.glass.IosGroupedList
import com.huanchengfly.tieba.post.ui.glass.IosListRow
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.page.settings.components.GroupFooter
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.utils.GsonUtil

/** 屏蔽词列表内容：iOS 分组列表 + 空态 + 占位。 */

/** 关键词写进剪贴板：正则按原样拼接，否则以空格连接。 */
internal fun copyBlockKeywords(context: Context, item: Block) {
    val keywordsList: List<String> = GsonUtil.getGson().fromJson(
        item.keywords ?: "[]",
        object : TypeToken<List<String>>() {}.type
    )
    val merged = if (item.isRegex) {
        keywordsList.joinToString(separator = "")
    } else {
        keywordsList.joinToString(separator = " ")
    }
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("block", merged))
}

@Composable
internal fun BlockListContent(
    items: List<Block>,
    isLoading: Boolean,
    onCopy: (Block) -> Unit,
    onDelete: (Block) -> Unit,
) {
    if (isLoading && items.isEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(4) { BlockItemPlaceholder() }
        }
        return
    }
    if (items.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SfIcon("shield", null, size = 44.dp, tint = LocalGlassColors.current.tertiaryContent)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.summary_not_set),
                style = IosTypography.subheadline,
                color = LocalGlassColors.current.secondaryContent,
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            IosGroupedList {
                items.forEachIndexed { index, item ->
                    BlockRow(
                        item = item,
                        showTopSeparator = index != 0,
                        onCopy = { onCopy(item) },
                        onDelete = { onDelete(item) },
                    )
                }
            }
        }
        item {
            GroupFooter(
                text = androidx.compose.ui.text.AnnotatedString(
                    stringResource(id = R.string.tip_add_block)
                )
            )
        }
    }
}

@Composable
private fun BlockRow(
    item: Block,
    showTopSeparator: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    LongClickMenu(
        menuContent = {
            DropdownMenuItem(onClick = onCopy) {
                Text(text = stringResource(id = R.string.title_copy))
            }
            DropdownMenuItem(onClick = onDelete) {
                Text(text = stringResource(id = R.string.title_delete))
            }
        }
    ) {
        IosListRow(
            title = item.displayTitle(),
            subtitle = item.displaySubtitle(),
            systemName = if (item.type == Block.TYPE_USER) "person.crop.circle" else "bubble.left",
            showTopSeparator = showTopSeparator,
        )
    }
}

private fun Block.displayTitle(): String {
    if (type == Block.TYPE_USER) return username ?: ""
    val keywordsList: List<String> = runCatching {
        GsonUtil.getGson()
            .fromJson<List<String>>(
                keywords ?: "[]",
                object : TypeToken<List<String>>() {}.type
            )
    }.getOrDefault(emptyList())
    return keywordsList.joinToString(" ")
}

private fun Block.displaySubtitle(): String? = when {
    type == Block.TYPE_USER -> "UID: $uid"
    isRegex -> "正则"
    else -> null
}

@Composable
private fun BlockItemPlaceholder() {
    val color = LocalGlassColors.current.elevatedBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "",
            modifier = Modifier
                .width(160.dp)
                .height(14.dp)
                .placeholder(visible = true, color = color)
        )
    }
}
