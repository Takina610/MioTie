package com.huanchengfly.tieba.post.ui.page.settings.fontsize

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.glass.GlassSlider
import com.huanchengfly.tieba.post.ui.glass.IosGroupedList
import com.huanchengfly.tieba.post.ui.glass.IosTypography
import com.huanchengfly.tieba.post.ui.glass.LocalGlassColors
import com.huanchengfly.tieba.post.ui.glass.SfIcon
import com.huanchengfly.tieba.post.ui.page.settings.IosTopBarScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.utils.appPreferences
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt

private const val FONT_SCALE_STEPS = 10

/** 档位到字号的换算与旧版 SeekBar 一致：0.8f 起、步长 0.05f。 */
private const val FONT_SCALE_MIN = 0.8f
private const val FONT_SCALE_STEP = 0.05f

private val FontSizeLabelRes = mapOf(
    R.string.text_size_small to (0..1),
    R.string.text_size_little_small to (2..3),
    R.string.text_size_default to (4..4),
    R.string.text_size_little_large to (5..6),
    R.string.text_size_large to (7..8),
    R.string.text_size_very_large to (9..10),
)

private fun stepToFontScale(step: Int): Float = FONT_SCALE_MIN + step * FONT_SCALE_STEP

private fun fontScaleToStep(fontScale: Float): Int =
    ((fontScale - FONT_SCALE_MIN) / FONT_SCALE_STEP).roundToInt().coerceIn(0, FONT_SCALE_STEPS)

/** 字号对应的档位名称，供设置入口行作二级说明。 */
@Composable
fun rememberFontScaleLabel(fontScale: Float): String {
    val step = fontScaleToStep(fontScale)
    return stringResource(id = FontSizeLabelRes.entries.first { step in it.value }.key)
}

/**
 * 字体大小设置（对齐 MeiloX 的 iOS 排版 + 液体玻璃滑块）：
 * 大标题下方是可滚动的气泡预览列表（字号随滑块实时变化），滑块固定贴底。
 *
 * 字号写在 Resources 配置里，运行中改不生效，所以改动先在页内保留，离开本页时才写盘并重启。
 */
@Destination
@Composable
fun FontSizePage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val savedScale = remember { context.appPreferences.fontScale }
    var step by remember { mutableFloatStateOf(fontScaleToStep(savedScale).toFloat()) }
    val previewScale = stepToFontScale(step.roundToInt())
    val labelRes = FontSizeLabelRes.entries.first { step.roundToInt() in it.value }.key

    val leavePage: () -> Unit = {
        if (previewScale != savedScale) {
            context.appPreferences.fontScale = previewScale
            restartWithFontScale(context)
        } else {
            navigator.navigateUp()
        }
    }
    MyBackHandler(enabled = true, onBack = leavePage)

    IosTopBarScaffold(
        title = stringResource(id = R.string.title_custom_font_size),
        onNavigateBack = leavePage,
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = contentPadding.calculateBottomPadding()),
        ) {
            // 气泡预览是列表：从大标题下方开始排布，内容多时可滚动
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    PreviewBubble(
                        text = stringResource(id = R.string.bubble_want_change_font_size),
                        fontScale = previewScale,
                        fromSelf = true,
                    )
                }
                item {
                    PreviewBubble(
                        text = stringResource(id = R.string.bubble_change_font_size),
                        fontScale = previewScale,
                        fromSelf = false,
                    )
                }
            }
            IosGroupedList(Modifier.padding(horizontal = 16.dp)) {
                FontSizeSliderRow(
                    step = step,
                    label = stringResource(id = labelRes),
                    onStepChange = { step = it },
                    // 手势结束才吸附到整档，避免拖动途中每帧都改成整档而抖动
                    onStepChangeFinished = { step = it.roundToInt().toFloat() },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
@Composable
private fun PreviewBubble(
    text: String,
    fontScale: Float,
    fromSelf: Boolean,
) {
    val colors = LocalGlassColors.current
    // 对端气泡要用与页面底色拉开明度的半透明色才看得出形状
    val bubbleColor = if (fromSelf) {
        colors.accent
    } else if (colors.isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val textColor = if (fromSelf) {
        // 强调色可能被主题染成浅色，白字会糊在底上，按亮度选前景
        if (colors.accent.luminance() > 0.55f) Color.Black else Color.White
    } else {
        colors.content
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromSelf) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = text,
            style = IosTypography.body.copy(
                fontSize = 15.sp * fontScale,
                lineHeight = 21.sp * fontScale,
            ),
            color = textColor,
            modifier = Modifier
                .clip(ContinuousRoundedRectangle(18.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** 滑块行（对齐 MeiloX `AppearanceLetterSpacing`）：图标 + 标题 + 右侧当前档位 + 液体玻璃滑块。 */
@Composable
private fun FontSizeSliderRow(
    step: Float,
    label: String,
    onStepChange: (Float) -> Unit,
    onStepChangeFinished: (Float) -> Unit,
) {
    val colors = LocalGlassColors.current
    val title = stringResource(id = R.string.title_custom_font_size)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SfIcon("textformat.size", null, size = 23.dp)
            Text(
                text = title,
                style = IosTypography.body,
                color = colors.content,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            Text(
                text = label,
                style = IosTypography.subheadline,
                color = colors.secondaryContent,
            )
        }
        GlassSlider(
            value = step,
            onValueChange = onStepChange,
            onValueChangeFinished = onStepChangeFinished,
            valueRange = 0f..FONT_SCALE_STEPS.toFloat(),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .semantics {
                    contentDescription = title
                    stateDescription = label
                },
        )
    }
}

/**
 * 销毁所有界面后用新字号重新拉起入口：新建 Activity 才会以新密度/字号初始化 Resources。
 */
private fun restartWithFontScale(context: Context) {
    context.toastShort(R.string.toast_after_change_will_restart)
    App.INSTANCE.removeAllActivity()
    context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
        context.startActivity(it)
    }
}
