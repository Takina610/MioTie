package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import dev.chrisbanes.haze.HazeState

/** 按下时圆钮整体放大比例（横向 / 纵向），与导航胶囊一致。 */
private const val DockPressBloomX = 0.05f
private const val DockPressBloomY = 0.09f

@Composable
fun DockSearchButton(
    hazeState: HazeState,
    compactProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val blurSupported = supportsBackdropBlur()
    val compact = compactProgress.coerceIn(0f, 1f)
    val tint = themeColors.bottomBarSurface.copy(
        alpha = if (blurSupported) GlassSurfaceAlphaBlur else GlassSurfaceAlphaFallback
    )
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 900f),
        label = "SearchPress",
    )
    Box(
        modifier = modifier
            .size(lerp(DockNavBarHeight, DockCompactHeight, compact))
            .graphicsLayer {
                scaleX = 1f + DockPressBloomX * press
                scaleY = 1f + DockPressBloomY * press
            },
    ) {
        GlassCapsule(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            hazeState = hazeState,
            tint = tint,
            isNightMode = themeColors.isNightMode,
            blurSupported = blurSupported,
            glowProgress = press,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface,
                    modifier = Modifier.size(lerp(24.dp, 20.dp, compact)),
                )
            }
        }
    }
}
