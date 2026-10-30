package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.ui.glass.GlassButton
import com.huanchengfly.tieba.post.ui.glass.GlassEmphasis
import com.huanchengfly.tieba.post.ui.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.glass.IosFormToggleRow
import com.huanchengfly.tieba.post.ui.glass.IosInputDialog
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.page.settings.IosTopBarScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

/**
 * 屏蔽词管理（对齐 MeiloX 十段均衡器的胶囊按钮切换）：
 * 固定顶栏 + 玻璃胶囊「黑名单 / 白名单」+ 26dp 连续圆角分组列表，新增入口在顶栏右侧。
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
    var showAddDialog by remember { mutableStateOf(false) }
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

    IosTopBarScaffold(
        title = stringResource(id = R.string.title_block_list),
        onNavigateBack = { navigator.navigateUp() },
        actions = {
            GlassIconButton(onClick = {
                addBlockCategory = if (pagerState.currentPage == 0) Block.CATEGORY_BLACK_LIST
                else Block.CATEGORY_WHITE_LIST
                showAddDialog = true
            }) {
                SfIcon("plus", stringResource(id = R.string.title_add_black), size = 19.dp)
            }
        },
    ) { contentPadding ->
        // LocalSnackbarHostState 由 IosTopBarScaffold 提供，只能在壳内读取
        val snackbarHostState = LocalSnackbarHostState.current
        // 弹层的玻璃配色也取自壳内的 LocalGlassColors
        IosInputDialog(
            visible = showAddDialog,
            title = if (addBlockCategory == Block.CATEGORY_WHITE_LIST) {
                stringResource(id = R.string.title_add_white)
            } else {
                stringResource(id = R.string.title_add_black)
            },
            initialValue = "",
            message = stringResource(id = R.string.tip_add_block),
            placeholder = stringResource(id = R.string.title_add_block_hint),
            confirmLabel = stringResource(id = R.string.button_add),
            cancelLabel = stringResource(id = R.string.button_cancel),
            onConfirm = { text ->
                viewModel.send(
                    BlockListUiIntent.Add(
                        category = addBlockCategory,
                        // 避免正则中的空格导致正则失效
                        keywords = if (isRegex) listOf(text) else text.split(" "),
                        isRegex = isRegex
                    )
                )
                isRegex = false
            },
            onDismiss = {
                showAddDialog = false
                isRegex = false
            },
        ) {
            IosFormToggleRow(
                title = stringResource(id = R.string.title_add_regex),
                checked = isRegex,
                onCheckedChange = { isRegex = it },
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // 与 MeiloX 十段均衡器同一套 preset 切换：GlassButton（选中 Prominent），严格单选。
            // 不用 LazyRow：两项直接平铺，选中态随 currentPage 在同一组合内重组。
            val currentPage = pagerState.currentPage
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    0 to R.string.title_black_list,
                    1 to R.string.title_white_list,
                ).forEach { (page, label) ->
                    GlassButton(
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                        },
                        emphasis = if (currentPage == page) {
                            GlassEmphasis.Prominent
                        } else {
                            GlassEmphasis.Regular
                        },
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
                verticalAlignment = Alignment.Top
            ) { position ->
                val items = if (position == 0) blackList else whiteList
                BlockListContent(
                    items = items,
                    isLoading = isLoading,
                    onCopy = { item ->
                        copyBlockKeywords(context, item)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.toast_copy_success)
                            )
                        }
                    },
                    onDelete = { viewModel.send(BlockListUiIntent.Delete(it.id)) },
                )
            }
        }
    }
}
