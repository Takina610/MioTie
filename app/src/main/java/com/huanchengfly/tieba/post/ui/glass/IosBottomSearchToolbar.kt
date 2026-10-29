package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule

/** 搜索工具栏宿主高度（对齐 MeiloX 1.55.1 IosBottomSearchToolbar 的 64dp 行高）。 */
internal val IosSearchToolbarHeight = 64.dp

/** 搜索输入胶囊/取消圆钮直径（对齐 MeiloX 1.55.1 的 56dp）。 */
private val IosSearchControlHeight = 56.dp

/** 工具栏水平外边距（对齐 MeiloX 1.55.1 的 bottomNavigationHorizontalMargin = 20dp）。 */
internal val IosSearchHorizontalMargin = 20.dp

/** 工具栏距屏幕底部（不含系统栏/IME）的边距（对齐 MeiloX NavigationBarBottomMargin = 8dp）。 */
internal val IosSearchBottomMargin = 8.dp

/**
 * 搜索页内容底部预留（对齐 MeiloX 1.55.1 搜索页的 NavigationBarHeight = 80dp）。
 * 工具栏本体占 64dp + 8dp，内容再多留一点呼吸位。
 */
internal val IosSearchContentBottomPadding = 80.dp

/** Figma node 2517:14528. Floating bottom search toolbar variants share the liquid capsule. */
@Composable
fun IosBottomSearchToolbar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onCancel: () -> Unit,
    cancelLabel: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: GlassSurfaceStyle = GlassSurfaceStyle.Navigation,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    focusRequester: FocusRequester = remember { FocusRequester() },
    autoFocus: Boolean = true,
) {
    val colors = LocalGlassColors.current
    Row(
        modifier.height(IosSearchToolbarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlassSurface(
            modifier = Modifier.weight(1f).height(IosSearchControlHeight),
            backdrop = backdrop,
            shape = Capsule(),
            style = style,
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SfIcon(SfSymbol.Search, null, size = 21.dp, tint = colors.secondaryContent)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = IosTypography.body.copy(color = colors.content),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
                    decorationBox = { inner ->
                        if (query.text.isEmpty()) {
                            Text(placeholder, style = IosTypography.body, color = colors.tertiaryContent)
                        }
                        inner()
                    },
                )
                if (query.text.isNotEmpty()) {
                    Box(
                        Modifier.size(32.dp).clickable { onQueryChange(TextFieldValue()) },
                        contentAlignment = Alignment.Center,
                    ) {
                        SfIcon(SfSymbol.CloseCircle, null, size = 18.dp, tint = colors.secondaryContent)
                    }
                }
            }
        }
        GlassIconButton(
            onClick = onCancel,
            modifier = Modifier.size(IosSearchControlHeight),
            backdrop = backdrop,
            style = style,
        ) {
            SfIcon(SfSymbol.Close, cancelLabel, size = 20.dp, tint = colors.content)
        }
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
}
