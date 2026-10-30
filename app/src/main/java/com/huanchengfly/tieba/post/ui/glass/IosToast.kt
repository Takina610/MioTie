package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay

/**
 * iOS 浮层提示（替代 Material Snackbar）：底部居中的玻璃胶囊，2 秒后自动消失。
 * [bottomInset] 由宿主传入「当前底栏可见高度」，让提示停在 tab 栏正上方。
 */
@Composable
fun IosToastHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val data = hostState.currentSnackbarData
    var message by remember { mutableStateOf("") }
    LaunchedEffect(data) {
        if (data != null) {
            message = data.message
            delay(2000)
            data.dismiss()
        }
    }
    AnimatedVisibility(
        visible = data != null,
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = bottomInset + 12.dp),
        enter = fadeIn(tween(160)) +
            slideInVertically(tween(220)) { it / 2 },
        exit = fadeOut(tween(140)) +
            slideOutVertically(tween(180)) { it / 3 },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassSurface(shape = Capsule()) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        message,
                        style = IosTypography.subheadline,
                        color = LocalGlassColors.current.content,
                    )
                }
            }
        }
    }
}
