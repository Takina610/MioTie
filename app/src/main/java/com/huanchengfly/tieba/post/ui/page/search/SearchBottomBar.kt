package com.huanchengfly.tieba.post.ui.page.search

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.GlassCapsule
import com.huanchengfly.tieba.post.ui.widgets.compose.glassDockTint
import com.huanchengfly.tieba.post.ui.widgets.compose.supportsBackdropBlur
import com.kyant.backdrop.Backdrop

/** 搜索栏高度（对齐 MeiloX GlasmioSearchToolbar）。 */
internal val SearchBottomBarHeight = 48.dp

/** 搜索栏水平/底部外边距（与悬浮底栏一致）。 */
private val SearchBottomBarMargin = 20.dp

/** 输入胶囊与取消圆钮的间距。 */
private val SearchBottomBarGap = 8.dp

/**
 * 搜索栏底部避让：系统导航栏与 20dp 下限取大；IME 弹出时在输入法高度上再留 20dp。
 */
@Composable
internal fun searchBottomBarBottomInsets(): WindowInsets =
    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
        .union(WindowInsets(bottom = SearchBottomBarMargin))
        .union(WindowInsets.ime.add(WindowInsets(bottom = SearchBottomBarMargin)))
        .only(WindowInsetsSides.Bottom)

/**
 * 底部悬浮搜索栏（对齐 MeiloX GlasmioSearchToolbar）：
 * 48dp 玻璃胶囊（放大镜 + 输入框 + 清除）与独立取消圆钮，随 IME 上浮。
 */
@Composable
internal fun SearchBottomBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    backdrop: Backdrop,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val blurSupported = supportsBackdropBlur()
    val tint = glassDockTint(themeColors.isNightMode, blurSupported)
    val bottomInsets = searchBottomBarBottomInsets()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(bottomInsets)
            .padding(horizontal = SearchBottomBarMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCapsule(
            modifier = Modifier
                .weight(1f)
                .height(SearchBottomBarHeight),
            shape = CircleShape,
            backdrop = backdrop,
            tint = tint,
            isNightMode = themeColors.isNightMode,
            blurSupported = blurSupported,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = themeColors.textSecondary,
                    modifier = Modifier.size(21.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.body1.copy(color = themeColors.text),
                    singleLine = true,
                    cursorBrush = SolidColor(themeColors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.text.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.hint_search),
                                    style = MaterialTheme.typography.body1,
                                    color = themeColors.textSecondary.copy(alpha = ContentAlpha.medium),
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (query.text.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current,
                                role = Role.Button,
                                onClick = onClear,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = stringResource(id = R.string.button_clear),
                            tint = themeColors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(SearchBottomBarGap))
        GlassCapsule(
            modifier = Modifier.size(SearchBottomBarHeight),
            shape = CircleShape,
            backdrop = backdrop,
            tint = tint,
            isNightMode = themeColors.isNightMode,
            blurSupported = blurSupported,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onCancel,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(id = R.string.button_cancel),
                    tint = themeColors.text,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
