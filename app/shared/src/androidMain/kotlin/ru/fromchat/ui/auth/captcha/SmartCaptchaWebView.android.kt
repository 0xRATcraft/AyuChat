package ru.fromchat.ui.auth.captcha

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.fromchat.Logger

private const val SMARTCAPTCHA_WEBVIEW_BASE = "https://smartcaptcha.cloud.yandex.ru/webview"

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun SmartCaptchaWebView(
    sitekey: String,
    languageTag: String,
    modifier: Modifier,
    onToken: (String) -> Unit,
    onReady: () -> Unit,
    onChallengeVisible: () -> Unit,
    onChallengeHidden: () -> Unit,
    onError: (String) -> Unit,
) {
    val onTokenState = rememberUpdatedState(onToken)
    val onReadyState = rememberUpdatedState(onReady)
    val onChallengeVisibleState = rememberUpdatedState(onChallengeVisible)
    val onChallengeHiddenState = rememberUpdatedState(onChallengeHidden)
    val onErrorState = rememberUpdatedState(onError)
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundArgb = MaterialTheme.colorScheme.surfaceContainer.toArgb()
    var webView by remember { mutableStateOf<WebView?>(null) }
    val instanceId = remember { Integer.toHexString(System.identityHashCode(Any())) }

    val lang = languageTag.substringBefore('-').lowercase().ifBlank { "en" }
    val captchaUrl = remember(sitekey, lang) {
        "$SMARTCAPTCHA_WEBVIEW_BASE?sitekey=${sitekey.trim()}&hl=$lang"
    }

    DisposableEffect(instanceId) {
        Logger.i(
            SmartCaptchaLog.TAG,
            "WebView compose enter id=$instanceId sitekey=${SmartCaptchaLog.redactKey(sitekey)} " +
                "lang=$lang languageTag=$languageTag url=${SmartCaptchaLog.shortUrl(captchaUrl)}",
        )
        onDispose {
            Logger.i(SmartCaptchaLog.TAG, "WebView compose dispose id=$instanceId")
        }
    }

    val bridge = remember {
        val mainHandler = Handler(Looper.getMainLooper())
        object {
            @JavascriptInterface
            fun onGetToken(token: String) {
                val cleaned = token.trim()
                Logger.i(
                    SmartCaptchaLog.TAG,
                    "JS onGetToken id=$instanceId ${SmartCaptchaLog.redactToken(cleaned)}",
                )
                mainHandler.post {
                    if (cleaned.isNotEmpty()) {
                        onTokenState.value(cleaned)
                    } else {
                        Logger.w(SmartCaptchaLog.TAG, "JS onGetToken empty id=$instanceId")
                        onErrorState.value("")
                    }
                }
            }

            @JavascriptInterface
            fun onChallengeVisible() {
                Logger.i(SmartCaptchaLog.TAG, "JS onChallengeVisible id=$instanceId")
                mainHandler.post { onChallengeVisibleState.value() }
            }

            @JavascriptInterface
            fun onChallengeHidden() {
                Logger.i(SmartCaptchaLog.TAG, "JS onChallengeHidden id=$instanceId")
                mainHandler.post { onChallengeHiddenState.value() }
            }
        }
    }

    DisposableEffect(webView, lifecycleOwner) {
        val wv = webView ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            Logger.d(
                SmartCaptchaLog.TAG,
                "lifecycle $event id=$instanceId url=${SmartCaptchaLog.shortUrl(wv.url)} " +
                    "progress=${wv.progress}",
            )
            when (event) {
                Lifecycle.Event.ON_PAUSE -> wv.onPause()
                Lifecycle.Event.ON_RESUME -> wv.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            wv.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            wv.onPause()
        }
    }

    AndroidView(
        factory = { context ->
            Logger.i(SmartCaptchaLog.TAG, "AndroidView.factory id=$instanceId")
            WebView(context).apply {
                setBackgroundColor(backgroundArgb)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                addJavascriptInterface(bridge, "NativeClient")
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val url = request?.url?.toString()
                        val host = request?.url?.host?.lowercase().orEmpty()
                        val block = host.isNotEmpty() &&
                            !host.endsWith("yandex.ru") &&
                            !host.endsWith("yandex.com") &&
                            !host.endsWith("yandex.net")
                        Logger.d(
                            SmartCaptchaLog.TAG,
                            "shouldOverrideUrlLoading id=$instanceId block=$block " +
                                "host=$host url=${SmartCaptchaLog.shortUrl(url)}",
                        )
                        return block
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        Logger.i(
                            SmartCaptchaLog.TAG,
                            "onPageStarted id=$instanceId url=${SmartCaptchaLog.shortUrl(url)}",
                        )
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        Logger.i(
                            SmartCaptchaLog.TAG,
                            "onPageFinished id=$instanceId progress=${view?.progress} " +
                                "url=${SmartCaptchaLog.shortUrl(url)}",
                        )
                        Handler(Looper.getMainLooper()).post {
                            onReadyState.value()
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        Logger.w(
                            SmartCaptchaLog.TAG,
                            "onReceivedError id=$instanceId main=${request?.isForMainFrame} " +
                                "code=${error?.errorCode} desc=${error?.description} " +
                                "url=${SmartCaptchaLog.shortUrl(request?.url?.toString())}",
                        )
                        if (request?.isForMainFrame == true) {
                            Handler(Looper.getMainLooper()).post {
                                onErrorState.value(error?.description?.toString().orEmpty())
                            }
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        Logger.w(
                            SmartCaptchaLog.TAG,
                            "onReceivedHttpError id=$instanceId main=${request?.isForMainFrame} " +
                                "status=${errorResponse?.statusCode} " +
                                "url=${SmartCaptchaLog.shortUrl(request?.url?.toString())}",
                        )
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?,
                    ) {
                        Logger.e(
                            SmartCaptchaLog.TAG,
                            "onReceivedSslError id=$instanceId primary=${error?.primaryError} " +
                                "url=${SmartCaptchaLog.shortUrl(error?.url)}",
                        )
                        handler?.cancel()
                        Handler(Looper.getMainLooper()).post {
                            onErrorState.value("SSL error")
                        }
                    }
                }
                Logger.i(
                    SmartCaptchaLog.TAG,
                    "loadUrl id=$instanceId url=${SmartCaptchaLog.shortUrl(captchaUrl)}",
                )
                loadUrl(captchaUrl)
                webView = this
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { wv ->
            wv.setBackgroundColor(backgroundArgb)
            webView = wv
        },
        onRelease = { wv ->
            Logger.i(
                SmartCaptchaLog.TAG,
                "AndroidView.onRelease id=$instanceId url=${SmartCaptchaLog.shortUrl(wv.url)}",
            )
            wv.removeJavascriptInterface("NativeClient")
            wv.stopLoading()
            wv.destroy()
            if (webView === wv) webView = null
        },
    )
}
