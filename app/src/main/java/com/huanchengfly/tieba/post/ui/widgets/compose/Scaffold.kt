package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissValue
import androidx.compose.material.DrawerDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberDismissState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.ThemeUtil

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeToDismissSnackbarHost(hostState: SnackbarHostState) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value != DismissValue.Default) {
                hostState.currentSnackbarData?.dismiss()
                true
            } else {
                false
            }
        }
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != DismissValue.Default) {
            dismissState.reset()
        }
    }
    SwipeToDismiss(state = dismissState, background = {}) {
        SnackbarHost(hostState = hostState)
    }
}

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("no scaffold here!") }

/**
 * 页面级不透明背景：与窗口根背景同一绘制来源（主题底色 + 半透明主题背景图）。
 * 页面列表在转场中会同时叠放，若页面背景透明，新页就遮不住旧页，
 * 旧页内容会残留到转场结束整层被移除，表现为「闪一下消失」。
 */
@Composable
fun PageBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(ExtendedTheme.colors.background)) {
        if (ThemeUtil.isTranslucentTheme(ExtendedTheme.colors.theme)) {
            val backgroundPath by rememberPreferenceAsMutableState(
                key = stringPreferencesKey("translucent_theme_background_path"),
                defaultValue = ""
            )
            val backgroundUri by remember { derivedStateOf { newFileUri(backgroundPath) } }
            AsyncImage(
                imageUri = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * 导航转场时按屏幕圆角裁出页面左前缘，对齐 miuix `NavDisplayTransitionEffects` 的 scene clip。
 * 页面在转场中生命周期低于 RESUMED，转场结束回到 RESUMED 后恢复直角。
 * 下层页面的左缘在整段动画里都在屏幕外，因此每个页面都裁与 miuix 只裁上层场景视觉等效。
 */
@Composable
private fun rememberNavTransitionClip(): Modifier {
    val entry = LocalViewModelStoreOwner.current as? NavBackStackEntry ?: return Modifier
    var inTransition by remember(entry) {
        mutableStateOf(entry.lifecycle.currentState != Lifecycle.State.RESUMED)
    }
    DisposableEffect(entry) {
        val observer = LifecycleEventObserver { _, _ ->
            inTransition = entry.lifecycle.currentState != Lifecycle.State.RESUMED
        }
        entry.lifecycle.addObserver(observer)
        onDispose { entry.lifecycle.removeObserver(observer) }
    }
    if (!inTransition) return Modifier
    val corner = rememberDisplayCornerRadius()
    if (corner <= 0.dp) return Modifier
    return Modifier.clip(RoundedCornerShape(topStart = corner, bottomStart = corner))
}

@Composable
private fun rememberDisplayCornerRadius(): Dp {
    val context = LocalContext.current
    val insets = LocalView.current.rootWindowInsets
    val inMultiWindow = LocalActivity.current?.isInMultiWindowMode == true
    val radiusPx = remember(context, insets, inMultiWindow) {
        when {
            inMultiWindow -> 0
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                insets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius
                    ?.takeIf { it > 0 }
                    ?: context.roundedCornerRadiusBottom()
            else -> 0
        }
    }
    return with(LocalDensity.current) { radiusPx.toDp() }
}

private fun Context.roundedCornerRadiusBottom(): Int {
    val id = resources.getIdentifier("rounded_corner_radius_bottom", "dimen", "android")
    return if (id > 0) resources.getDimensionPixelSize(id) else 0
}

@Composable
fun MyScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SwipeToDismissSnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    CompositionLocalProvider(LocalSnackbarHostState provides scaffoldState.snackbarHostState) {
        Box(rememberNavTransitionClip()) {
            if (backgroundColor == Color.Transparent) {
                PageBackground(Modifier.matchParentSize())
            }
            Scaffold(
                modifier,
                scaffoldState,
                topBar,
                bottomBar,
                snackbarHost,
                floatingActionButton,
                floatingActionButtonPosition,
                isFloatingActionButtonDocked,
                drawerContent,
                drawerGesturesEnabled,
                drawerShape,
                drawerElevation,
                drawerBackgroundColor,
                drawerContentColor,
                drawerScrimColor,
                backgroundColor,
                contentColor,
                content
            )
        }
    }
}