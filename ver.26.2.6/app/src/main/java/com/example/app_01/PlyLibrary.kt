package com.example.app_01

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** PLY·GLB 3D 모델 라이브러리 — 서버 파이프라인 산출물 및 사용자 가져오기 파일 */
object PlyLibrary {

    private const val IMPORTED_SUBDIR = "imported"

    fun dir(context: Context): File = ModelLibraryPaths.plyDir(context)

    fun importedDir(context: Context): File =
        File(dir(context), IMPORTED_SUBDIR).apply { mkdirs() }

    fun listFilesSorted(context: Context): List<File> =
        collectModelFilesRecursive(dir(context))
            .sortedByDescending { it.lastModified() }

    internal fun collectModelFilesRecursive(root: File): List<File> {
        val out = ArrayList<File>()
        val entries = root.listFiles() ?: return out
        for (entry in entries) {
            when {
                entry.isFile && isSupportedModelFile(entry) -> out.add(entry)
                entry.isDirectory -> {
                    val sub = entry.listFiles() ?: continue
                    for (f in sub) {
                        if (f.isFile && isSupportedModelFile(f)) out.add(f)
                    }
                }
            }
        }
        return out.distinctBy { it.absolutePath }
    }

    fun isSupportedModelFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext == "ply" || ext == "glb"
    }

    suspend fun importExternalModelFiles(context: Context, uris: List<Uri>): MediaTransferResult =
        withContext(Dispatchers.IO) {
            importExternalModelFilesImpl(context, uris)
        }

    private fun importExternalModelFilesImpl(context: Context, uris: List<Uri>): MediaTransferResult {
        if (uris.isEmpty()) return MediaTransferResult(0, 0, "가져올 파일이 없습니다.")
        val outDir = importedDir(context)
        val resolver = context.contentResolver
        var ok = 0
        var fail = 0
        var skipped = 0

        for (uri in uris) {
            try {
                val displayName = resolveDisplayName(context, uri)
                val ext = guessModelExt(context, displayName, uri, resolver)
                if (ext == null) {
                    skipped++
                    continue
                }
                val base = sanitizeBaseName(
                    displayName
                        ?.substringBeforeLast('.', missingDelimiterValue = "")
                        ?.takeIf { it.isNotBlank() }
                        ?: "model_${timestampCompact()}",
                )
                val fileName = uniqueFileName(outDir, base, ext)
                val outFile = File(outDir, fileName)
                val input = resolver.openInputStream(uri)
                if (input == null) {
                    fail++
                    continue
                }
                input.use { stream ->
                    outFile.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                }
                if (outFile.length() == 0L) {
                    outFile.delete()
                    fail++
                } else {
                    ok++
                }
            } catch (_: Exception) {
                fail++
            }
        }

        val msg = when {
            ok > 0 && (fail > 0 || skipped > 0) ->
                "PLY·GLB ${ok}개 추가 (실패 $fail, 지원하지 않는 형식 $skipped)"
            ok > 0 -> "PLY·GLB ${ok}개를 PLY 폴더에 추가했습니다."
            skipped > 0 && fail == 0 -> "PLY·GLB 파일만 선택할 수 있습니다."
            else -> "파일을 추가하지 못했습니다."
        }
        return MediaTransferResult(ok, fail + skipped, msg)
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            } else {
                uri.lastPathSegment
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    private fun guessModelExt(
        context: Context,
        displayName: String?,
        uri: Uri,
        resolver: android.content.ContentResolver,
    ): String? {
        extFromFileName(displayName)?.let { return it }
        extFromUriPath(uri)?.let { return it }

        when (resolver.getType(uri)?.lowercase()) {
            "model/ply" -> return "ply"
            "model/gltf-binary", "model/gltf+glb" -> return "glb"
        }

        return sniffModelExt(context, uri)
    }

    private fun extFromFileName(name: String?): String? =
        name?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it == "ply" || it == "glb" }

    /** DocumentsProvider URI는 lastPathSegment가 숫자 ID인 경우가 많아 path·전체 URI까지 검사합니다. */
    private fun extFromUriPath(uri: Uri): String? {
        val candidates = listOfNotNull(uri.lastPathSegment, uri.path, uri.toString())
        for (raw in candidates) {
            val decoded = Uri.decode(raw)
            val ext = decoded.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            if (ext == "ply" || ext == "glb") return ext
        }
        return null
    }

    /** 확장자·MIME가 없을 때 GLB(`glTF` 매직) / PLY(`ply` 헤더) 바이트로 판별합니다. */
    private fun sniffModelExt(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(12)
                val read = stream.read(header)
                if (read >= 4 &&
                    header[0] == 'g'.code.toByte() &&
                    header[1] == 'l'.code.toByte() &&
                    header[2] == 'T'.code.toByte() &&
                    header[3] == 'F'.code.toByte()
                ) {
                    "glb"
                } else if (read >= 3 &&
                    header[0] == 'p'.code.toByte() &&
                    header[1] == 'l'.code.toByte() &&
                    header[2] == 'y'.code.toByte()
                ) {
                    "ply"
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeBaseName(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-\\uAC00-\\uD7A3]"), "_")
            .take(48)
            .ifBlank { "model" }

    private fun uniqueFileName(dir: File, base: String, ext: String): String {
        var candidate = "$base.$ext"
        var n = 1
        while (File(dir, candidate).exists()) {
            candidate = "${base}_$n.$ext"
            n++
        }
        return candidate
    }
}
