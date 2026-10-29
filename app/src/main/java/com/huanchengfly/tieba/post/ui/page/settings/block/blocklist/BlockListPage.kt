package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.material.placeholder
import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.glass.IosGroupedList
import com.huanchengfly.tieba.post.ui.glass.IosListRow
import com.huanchengfly.tieba.post.ui.glass.IosScrollableTabRow
import com.huanchengfly.tieba.post.ui.page.settings.IosTopBarScaffold
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.page.settings.components.GroupFooter
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.GsonUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

/**
 * 屏蔽词管理（对齐 MeiloX 的 iOS Lists）：
 * 固定顶栏 + 胶囊分段选择黑/白名单 + 26dp 连续圆角分组列表，新增入口放在顶栏右侧。
 */
@Destination
@Composable
fun BlockListPage(
    viewModel: BlockListViewModel = pageViewModel<BlockListUiIntent, BlockListViewModel>(
        listOf(BlockListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    var addBlockCategory by remember { mutableStateOf(Block.CATEGORY_BLACK_LIST) }
    val dialogState = rememberDialogState()
    var isRegex by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val blackList by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::blackList,
        initial = emptyList()
    )
    val whiteList by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::whiteList,
        initial = emptyList()
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = BlockListUiState::isLoading,
        initial = false
    )

    PromptDialog(
        onConfirm = {
            viewModel.send(
                BlockListUiIntent.Add(
                    category = addBlockCategory,
                    // 避免正则中的空格导致正则失效
                    keywords = if (isRegex) listOf(it) else it.split(" "),
                    isRegex = isRegex
                )
            )
            isRegex = false
        },
        dialogState = dialogState,
        title = {
            Text(
                text = if (addBlockCategory == Block.CATEGORY_WHITE_LIST) stringResource(id = R.string.title_add_white)
                else stringResource(id = R.string.title_add_black)
            )
        }
    ) {
        Column {
            Text(text = stringResource(id = R.string.tip_add_block))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                Text(text = "作为正则表达式")
            }
        }
    }

    IosTopBarScaffold(
        title = stringResource(id = R.string.title_block_list),
        onNavigateBack = { navigator.navigateUp() },
        actions = {
            GlassIconButton(onClick = {
                addBlockCategory = if (pagerState.currentPage == 0) Block.CATEGORY_BLACK_LIST
                else Block.CATEGORY_WHITE_LIST
                dialogState.show()
            }) {
                SfIcon("plus", stringResource(id = R.string.title_add_black), size = 19.dp)
            }
        },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            IosScrollableTabRow(
                items = listOf(
                    0 to stringResource(id = R.string.title_black_list),
                    1 to stringResource(id = R.string.title_white_list),
                ),
                selected = pagerState.currentPage,
                onSelected = { coroutineScope.launch { pagerState.animateScrollToPage(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
                verticalAlignment = Alignment.Top
            ) { position ->
                val items by remember {
                    derivedStateOf { if (position == 0) blackList else whiteList }
                }
                BlockListContent(
                    items = items,
                    isLoading = isLoading,
                    onCopy = { item ->
                        copyBlockKeywords(context, item)
                        Toast.makeText(context, R.string.toast_copy_success, Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { viewModel.send(BlockListUiIntent.Delete(it.id)) },
                )
            }
        }
    }
}
