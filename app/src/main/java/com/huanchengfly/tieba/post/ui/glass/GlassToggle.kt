package com.huanchengfly.tieba.post.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.huanchengfly.tieba.post.ui.liquidglass.DampedDragAnimation
import kotlinx.coroutines.flow.collectLatest

/**
 * iOS 27 开关（照抄 MeiloX 1.55.1 `GlassToggle`）：
 * 64×28dp 轨道 + 40×24dp 可拖动、按压会放大的液态玻璃滑块。
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backdrop: Backdrop = LocalGlassBackdrop.current,
) {
    val colors = LocalGlassColors.current
    val appearance = LocalGlassAppearance.current
    val light = !colors.isDark
    val accent = colors.accent
    val track = (if (light) {
        Color(0xFF787878).copy(alpha = 0.20f)
    } else {
        Color(0xFF787880).copy(alpha = 0.36f)
    }).let {
        if (appearance.reduceTransparency) it.compositeOver(colors.elevatedBackground) else it
    }
    val density = LocalDensity.current
    val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val travelPx = with(density) { 20.dp.toPx() }
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }

    val animation = remember(scope, enabled) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (!currentEnabled) return@DampedDragAnimation
                fraction = if (didDrag) {
                    if (targetValue >= 0.5f) 1f else 0f
                } else if (currentChecked) 0f else 1f
                didDrag = false
                currentOnCheckedChange(fraction == 1f)
            },
            onDrag = { _, dragAmount ->
                if (!currentEnabled) return@DampedDragAnimation
                didDrag = didDrag || dragAmount.x != 0f
                val delta = dragAmount.x / travelPx
                fraction = if (ltr) (fraction + delta).fastCoerceIn(0f, 1f)
                else (fraction - delta).fastCoerceIn(0f, 1f)
            },
            // 开关自己认领手势，外层「整行可点」的行才不会再响应同一次点击
            consumeGestures = true,
        )
    }
    LaunchedEffect(animation) {
        snapshotFlow { fraction }.collectLatest(animation::updateValue)
    }
    LaunchedEffect(checked, enabled, animation) {
        didDrag = false
        val target = if (checked) 1f else 0f
        if (target != fraction) {
            fraction = target
            animation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val transformedTrackBackdrop = rememberBackdrop(
        backdrop = trackBackdrop,
        onDraw = remember(animation) {
            { draw ->
                val p = animation.pressProgress
                scale(lerp(2f / 3f, 0.75f, p), lerp(0f, 0.75f, p)) { draw() }
            }
        },
    )
    val thumbBackdrop = rememberCombinedBackdrop(backdrop, transformedTrackBackdrop)
    val thumbGlassModifier = remember(thumbBackdrop, animation, enabled, appearance) {
        Modifier.drawBackdrop(
            backdrop = if (appearance.usesBackdrop) thumbBackdrop else emptyBackdrop(),
            shape = { Capsule() },
            effects = {
                if (appearance.usesBackdrop) {
                    val p = animation.pressProgress
                    blur(8.dp.toPx() * if (appearance.usesRefraction) 1f - p else 1f)
                    if (appearance.usesRefraction) {
                        lens(5.dp.toPx() * p, 10.dp.toPx() * p, chromaticAberration = true)
                    }
                }
            },
            highlight = {
                Highlight.Ambient.copy(
                    width = Highlight.Ambient.width / 1.5f,
                    blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                    alpha = animation.pressProgress,
                )
            },
            shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.05f)) },
            innerShadow = {
                InnerShadow(
                    radius = 4.dp * animation.pressProgress,
                    alpha = animation.pressProgress,
                )
            },
            layerBlock = {
                scaleX = animation.scaleX
                scaleY = animation.scaleY
                val velocity = animation.velocity / 50f
                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                alpha = if (enabled) 1f else 0.45f
            },
            onDrawSurface = {
                val alpha = if (appearance.reduceTransparency) 1f else 1f - animation.pressProgress
                drawRect(Color.White.copy(alpha = alpha))
            },
        )
    }
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 28.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (!enabled) disabled()
                onClick {
                    if (currentEnabled) currentOnCheckedChange(!currentChecked)
                    currentEnabled
                }
            }
            .then(if (enabled) animation.modifier else Modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .then(if (appearance.usesBackdrop) Modifier.layerBackdrop(trackBackdrop) else Modifier)
                .clip(Capsule())
                .drawBehind { drawRect(colorLerp(track, accent, animation.value)) }
                .size(width = 64.dp, height = 28.dp),
        )
        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = if (ltr) lerp(padding, padding + travelPx, animation.value)
                    else lerp(-padding, -(padding + travelPx), animation.value)
                }
                .then(thumbGlassModifier)
                .size(width = 40.dp, height = 24.dp),
        )
    }
}
