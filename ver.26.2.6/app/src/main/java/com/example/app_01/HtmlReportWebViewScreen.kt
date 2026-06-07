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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlReportWebViewScreen(
    htmlFile: File,
    title: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    /** 깨진 `<img>` 보강용 첨부·DA3 투영 이미지 URI */
    attachmentImageUris: List<Uri> = emptyList(),
) {
    val context = LocalContext.current
    val htmlContent = remember(htmlFile.absolutePath, attachmentImageUris) {
        val raw = runCatching { htmlFile.readText(Charsets.UTF_8) }.getOrDefault("")
        val parent = htmlFile.parentFile ?: return@remember raw
        if (attachmentImageUris.isEmpty()) return@remember raw
        val needsPatch = HtmlReportImageEmbedder.htmlNeedsImageEmbedPatch(raw)
        if (!needsPatch && !raw.contains("<img", ignoreCase = true)) return@remember raw
        val patched = HtmlReportImageEmbedder.embedAttachedImages(
            context,
            raw,
            parent,
            attachmentImageUris,
        )
        if (patched != raw) {
            runCatching { htmlFile.writeText(patched, Charsets.UTF_8) }
        }
        patched
    }
    val baseUrl = remember(htmlFile.absolutePath) {
        "file://${htmlFile.parentFile?.absolutePath?.replace('\\', '/')}/"
    }

    BackHandler(onBack = onClose)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B4F8A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
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
                    tint = Color.White.copy(alpha = 0.9f),
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
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    @Suppress("DEPRECATION")
                    run {
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                    }
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {}
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            android.util.Log.e(
                                "HtmlReportWebView",
                                "Error: ${error?.description} code=${error?.errorCode}",
                            )
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                            if (cm != null) {
                                android.util.Log.i(
                                    "HtmlReportWebView",
                                    "[${cm.messageLevel()}] ${cm.message()}",
                                )
                            }
                            return true
                        }
                    }
                    loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.weight(1f),
        )
    }
}
