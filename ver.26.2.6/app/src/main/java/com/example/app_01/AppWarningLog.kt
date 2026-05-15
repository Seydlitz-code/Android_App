package com.example.app_01

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 프로필「경고 로그」용 내부 기록.
 * 처리되지 않은 JVM 예외(강제 종료 직전)와 선택적 [record] 호출 내용을
 * 앱 전용 디렉터리 파일에 남겨 재실행 후에도 확인할 수 있게 한다.
 *
 * 네이티브 SIGSEGV 등은 Java 핸들러로 잡히지 않음.
 */
object AppWarningLog {

    private const val FILE_NAME = "app_warning_log.txt"
    private const val MAX_TOTAL_BYTES = 512_000
    private val lock = Any()
    private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private fun logFile(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    private fun timestamp(): String = timeFmt.format(Date())

    /** 일반 실행 중 기록(OOM 방지 위해 본문이 길면 잘라 씀) */
    fun record(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        val truncated = message.take(12_000)
        val sb = StringBuilder().apply {
            append("[WARN] ").append(timestamp()).append('\n')
            append("태그: ").append(tag).append('\n')
            append(truncated).append('\n')
            if (throwable != null) {
                append(throwableToString(throwable, maxStackLines = 80))
            }
            append("――――――――――――――――――――――――\n")
        }
        appendSynced(context.applicationContext, sb.toString())
    }

    /** 비정상 종료 직전 동기 저장(디스크에 최대한 flush) */
    fun appendCrashBlocking(context: Context, thread: Thread, throwable: Throwable) {
        val sb = StringBuilder().apply {
            append("[CRASH] ").append(timestamp()).append('\n')
            append("앱 버전: ").append(appVersionLabel(context.applicationContext)).append('\n')
            append(
                "기기/OS: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(", Android ").append(Build.VERSION.SDK_INT).append('\n')
            append("스레드: ").append(thread.name).append(" (").append(thread.id).append(')').append('\n')
            append(throwableToString(throwable, maxStackLines = 120))
            append("――――――――――――――――――――――――\n")
        }
        appendSynced(context.applicationContext, sb.toString())
    }

    private fun appendSynced(appContext: Context, chunk: String) {
        synchronized(lock) {
            try {
                val f = logFile(appContext)
                val approxBytes = chunk.toByteArray(Charsets.UTF_8).size.toLong()
                val needTrim = f.exists() && f.length() + approxBytes > MAX_TOTAL_BYTES
                if (needTrim && f.exists()) {
                    val keep = tailUtf8Chars(f.readText(Charsets.UTF_8), MAX_TOTAL_BYTES / 2)
                    f.writeText(
                        "--- (이전 기록 길이 제한으로 일부 삭제됨) ---\n$keep",
                        Charsets.UTF_8,
                    )
                }
                FileOutputStream(f, true).use { fos ->
                    PrintWriter(OutputStreamWriter(fos, Charsets.UTF_8)).use { pw ->
                        pw.print(chunk)
                        pw.flush()
                    }
                    fos.flush()
                    try {
                        fos.fd.sync()
                    } catch (_: IOException) {
                    }
                }
            } catch (_: Throwable) {
                try {
                    FileOutputStream(logFile(appContext), true).use { fos ->
                        fos.write(chunk.toByteArray(Charsets.UTF_8))
                        fos.flush()
                        try {
                            fos.fd.sync()
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun readLogText(context: Context): String {
        synchronized(lock) {
            val f = logFile(context.applicationContext)
            if (!f.isFile || f.length() == 0L) {
                return "저장된 경고 로그가 없습니다.\n\n처리되지 않은 치명적 오류가 발생하면 재실행 후 이 화면에서 확인할 수 있습니다."
            }
            return try {
                f.readText(Charsets.UTF_8)
            } catch (_: Throwable) {
                "(로그 파일을 읽을 수 없습니다.)"
            }
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            try {
                logFile(context.applicationContext).delete()
            } catch (_: Throwable) {}
        }
    }

    private fun throwableToString(t: Throwable, maxStackLines: Int): String {
        val sw = java.io.StringWriter()
        PrintWriter(sw).use { pw ->
            t.printStackTrace(pw)
            var cause = t.cause
            var depth = 0
            while (cause != null && depth < 6) {
                pw.append("\nCaused by: ")
                cause.printStackTrace(pw)
                cause = cause.cause
                depth++
            }
        }
        val lines = sw.toString().lineSequence().take(maxStackLines).joinToString("\n")
        return "$lines\n"
    }

    private fun tailUtf8Chars(s: String, maxChars: Int): String =
        if (s.length <= maxChars) s else s.substring(s.length - maxChars)

    private fun appVersionLabel(context: Context): String {
        return try {
            val pm = context.packageManager
            val pn = context.packageName
            if (Build.VERSION.SDK_INT >= 33) {
                val p = pm.getPackageInfo(pn, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                "${p.versionName ?: "?"} (${p.longVersionCode})"
            } else {
                @Suppress("DEPRECATION")
                val p = pm.getPackageInfo(pn, 0)
                "${p.versionName ?: "?"} (${p.versionCode})"
            }
        } catch (_: Throwable) {
            "?"
        }
    }
}

object AppWarningLogInstaller {
    fun install(application: Application) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                AppWarningLog.appendCrashBlocking(application.applicationContext, thread, throwable)
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
