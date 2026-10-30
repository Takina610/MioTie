package com.huanchengfly.tieba.post.ui.page.search

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.glass.IosBottomSearchToolbar
import com.huanchengfly.tieba.post.ui.glass.IosSearchBottomMargin
import com.huanchengfly.tieba.post.ui.glass.IosSearchContentBottomPadding
import com.huanchengfly.tieba.post.ui.glass.IosSearchHorizontalMargin
import com.huanchengfly.tieba.post.ui.glass.LocalGlassBackdrop
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.recordBackdropSource
import com.huanchengfly.tieba.post.ui.glass.rememberRecordedBackdrop
import com.huanchengfly.tieba.post.ui.glass.defaultGlassColors
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadUiEvent
import com.huanchengfly.tieba.post.ui.page.search.user.SearchUserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarContainer
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@Immutable
data class SearchPageItem(
    val id: String,
    val text: @Composable (selected: Boolean) -> Unit,
    val content: @Composable () -> Unit,
    val supportSort: Boolean = false,
    val sortTypes: ImmutableMap<String, Int> = persistentMapOf(),
    val selectedSortType: () -> Int = { -1 },
    val onSelectedSortTypeChange: (Int) -> Unit = {},
)

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://search")
    ]
)
@Composable
fun SearchPage(
    navigator: DestinationsNavigator,
    viewModel: SearchViewModel = pageViewModel<SearchUiIntent, SearchViewModel>(
        listOf(SearchUiIntent.Init)
    ),
    autoFocus: Boolean = false,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val searchHistories by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::searchHistories,
        initial = persistentListOf()
    )
    val keyword by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::keyword,
        initial = ""
    )

    val isKeywordEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::isKeywordEmpty,
        initial = true
    )
    val suggestions by viewModel.uiState.collectPartialAsState(
        prop1 = SearchUiState::suggestions,
        initial = persistentListOf()
    )

    val showSuggestions by remember {
        derivedStateOf { suggestions.isNotEmpty() }
    }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    // 只在从底栏搜索钮进入时聚焦；从首页搜索框/探索/消息等入口进入保持不聚焦（对齐 MeiloX）
    var autoFocusPending by rememberSaveable { mutableStateOf(autoFocus) }
    LaunchedEffect(Unit) {
        if (autoFocusPending) {
            autoFocusPending = false
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { query.text }
            .debounce(500)
            .collect {
                viewModel.send(SearchUiIntent.KeywordInputChanged(it))
            }
    }

    MyBackHandler(
        enabled = !isKeywordEmpty,
        currentScreen = SearchPageDestination
    ) {
        focusManager.clearFocus()
        viewModel.send(SearchUiIntent.SubmitKeyword(""))
    }

    var expanded by remember { mutableStateOf(false) }

    val initialSortType = remember { SearchThreadSortType.SORT_TYPE_NEWEST }
    var searchThreadSortType by remember { mutableIntStateOf(initialSortType) }
    LaunchedEffect(searchThreadSortType) {
        emitGlobalEvent(SearchThreadUiEvent.SwitchSortType(searchThreadSortType))
    }
    viewModel.onEvent<SearchUiEvent.KeywordChanged> {
        if (query.text != it.keyword) {
            query = TextFieldValue(it.keyword, TextRange(it.keyword.length))
        }
        if (it.keyword.isNotBlank()) emitGlobalEventSuspend(it)
    }

    val submitKeyword: (String) -> Unit = { rawKeyword ->
        val newKeyword = rawKeyword.trim()
        if (newKeyword.isNotEmpty()) {
            query = TextFieldValue(newKeyword, TextRange(newKeyword.length))
            focusManager.clearFocus()
            keyboardController?.hide()
            viewModel.send(SearchUiIntent.SubmitKeyword(newKeyword))
        }
    }
    // 清除按钮只清空输入并回到落地态，不额外抢焦点（对齐 MeiloX 搜索工具栏）
    val onQueryChange: (TextFieldValue) -> Unit = { value ->
        query = value
        if (value.text.isEmpty() && !isKeywordEmpty) {
            viewModel.send(SearchUiIntent.SubmitKeyword(""))
        }
    }
    val cancelSearch: () -> Unit = {
        query = TextFieldValue()
        focusManager.clearFocus()
        keyboardController?.hide()
        if (!isKeywordEmpty) {
            viewModel.send(SearchUiIntent.SubmitKeyword(""))
        }
        navigator.navigateUp()
    }

    val pages by remember {
        derivedStateOf {
            persistentListOf(
                SearchPageItem(
                    id = "forum",
                    text = {
                        Text(
                            text = stringResource(id = R.string.title_search_forum),
                            fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    content = { SearchForumPage(keyword = keyword) }
                ),
                SearchPageItem(
                    id = "thread",
                    text = {
                        Text(
                            text = stringResource(id = R.string.title_search_thread),
                            fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    content = {
                        SearchThreadPage(
                            keyword = keyword,
                            initialSortType = initialSortType
                        )
                    },
                    supportSort = true,
                    sortTypes = persistentMapOf(
                        context.getString(R.string.title_search_order_new) to SearchThreadSortType.SORT_TYPE_NEWEST,
                        context.getString(R.string.title_search_order_old) to SearchThreadSortType.SORT_TYPE_OLDEST,
                        context.getString(R.string.title_search_order_relevant) to SearchThreadSortType.SORT_TYPE_RELATIVE,
                    ),
                    selectedSortType = { searchThreadSortType },
                    onSelectedSortTypeChange = { searchThreadSortType = it }
                ),
                SearchPageItem(
                    id = "user",
                    text = {
                        Text(
                            text = stringResource(id = R.string.title_search_user),
                            fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    content = { SearchUserPage(keyword = keyword) }
                ),
            )
        }
    }
    val pagerState = rememberPagerState { pages.size }
    val searchBackdrop = rememberRecordedBackdrop()
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    // 对齐 MeiloX 搜索页：底部避让取 IME 与系统栏的较大值
    val searchBottomInset = maxOf(
        with(density) { WindowInsets.ime.getBottom(this).toDp() },
        WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
    )
    val themeColors = ExtendedTheme.colors
    val accentColor = MaterialTheme.colors.secondary
    val glassColors = remember(themeColors.isNightMode, accentColor) {
        defaultGlassColors(
            isDark = themeColors.isNightMode,
            accent = accentColor,
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .recordBackdropSource(searchBackdrop)
        ) {
            MyScaffold(
                topBar = {
                    if (!isKeywordEmpty) {
                        TopAppBarContainer(topBar = {}) {
                            SearchTabRow(pagerState = pagerState, pages = pages)
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            PaddingValues(
                                start = paddingValues.calculateStartPadding(layoutDirection),
                                end = paddingValues.calculateEndPadding(layoutDirection),
                            )
                        )
                        // 落地态没有顶部栏：自行避让状态栏（M2 Scaffold 的空 topBar 在此路径只给 0）
                        .then(if (isKeywordEmpty) Modifier.statusBarsPadding() else Modifier)
                        // 底部预留搜索工具栏位置，并随 IME 上浮（对齐 MeiloX 搜索页）
                        .padding(bottom = searchBottomInset + IosSearchContentBottomPadding)
                ) {
                    if (!isKeywordEmpty) {
                        ProvideNavigator(navigator = navigator) {
                            LazyLoadHorizontalPager(
                                state = pagerState,
                                key = { pages[it].id },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                pages[it].content()
                            }
                        }
                    } else {
                        if (showSuggestions) {
                            SearchSuggestionList(
                                suggestions = suggestions,
                                onItemClick = { submitKeyword(it) }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Container {
                                    SearchHistoryList(
                                        searchHistories = searchHistories,
                                        onSearchHistoryClick = {
                                            submitKeyword(it.content)
                                        },
                                        expanded = expanded,
                                        onToggleExpand = { expanded = !expanded },
                                        onDelete = { viewModel.send(SearchUiIntent.DeleteSearchHistory(it.id)) },
                                        onClear = { viewModel.send(SearchUiIntent.ClearSearchHistory) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        val toolbarModifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = IosSearchHorizontalMargin)
            .padding(bottom = searchBottomInset + IosSearchBottomMargin)
        CompositionLocalProvider(LocalGlassColors provides glassColors) {
            IosBottomSearchToolbar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { submitKeyword(it) },
                onCancel = cancelSearch,
                cancelLabel = stringResource(id = R.string.button_cancel),
                placeholder = stringResource(id = R.string.title_search),
                modifier = toolbarModifier,
                backdrop = searchBackdrop,
                focusRequester = focusRequester,
                autoFocus = false,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchSuggestionList(
    suggestions: ImmutableList<String>,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = suggestions,
            key = { it }
        ) {
            Container(
                modifier = Modifier.animateItem()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .debounceClickable(onClick =  {
                            onItemClick(it)
                        })
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.desc_search_sug, it),
                        tint = ExtendedTheme.colors.text
                    )

                    Text(
                        text = it,
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.weight(1f),
                        color = ExtendedTheme.colors.text
                    )
                }
            }
        }
    }
}

@Preview("SearchSuggestionList", backgroundColor = 0xFFFFFFFF)
@Composable
private fun SearchSuggestionListPreview() {
    TiebaLiteTheme {
        Box(
            modifier = Modifier.background(ExtendedTheme.colors.topBar)
        ) {
            SearchSuggestionList(
                suggestions = persistentListOf("1", "2", "3"),
                onItemClick = {}
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.SearchTabRow(
    pagerState: PagerState,
    pages: ImmutableList<SearchPageItem>,
) {
    val coroutineScope = rememberCoroutineScope()
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions ->
            PagerTabIndicator(
                pagerState = pagerState,
                tabPositions = tabPositions
            )
        },
        divider = {},
        backgroundColor = Color.Transparent,
        contentColor = ExtendedTheme.colors.onTopBar,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .width(76.dp * pages.size),
    ) {
        pages.fastForEachIndexed { index, item ->
            val tabTextStyle =
                MaterialTheme.typography.button.copy(fontSize = 13.sp, letterSpacing = 0.sp)

            if (item.supportSort) {
                TabClickMenu(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        ProvideTextStyle(value = tabTextStyle) {
                            item.text(pagerState.currentPage == index)
                        }
                    },
                    menuContent = {
                        ListSinglePicker(
                            itemTitles = item.sortTypes.keys.toImmutableList(),
                            itemValues = item.sortTypes.values.toImmutableList(),
                            selectedPosition = item.sortTypes.values.indexOf(item.selectedSortType()),
                            onItemSelected = { _, _, value, changed ->
                                if (changed) item.onSelectedSortTypeChange(value)
                                dismiss()
                            }
                        )
                    }
                )
            } else {
                Tab(
                    text = {
                        ProvideTextStyle(value = tabTextStyle) {
                            item.text(pagerState.currentPage == index)
                        }
                    },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SearchHistoryList(
    searchHistories: ImmutableList<SearchHistory>,
    onSearchHistoryClick: (SearchHistory) -> Unit,
    expanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onDelete: (SearchHistory) -> Unit = {},
    onClear: () -> Unit = {},
) {
    val hasItem = remember(searchHistories) {
        searchHistories.isNotEmpty()
    }
    val hasMore = remember(searchHistories) {
        searchHistories.size > 6
    }
    val showItem = remember(expanded, hasMore, searchHistories) {
        if (!expanded && hasMore) searchHistories.take(6) else searchHistories
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.title_search_history),
                modifier = Modifier
                    .weight(1f),
                style = MaterialTheme.typography.subtitle1
            )
            if (hasItem) {
                Text(
                    text = stringResource(id = R.string.button_clear_all),
                    modifier = Modifier.debounceClickable(onClick = onClear),
                    style = MaterialTheme.typography.button
                )
            }
        }
        FlowRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            showItem.fastForEach { searchHistory ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(100))
                        .combinedClickable(
                            onClick = { onSearchHistoryClick(searchHistory) },
                            onLongClick = { onDelete(searchHistory) }
                        )
                        .background(ExtendedTheme.colors.chip)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = searchHistory.content
                    )
                }
            }
        }
        if (hasMore) {
            Button(
                onClick = onToggleExpand,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = ExtendedTheme.colors.text
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(
                            id = if (expanded) R.string.button_expand_less_history else R.string.button_expand_more_history
                        ),
                        style = MaterialTheme.typography.button,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (!hasItem) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.tip_empty),
                    color = ExtendedTheme.colors.textDisabled,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview("SearchHistoryList")
@Composable
fun PreviewSearchHistoryList() {
    TiebaLiteTheme {
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.background(ExtendedTheme.colors.background)) {
            SearchHistoryList(
                searchHistories = (0..20).map {
                    SearchHistory(content = if (it % 2 == 0) "记录$it" else "搜索记录$it")
                }.toImmutableList(),
                onSearchHistoryClick = {},
                expanded = expanded,
                onToggleExpand = { expanded = !expanded },
            )
        }
    }
}