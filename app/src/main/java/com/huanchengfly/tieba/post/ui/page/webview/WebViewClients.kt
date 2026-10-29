package com.huanchengfly.tieba.post.ui.page.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.dialogs.PermissionDialog
import com.huanchengfly.tieba.post.models.PermissionBean
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebChromeClient
import com.huanchengfly.tieba.post.ui.widgets.compose.AccompanistWebViewClient
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.WebView
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSaveableWebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberWebViewNavigator
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DialogUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PermissionUtils.PermissionData
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.compose.launchActivityForResult
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** 网页视图的客户端与域名判定（与页面 UI 分离）。 */
fun isTiebaHost(host: String): Boolean {
    return host == "wapp.baidu.com" ||
            host.contains("tieba.baidu.com") ||
            host == "tiebac.baidu.com"
}

fun isInternalHost(host: String): Boolean {
    return isTiebaHost(host) ||
            host.contains("wappass.baidu.com") ||
            host.contains("ufosdk.baidu.com") ||
            host.contains("m.help.baidu.com")
}

open class MyWebViewClient(
    protected val nativeNavigator: DestinationsNavigator? = null,
) : AccompanistWebViewClient() {
    val context: Context
        get() = state.webView?.context ?: App.INSTANCE

    private fun interceptWebViewRequest(
        webView: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val newUri = request.url
        val scheme = newUri.scheme?.lowercase() ?: return false
        val host = newUri.host?.lowercase() ?: return false
        val path = newUri.path?.lowercase() ?: return false
        val isHttp = scheme.startsWith("http")
        val isTieba = host == "wapp.baidu.com" ||
                host.contains("tieba.baidu.com") ||
                host == "tiebac.baidu.com"
        val isInternal = isTieba ||
                host.contains("wappass.baidu.com") ||
                host.contains("ufosdk.baidu.com") ||
                host.contains("m.help.baidu.com")
        return when {
            isHttp && isTieba -> {
                if (path == "/f" || path == "/mo/q/m") {
                    val forumName =
                        newUri.getQueryParameter("kw") ?: newUri.getQueryParameter("word")
                    val threadId = newUri.getQueryParameter("kz")?.toLongOrNull()
                    if (threadId != null) {
                        nativeNavigator?.navigate(
                            ThreadPageDestination(threadId)
                        )
                        true
                    } else if (forumName != null) {
                        nativeNavigator?.navigate(
                            ForumPageDestination(forumName)
                        )
                        true
                    } else false
                } else if (path.startsWith("/p/")) {
                    val threadId = path.substring(3).toLongOrNull()
                    if (threadId != null) {
                        nativeNavigator?.navigate(
                            ThreadPageDestination(threadId)
                        )
                        true
                    } else false
                } else false
            }

            isHttp && !isInternal -> {
                if (context.appPreferences.useWebView)
                    false
                else {
                    if (context.appPreferences.useCustomTabs) {
                        runCatching {
                            CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .setDefaultColorSchemeParams(
                                    CustomTabColorSchemeParams.Builder()
                                        .setToolbarColor(
                                            ThemeUtils.getColorByAttr(
                                                context,
                                                R.attr.colorToolbar
                                            )
                                        )
                                        .setNavigationBarColor(
                                            ThemeUtils.getColorByAttr(
                                                context,
                                                R.attr.colorNavBar
                                            )
                                        )
                                        .setNavigationBarDividerColor(
                                            ThemeUtils.getColorByAttr(
                                                context,
                                                R.attr.colorDivider
                                            )
                                        )
                                        .build()
                                )
                                .build()
                                .launchUrl(context, newUri)
                        }.onFailure {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    newUri
                                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            )
                        }
                    } else {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                newUri
                            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                    }
                    true
                }
            }

            !isHttp -> {
                val currentUri = webView.url?.toUri()
                val currentHost = currentUri?.host
                if (currentHost != null) {
                    launchThirdPartyApp(newUri, currentHost)
                }
                true
            }

            else -> false
        }
    }

    private fun launchThirdPartyApp(
        intent: Intent,
        host: String,
    ) {
        val resolves = context.packageManager.queryIntentActivities(intent, 0)
        if (resolves.isEmpty()) return
        val appName = if (resolves.size == 1) {
            resolves[0].loadLabel(context.packageManager)
        } else {
            context.getString(R.string.name_multi_app)
        }
        val scheme = intent.scheme ?: ""
        PermissionDialog(
            context,
            PermissionBean(
                PermissionDialog.CustomPermission.PERMISSION_START_APP,
                "${host}_${scheme}",
                context.getString(R.string.title_start_app_permission, host, appName),
                R.drawable.ic_round_exit_to_app
            )
        )
            .setOnGrantedCallback { context.startActivity(intent) }
            .show()
    }

    private fun launchThirdPartyApp(
        uri: Uri,
        host: String,
    ) {
        if (uri.scheme.equals("intent", ignoreCase = true)) {
            launchThirdPartyApp(
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    .addCategory(Intent.CATEGORY_BROWSABLE), host
            )
        } else {
            launchThirdPartyApp(
                Intent(
                    Intent.ACTION_VIEW,
                    uri
                ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), host
            )
        }
    }

    open fun injectCookies(url: String) {
        val cookieStr = CookieManager.getInstance().getCookie(url) ?: ""
        val cookies = AccountUtil.parseCookie(cookieStr)
        val BDUSS = cookies["BDUSS"]
        val currentAccountBDUSS = AccountUtil.getBduss()
        if (currentAccountBDUSS != null && BDUSS != currentAccountBDUSS) {
            CookieManager.getInstance()
                .setCookie(url, AccountUtil.getBdussCookie(currentAccountBDUSS))
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        injectCookies(url ?: "")
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest?): Boolean {
        if (request == null) return false
        return interceptWebViewRequest(view, request)
    }
}
