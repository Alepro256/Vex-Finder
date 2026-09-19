package com.example

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VexCyan
import com.example.ui.theme.VexDarkBackground
import com.example.ui.theme.VexPillTeal
import com.example.ui.theme.VexSurfaceBorder
import com.example.ui.theme.VexSurfaceDark
import com.example.ui.theme.VexTextMuted
import com.example.ui.theme.VexTextPrimary

enum class VexTab(
    val titleRes: Int,
    val icon: ImageVector,
    val url: String,
    val testTag: String
) {
    TEAMS(
        titleRes = R.string.tab_teams,
        icon = Icons.Default.Groups,
        url = "https://altf4.sbs/app/teams/",
        testTag = "tab_equipos"
    ),
    EVENTS(
        titleRes = R.string.tab_events,
        icon = Icons.Default.EmojiEvents,
        url = "https://altf4.sbs/app/events/",
        testTag = "tab_eventos"
    ),
    VEXBOT_V5(
        titleRes = R.string.tab_vexbot_v5,
        icon = Icons.Default.SmartToy,
        url = "https://altf4.sbs/app/vexbot/app_v5.php",
        testTag = "tab_vexbot_v5"
    ),
    VEXBOT_IQ(
        titleRes = R.string.tab_vexbot_iq,
        icon = Icons.Default.Extension,
        url = "https://altf4.sbs/app/vexbot/app_iq.php",
        testTag = "tab_vexbot_iq"
    ),
    ACCOUNT(
        titleRes = R.string.tab_account,
        icon = Icons.Default.AccountCircle,
        url = "https://altf4.sbs/app/account/",
        testTag = "tab_cuenta"
    ),
    ABOUT(
        titleRes = R.string.tab_about,
        icon = Icons.Default.Info,
        url = "https://altf4.sbs/app/about/",
        testTag = "tab_acerca_de"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable global CookieManager acceptance
        CookieManager.getInstance().setAcceptCookie(true)

        setContent {
            MyApplicationTheme {
                VexFinderApp()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }
}

@Composable
fun VexFinderApp() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTab = VexTab.entries[selectedTabIndex]

    // Track state per tab so instances remain intact and keep their scroll/inputs
    val webViews = remember { mutableMapOf<VexTab, WebView>() }
    val loadingStates = remember { mutableStateMapOf<VexTab, Boolean>() }
    val progressStates = remember { mutableStateMapOf<VexTab, Float>() }
    val errorStates = remember { mutableStateMapOf<VexTab, Boolean>() }
    val errorMessages = remember { mutableStateMapOf<VexTab, String?>() }

    // Intercept back button to navigate WebView history or return to Teams tab
    BackHandler {
        val currentWebView = webViews[currentTab]
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else if (selectedTabIndex != 0) {
            selectedTabIndex = 0
        }
    }

    // Flush cookies on lifecycle pause/stop to persist session across app runs
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                CookieManager.getInstance().flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Clean up webviews when composable is destroyed
    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webViews.values.forEach { webView ->
                webView.stopLoading()
                webView.destroy()
            }
            webViews.clear()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vex_finder_scaffold"),
        containerColor = VexDarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            VexBottomBar(
                selectedTab = currentTab,
                onTabSelected = { newTab ->
                    selectedTabIndex = newTab.ordinal
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VexDarkBackground)
        ) {
            // All tabs are maintained concurrently so switching is instantaneous and cookies are shared
            VexTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val hasError = errorStates[tab] ?: false
                val errorMsg = errorMessages[tab]
                val webView = webViews[tab]

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    VexTabWebView(
                        tab = tab,
                        isSelected = isSelected,
                        onWebViewReady = { readyWebView ->
                            webViews[tab] = readyWebView
                        },
                        onLoadingChanged = { isLoading ->
                            loadingStates[tab] = isLoading
                        },
                        onProgressChanged = { progress ->
                            progressStates[tab] = progress
                        },
                        onErrorChanged = { isError, message ->
                            errorStates[tab] = isError
                            errorMessages[tab] = message
                        }
                    )

                    // Error overlay when loading fails
                    if (isSelected && hasError) {
                        VexErrorView(
                            errorMessage = errorMsg,
                            onRetry = {
                                errorStates[tab] = false
                                errorMessages[tab] = null
                                if (webView?.url.isNullOrBlank() || webView?.url == "about:blank") {
                                    webView?.loadUrl(tab.url)
                                } else {
                                    webView?.reload()
                                }
                            }
                        )
                    }
                }
            }

            // Top sleek loading indicator
            val activeLoading = loadingStates[currentTab] ?: false
            val activeProgress = progressStates[currentTab] ?: 0f

            AnimatedVisibility(
                visible = activeLoading && activeProgress < 1.0f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    progress = { activeProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .testTag("top_progress_bar"),
                    color = VexCyan,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VexTabWebView(
    tab: VexTab,
    isSelected: Boolean,
    onWebViewReady: (WebView) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Float) -> Unit,
    onErrorChanged: (Boolean, String?) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .testTag("webview_${tab.name.lowercase()}"),
        factory = {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.parseColor("#070D14"))
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER

                // Global CookieManager for session and cookie sharing across WebViews
                val webViewInstance = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webViewInstance, true)
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val host = uri.host ?: ""

                        // Keep altf4.sbs navigation and auth flows inside the WebView
                        if (host.contains("altf4.sbs") ||
                            host.contains("accounts.google.com") ||
                            host.contains("google.com")
                        ) {
                            return false
                        }

                        // Open external links in default external browser/app
                        return try {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        handler?.proceed()
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChanged(true)
                        onErrorChanged(false, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                        // Flush cookies so session IDs (PHPSESSID) and tokens persist immediately
                        CookieManager.getInstance().flush()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val desc = error?.description?.toString() ?: "Error de red"
                            onErrorChanged(true, desc)
                            onLoadingChanged(false)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress / 100f)
                    }

                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        AlertDialog.Builder(context)
                            .setMessage(message ?: "")
                            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                            .setOnCancelListener { result?.cancel() }
                            .show()
                        return true
                    }

                    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        AlertDialog.Builder(context)
                            .setMessage(message ?: "")
                            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                            .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                            .setOnCancelListener { result?.cancel() }
                            .show()
                        return true
                    }

                    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                        val input = EditText(context).apply { setText(defaultValue ?: "") }
                        AlertDialog.Builder(context)
                            .setMessage(message ?: "")
                            .setView(input)
                            .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm(input.text.toString()) }
                            .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                            .setOnCancelListener { result?.cancel() }
                            .show()
                        return true
                    }
                }

                loadUrl(tab.url)
                onWebViewReady(this)
            }
        },
        update = { webView ->
            webView.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    )
}

@Composable
fun VexBottomBar(
    selectedTab: VexTab,
    onTabSelected: (VexTab) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = VexSurfaceBorder
        )
        NavigationBar(
            containerColor = VexDarkBackground,
            contentColor = VexTextMuted,
            tonalElevation = 0.dp,
            modifier = Modifier.testTag("bottom_navigation_bar")
        ) {
            VexTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    alwaysShowLabel = true,
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.titleRes),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.titleRes),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VexCyan,
                        selectedTextColor = VexCyan,
                        indicatorColor = VexPillTeal,
                        unselectedIconColor = VexTextMuted,
                        unselectedTextColor = VexTextMuted
                    ),
                    modifier = Modifier.testTag(tab.testTag)
                )
            }
        }
    }
}

@Composable
fun VexErrorView(
    onRetry: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VexDarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = VexSurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("error_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = VexCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.error_loading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VexTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.error_check_connection),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VexTextMuted,
                    textAlign = TextAlign.Center
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = VexCyan.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VexCyan,
                        contentColor = VexDarkBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.retry),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun BottomBarPreview() {
    MyApplicationTheme {
        VexBottomBar(
            selectedTab = VexTab.TEAMS,
            onTabSelected = {}
        )
    }
}

