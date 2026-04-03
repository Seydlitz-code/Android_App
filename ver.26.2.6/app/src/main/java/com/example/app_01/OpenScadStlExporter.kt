package com.example.app_01

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * OpenSCAD 호환 소스를 **바이너리 STL**로 렌더링합니다.
 * CDN(jsDelivr)에서 openscad-wasm-prebuilt를 로드해 WebView(Chromium)에서 실행합니다.
 */
object OpenScadStlExporter {
    private const val TAG = "OpenScadStlExporter"
    private const val TIMEOUT_MS = 180_000L
    private const val WASM_MODULE_URL =
        "https://cdn.jsdelivr.net/npm/openscad-wasm-prebuilt@1.2.0/dist/openscad.js"

    /** WebView·WASM 동시 실행 방지 — 겹치면 기기 부하·실패·「계속 변환 중」처럼 보임 */
    private val renderMutex = Mutex()

    suspend fun renderStlBytes(context: Context, scadSource: String): Result<ByteArray> =
        renderMutex.withLock {
        withContext(Dispatchers.Main) {
            val cleaned = AiCadScadPreprocessor.prepareForRender(scadSource)
            suspendCancellableCoroutine { cont ->
                val appCtx = context.applicationContext
                val finished = AtomicBoolean(false)
                val mainHandler = Handler(Looper.getMainLooper())
                var webViewRef: WebView? = null

                fun destroyWebView() {
                    try {
                        webViewRef?.stopLoading()
                        webViewRef?.loadUrl("about:blank")
                        webViewRef?.destroy()
                    } catch (e: Exception) {
                        Log.w(TAG, "WebView destroy", e)
                    } finally {
                        webViewRef = null
                    }
                }

                fun finishSuccess(bytes: ByteArray) {
                    if (!finished.compareAndSet(false, true)) return
                    mainHandler.removeCallbacksAndMessages(null)
                    destroyWebView()
                    if (cont.isActive) cont.resume(Result.success(bytes))
                }

                fun finishFailure(t: Throwable) {
                    if (!finished.compareAndSet(false, true)) return
                    mainHandler.removeCallbacksAndMessages(null)
                    destroyWebView()
                    if (cont.isActive) cont.resume(Result.failure(t))
                }

                val timeoutRunnable = Runnable {
                    finishFailure(
                        IllegalStateException(
                            "OpenSCAD WASM 변환 시간 초과 (${TIMEOUT_MS / 1000}초). " +
                                "네트워크·코드·${'$'}fn을 줄여 보세요."
                        )
                    )
                }
                mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS)

                cont.invokeOnCancellation {
                    mainHandler.removeCallbacksAndMessages(null)
                    mainHandler.post { destroyWebView() }
                }

                val scadB64 = Base64.encodeToString(
                    cleaned.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )

                @SuppressLint("SetJavaScriptEnabled")
                val webView = WebView(appCtx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    // WASM 재로드·CDN 갱신이 필요할 때 캐시만 쓰지 않도록
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                }
                webViewRef = webView

                val bridge = object {
                    /** HTML에 Base64를 끼워 넣지 않고 전달 — 긴 스크립트(인터넷 참조 등)에서 loadData 실패 방지 */
                    @JavascriptInterface
                    fun getScadBase64(): String = scadB64

                    @JavascriptInterface
                    fun onSuccess(b64: String) {
                        mainHandler.post {
                            try {
                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                if (bytes.isEmpty()) {
                                    finishFailure(IllegalStateException("STL 결과가 비어 있습니다."))
                                } else {
                                    finishSuccess(bytes)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "STL Base64 디코드 실패", e)
                                finishFailure(e)
                            }
                        }
                    }

                    @JavascriptInterface
                    fun onError(msg: String) {
                        mainHandler.post {
                            Log.e(TAG, "JS 오류: $msg")
                            finishFailure(IllegalStateException(msg))
                        }
                    }
                }
                webView.addJavascriptInterface(bridge, "ScadExportBridge")

                webView.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            Log.d(TAG, "JS [${it.messageLevel()}] ${it.message()}")
                        }
                        return true
                    }
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            val desc = error?.description?.toString() ?: "unknown"
                            mainHandler.post {
                                finishFailure(IllegalStateException("페이지 로드 오류: $desc"))
                            }
                        }
                    }
                }

                val html = buildHtml()
                webView.loadDataWithBaseURL(
                    "https://cdn.jsdelivr.net/npm/openscad-wasm-prebuilt@1.2.0/dist/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
        }

    private fun buildHtml(): String = """
<!DOCTYPE html>
<html><head><meta charset="utf-8"></head>
<body>
<script type="module">
async function go() {
  try {
    const { createOpenSCAD } = await import('$WASM_MODULE_URL');
    if (typeof createOpenSCAD !== 'function') {
      ScadExportBridge.onError('createOpenSCAD를 불러오지 못했습니다.');
      return;
    }
    const scadB64 = ScadExportBridge.getScadBase64();
    const raw = atob(scadB64);
    const bytes = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
    const scad = new TextDecoder('utf-8').decode(bytes);

    let stdoutBuf = '';
    let stderrBuf = '';
    const api = await createOpenSCAD({
      noInitialRun: true,
      print: function (t) { stdoutBuf += t + '\n'; },
      printErr: function (t) { stderrBuf += t + '\n'; },
    });
    const instance = api.getInstance();
    // 문자열 그대로 쓰면 일부 환경에서 UTF-8과 불일치할 수 있어 바이트로 고정
    const enc = new TextEncoder();
    instance.FS.writeFile('/input.scad', enc.encode(scad));

    function readOutStl() {
      try {
        return instance.FS.readFile('/out.stl');
      } catch (e) {
        return null;
      }
    }
    function unlinkOut() {
      try { instance.FS.unlink('/out.stl'); } catch (e) {}
    }

    // 인자 순서·Manifold 백엔드(큰 CSG에 유리)로 재시도 — STL 미생성 시 원인 완화
    const attempts = [
      ['/input.scad', '-o', '/out.stl'],
      ['-o', '/out.stl', '/input.scad'],
      ['--enable=manifold', '/input.scad', '-o', '/out.stl'],
    ];
    let rc = -1;
    let out = null;
    for (let ai = 0; ai < attempts.length; ai++) {
      unlinkOut();
      rc = instance.callMain(attempts[ai]);
      out = readOutStl();
      if (out) break;
    }

    if (!out) {
      let rootList = '';
      try {
        rootList = instance.FS.readdir('/').join(', ');
      } catch (e) {
        rootList = String(e);
      }
      const diag = (stderrBuf + '\n' + stdoutBuf).trim();
      const tail = diag.length > 2500 ? diag.slice(-2500) : diag;
      ScadExportBridge.onError(
        'STL 출력 파일이 없습니다 (OpenSCAD 종료 코드: ' + String(rc) + '). FS(/): ' + rootList +
          (tail ? '\n--- OpenSCAD 로그 ---\n' + tail : '')
      );
      return;
    }
    const u8 = (out instanceof Uint8Array) ? out : new Uint8Array(out);
    if (!u8 || u8.length < 84) {
      ScadExportBridge.onError(
        'STL이 비어 있거나 손상되었습니다 (OpenSCAD 종료 코드: ' + String(rc) + '). 코드·${'$'}fn을 확인하세요.'
      );
      return;
    }
    // Emscripten/OpenSCAD는 경고만 있어도 비0 종료 코드를 줄 수 있음. 유효한 바이너리 STL이면 저장 허용.
    if (u8.length >= 84) {
      const tri = new DataView(u8.buffer, u8.byteOffset + 80, 4).getUint32(0, true);
      if (tri === 0 && u8.length <= 84) {
        ScadExportBridge.onError(
          '메쉬가 비어 있습니다 (OpenSCAD 종료 코드: ' + String(rc) + '). union()/geometry를 확인하세요.'
        );
        return;
      }
    }
    if (typeof rc === 'number' && rc !== 0) {
      console.warn('[OpenSCAD] nonzero exit code', rc, 'stlBytes', u8.length);
    }
    ScadExportBridge.onSuccess(u8ToB64(u8));
  } catch (e) {
    ScadExportBridge.onError(String(e && (e.stack || e)));
  }
}
function u8ToB64(u8) {
  const len = u8.length;
  if (len === 0) return '';
  let binary = '';
  const chunk = 0x8000;
  for (let i = 0; i < len; i += chunk) {
    binary += String.fromCharCode.apply(null, u8.subarray(i, Math.min(len, i + chunk)));
  }
  return btoa(binary);
}
go();
</script>
</body></html>
    """.trimIndent()
}

/** WASM 렌더 전 OpenSCAD 소스 정리 */
object OpenCadSanitizer {
    private val markdownHeadingLead = Regex("^#+\\s")

    fun stripColorForWasm(source: String): String {
        var s = source
        val patterns = listOf(
            Regex("""color\s*\(\s*\[[^\]]*\]\s*\)""", RegexOption.IGNORE_CASE),
            Regex("""color\s*\(\s*"[^"]*"\s*\)""", RegexOption.IGNORE_CASE),
            Regex("""color\s*\(\s*#[0-9A-Fa-f]{3,8}\s*\)""", RegexOption.IGNORE_CASE),
            Regex("""color\s*\(\s*\)""", RegexOption.IGNORE_CASE),
        )
        var iterations = 0
        while (iterations < 48) {
            iterations++
            val before = s
            for (p in patterns) {
                s = p.replace(s, "")
            }
            if (s == before) break
        }
        return s.replace(Regex("""\n{3,}"""), "\n\n").trim()
    }

    /**
     * 펜스 밖 한국어·설명만 있으면 구문 오류가 날 수 있어 첫 유효 라인까지 정리.
     */
    fun sanitizeForScad(source: String): String {
        val s = source.trim()
        if (s.isEmpty()) return s
        val noFences = s.lines()
            .filterNot { line -> line.trim().matches(Regex("^```[a-zA-Z0-9_-]*$")) }
            .joinToString("\n")
            .replace(Regex("```[a-zA-Z0-9_-]*"), "")
            .trim()
        val lines = noFences.lines()
        val sb = StringBuilder()
        var started = false
        for (line in lines) {
            val t = line.trim()
            if (!started) {
                if (t.isEmpty() || t.startsWith("//") || t.startsWith("/*")) {
                    sb.appendLine(line)
                    continue
                }
                if (t.startsWith("*")) continue
                // 마크다운 제목(# 뒤 공백)만 건너뜀. OpenSCAD `#cube()` 등 디버그 수정자는 유지
                if (t.startsWith("#") && markdownHeadingLead.containsMatchIn(t)) continue
                started = true
            }
            sb.appendLine(line)
        }
        return sb.toString().trim().ifBlank { noFences }
    }
}
