package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * iOS 表单弹窗（照搬 Glasmio `GlasmioAlertDialog` + `GlasmioAlertTextField`）：
 * 居中 300dp 玻璃卡片、26dp 圆角输入组、底部并排 48dp 胶囊按钮。
 */
@Composable
fun IosInputDialog(
    visible: Boolean,
    title: String,
    initialValue: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    placeholder: String? = null,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    if (!visible) return
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val backdrop = rememberCrossWindowBackdrop(LocalGlassBackdrop.current)
    var value by remember(visible) { mutableStateOf(TextFieldValue(initialValue)) }
    val focusRequester = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (colors.isDark) 0.2f else 0.08f))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onDismiss,
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(
                modifier
                    .padding(horizontal = 16.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                    ) { }
            ) {
                val alertWidth = 300.dp.coerceAtMost(maxWidth)
                Column(
                    Modifier
                        .width(alertWidth)
                        .then(
                            Modifier.navigationGlassBackground(
                                backdrop = backdrop,
                                shape = { ContinuousRoundedRectangle(34.dp) },
                                containerColor = colors.container,
                                sampleBackdrop = appearance.usesBackdrop,
                                appearance = appearance,
                            )
                        )
                        .padding(14.dp)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.content,
                        LocalGlassContentColor provides colors.content,
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(title, style = IosTypography.headline, color = colors.content)
                            message?.let {
                                Text(it, style = IosTypography.body, color = colors.content)
                            }
                        }
                        IosFieldGroup(Modifier.padding(bottom = 14.dp)) {
                            BasicTextField(
                                value = value,
                                onValueChange = { value = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(focusRequester),
                                textStyle = IosTypography.body.copy(color = colors.content),
                                cursorBrush = SolidColor(colors.accent),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (value.text.isNotBlank()) {
                                            onConfirm(value.text)
                                            onDismiss()
                                        }
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                                        if (value.text.isEmpty() && placeholder != null) {
                                            Text(
                                                placeholder,
                                                style = IosTypography.body,
                                                color = colors.tertiaryContent,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )
                        }
                        extraContent()
                        Row(
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IosSheetActionButton(
                                text = cancelLabel,
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                            )
                            IosSheetActionButton(
                                text = confirmLabel,
                                onClick = {
                                    if (value.text.isNotBlank()) {
                                        onConfirm(value.text)
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                prominent = true,
                                enabled = value.text.isNotBlank(),
                            )
                        }
                    }
                }
            }
        }
        LaunchedEffect(visible) {
            focusRequester.requestFocus()
        }
    }
}

/** 表单里的一行开关（如「作为正则表达式」）。 */
@Composable
fun IosFormToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = IosTypography.body,
            color = LocalGlassColors.current.content,
            modifier = Modifier.weight(1f),
        )
        GlassToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}
