package com.example.app_01

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Gs3dWebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
) {
    val validatedUrl = remember(url) {
        val u = url.trim()
        when {
            u.isEmpty() || u == "null" || u == "https://null" -> {
                android.util.Log.e("Gs3dWebView", "Invalid URL rejected: '$url'")
                "about:blank"
            }
            u.startsWith("http://", ignoreCase = true) || u.startsWith("https://", ignoreCase = true) -> u
            else -> "https://$u"
        }
    }
    android.util.Log.i("Gs3dWebView", "Loading URL: $validatedUrl")
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        android.util.Log.i("Gs3dWebView", "Page started: $url")
                    }
                    override fun onPageFinished(view: WebView?, url: String?) {
                        android.util.Log.i("Gs3dWebView", "Page finished: $url")
                    }
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        android.util.Log.e(
                            "Gs3dWebView",
                            "WebView error: ${error?.description} (code ${error?.errorCode}) url=${request?.url}",
                        )
                    }
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean = false
                }
                webChromeClient = WebChromeClient()
                loadUrl(validatedUrl)
            }
        },
        update = { webView ->
            if (webView.url != validatedUrl) {
                android.util.Log.i("Gs3dWebView", "Update URL: $validatedUrl")
                webView.loadUrl(validatedUrl)
            }
        },
        modifier = modifier,
    )
}
