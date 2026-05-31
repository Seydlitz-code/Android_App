package com.example.app_01

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Gs3dWebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
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

    BackHandler(onBack = onClose)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = validatedUrl,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

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
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {}
                        override fun onPageFinished(view: WebView?, url: String?) {}
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            android.util.Log.e("Gs3dWebView", "Error: ${error?.description} code=${error?.errorCode}")
                        }
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean = false
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                            if (cm != null) {
                                android.util.Log.i("Gs3dWebView", "[${cm.messageLevel()}] ${cm.message()}")
                            }
                            return true
                        }
                    }
                    loadUrl(validatedUrl)
                }
            },
            update = { webView ->
                if (webView.url != validatedUrl) webView.loadUrl(validatedUrl)
            },
            modifier = Modifier
                .weight(1f),
        )
    }
}
