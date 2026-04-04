package com.example.app_01

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix as GLMatrix
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

/** GL 미리보기·UI용: 초과 시 균등 간격으로 줄여 힙 OOM을 완화합니다. */
private const val MAX_PREVIEW_VERTICES = 350_000

/**
 * PLY -> OBJ 변환 (캐시)
 * - 색이 있으면 OBJ + MTL 쌍을 만들고 primitive별 `usemtl`로 색을 연결합니다.
 * - 포인트 클라우드는 `p`, 메쉬는 `f`를 기록합니다.
 */
data class ObjConversionResult(
    val file: File? = null,
    val error: String? = null,
    val previewMesh: ObjParseResult? = null
)

fun convertPlyToObjCached(context: Context, plyFile: File): ObjConversionResult {
    if (!plyFile.exists()) return ObjConversionResult(error = "원본 PLY 파일이 존재하지 않습니다.")
    val storageRoot = context.getExternalFilesDir(null)
        ?: return ObjConversionResult(error = "앱 저장 공간에 접근할 수 없습니다.")

    return try {
        val outDir = File(storageRoot, "models_obj").apply { mkdirs() }
        val key = "${plyFile.nameWithoutExtension}_${plyFile.lastModified()}_v5"
        val outFile = File(outDir, "${key}.obj")
        val mtlFile = File(outDir, "${key}.mtl")

        // 헤더만 읽어 색 유무 판별 — 캐시 히트 시 전체 PLY 파싱을 건너뜁니다(힙 OOM 방지).
        val hasVertexColorsHint = hasPlyVertexRgbHeader(plyFile)
        val cacheReady = outFile.exists() &&
            outFile.length() > 0L &&
            (!hasVertexColorsHint || (mtlFile.exists() && mtlFile.length() > 0L))
        if (cacheReady) {
            val fromObj = try {
                parseObjVertices(outFile)
            } catch (oom: OutOfMemoryError) {
                android.util.Log.e("ObjViewer", "parseObjVertices OOM (cached obj)", oom)
                null
            }
            if (fromObj != null) {
                return ObjConversionResult(file = outFile, previewMesh = fromObj)
            }
        }

        val parsed = parsePlyPointsAny(plyFile)
        val points = parsed.points
        val count = parsed.count
        val vtxColors = parsed.vertexColors
        val hasVertexColors = vtxColors != null && vtxColors.size == count * 3
        if (count <= 0) return ObjConversionResult(error = "PLY에서 vertex(점)를 0개로 인식했습니다.")

        val faces = readPlyFacesIfPresent(plyFile)
        val previewMesh = buildPreviewMeshFromPly(points, count, vtxColors, faces)

        // 기존 캐시(같은 nameWithoutExtension) 정리(너무 많이 쌓이는 것 방지)
        outDir.listFiles { f ->
            f.isFile &&
                f.name.startsWith("${plyFile.nameWithoutExtension}_") &&
                (
                    f.name.endsWith(".obj", ignoreCase = true) ||
                        f.name.endsWith(".mtl", ignoreCase = true)
                    ) &&
                f.name != outFile.name &&
                f.name != mtlFile.name
        }?.forEach { f ->
            try { f.delete() } catch (_: Exception) {}
        }

        val vertexMaterialNames = if (hasVertexColors) {
            buildObjMaterialNames(vtxColors!!)
        } else {
            null
        }

        if (vertexMaterialNames != null) {
            writeObjMaterialFile(mtlFile, plyFile, vtxColors!!, vertexMaterialNames)
        } else if (mtlFile.exists()) {
            try { mtlFile.delete() } catch (_: Exception) {}
        }

        FileOutputStream(outFile).bufferedWriter().use { w ->
            w.appendLine("# OBJ file with MTL materials")
            w.appendLine("# Generated from ${plyFile.name}")
            if (vertexMaterialNames != null) {
                w.appendLine("mtllib ${mtlFile.name}")
            }
            w.appendLine("# vertices: $count")
            w.appendLine()
            for (i in 0 until count) {
                appendObjVertexLine(w, points, i)
            }

            var faceWritten = 0
            if (!faces.isNullOrEmpty()) {
                var currentMaterial: String? = null
                for (tri in faces) {
                    if (tri.size < 3) continue
                    val a = tri[0]
                    val b = tri[1]
                    val c = tri[2]
                    if (a !in 0 until count || b !in 0 until count || c !in 0 until count) continue
                    if (vertexMaterialNames != null) {
                        val matName = vertexMaterialNames[a]
                        if (matName != currentMaterial) {
                            w.appendLine("usemtl $matName")
                            currentMaterial = matName
                        }
                    }
                    w.append("f ")
                    w.append((a + 1).toString()); w.append(' ')
                    w.append((b + 1).toString()); w.append(' ')
                    w.appendLine((c + 1).toString())
                    faceWritten++
                }
            }

            if (faceWritten <= 0) {
                if (vertexMaterialNames != null) {
                    var currentMaterial: String? = null
                    for (i in 0 until count) {
                        val matName = vertexMaterialNames[i]
                        if (matName != currentMaterial) {
                            w.appendLine("usemtl $matName")
                            currentMaterial = matName
                        }
                        w.append("p ")
                        w.appendLine((i + 1).toString())
                    }
                } else {
                    val chunk = 128
                    var idx = 1
                    while (idx <= count) {
                        val end = minOf(count, idx + chunk - 1)
                        w.append("p")
                        for (j in idx..end) {
                            w.append(' ')
                            w.append(j.toString())
                        }
                        w.appendLine()
                        idx = end + 1
                    }
                }
            }
        }

        if (!outFile.exists() || outFile.length() <= 0L) {
            ObjConversionResult(
                error = "OBJ 파일 생성에 실패했습니다(파일이 생성되지 않았거나 크기가 0입니다).",
                previewMesh = downsamplePreviewMesh(previewMesh)
            )
        } else {
            ObjConversionResult(file = outFile, previewMesh = downsamplePreviewMesh(previewMesh))
        }
    } catch (oom: OutOfMemoryError) {
        oom.printStackTrace()
        ObjConversionResult(
            error = "메모리가 부족합니다. PLY가 너무 크거나 다른 작업으로 힙이 가득 찼습니다. 앱을 다시 실행한 뒤 시도해 주세요."
        )
    } catch (t: Throwable) {
        t.printStackTrace()
        ObjConversionResult(error = "OBJ 변환 중 오류: ${t.message ?: t.javaClass.simpleName}")
    }
}

private fun hasPlyVertexRgbHeader(file: File): Boolean {
    return RandomAccessFile(file, "r").use { raf ->
        val header = parsePlyHeader(raf)
        val props = header.vertexProperties
        fun idxByNames(vararg names: String): Int {
            for (n in names) {
                val i = props.indexOfFirst { it.name.equals(n, ignoreCase = true) }
                if (i >= 0) return i
            }
            return -1
        }
        val redIdx = idxByNames("red", "diffuse_red", "r")
        val greenIdx = idxByNames("green", "diffuse_green", "g")
        val blueIdx = idxByNames("blue", "diffuse_blue", "b")
        redIdx >= 0 && greenIdx >= 0 && blueIdx >= 0
    }
}

/**
 * 렌더링용 메쉬가 너무 크면 균등 간격으로 줄입니다(512MB 힙에서 OOM 방지).
 */
private fun downsamplePreviewMesh(mesh: ObjParseResult): ObjParseResult {
    if (mesh.count <= MAX_PREVIEW_VERTICES) return mesh
    return when (mesh.drawMode) {
        MeshDrawMode.POINTS -> {
            val stride = ((mesh.count + MAX_PREVIEW_VERTICES - 1) / MAX_PREVIEW_VERTICES).coerceAtLeast(1)
            val newCount = (mesh.count + stride - 1) / stride
            val pts = FloatArray(newCount * 3)
            val cols = if (mesh.vertexColors != null && mesh.vertexColors.size == mesh.count * 3) {
                FloatArray(newCount * 3)
            } else {
                null
            }
            var wi = 0
            var i = 0
            while (i < mesh.count) {
                pts[wi * 3] = mesh.points[i * 3]
                pts[wi * 3 + 1] = mesh.points[i * 3 + 1]
                pts[wi * 3 + 2] = mesh.points[i * 3 + 2]
                if (cols != null) {
                    cols[wi * 3] = mesh.vertexColors!![i * 3]
                    cols[wi * 3 + 1] = mesh.vertexColors[i * 3 + 1]
                    cols[wi * 3 + 2] = mesh.vertexColors[i * 3 + 2]
                }
                wi++
                i += stride
            }
            ObjParseResult(pts, newCount, MeshDrawMode.POINTS, cols)
        }
        MeshDrawMode.TRIANGLES -> {
            val triCount = mesh.count / 3
            if (triCount <= 0) return mesh
            val stride = ((triCount + MAX_PREVIEW_VERTICES - 1) / MAX_PREVIEW_VERTICES).coerceAtLeast(1)
            val newTriCount = (triCount + stride - 1) / stride
            val pts = FloatArray(newTriCount * 9)
            val cols = if (mesh.vertexColors != null && mesh.vertexColors.size == mesh.count * 3) {
                FloatArray(newTriCount * 9)
            } else {
                null
            }
            var wi = 0
            var t = 0
            while (t < triCount) {
                val vb = t * 9
                for (k in 0 until 9) {
                    pts[wi * 9 + k] = mesh.points[vb + k]
                }
                if (cols != null) {
                    for (k in 0 until 9) {
                        cols[wi * 9 + k] = mesh.vertexColors!![vb + k]
                    }
                }
                wi++
                t += stride
            }
            ObjParseResult(pts, wi * 3, MeshDrawMode.TRIANGLES, cols)
        }
    }
}

private fun buildPreviewMeshFromPly(
    normalizedPoints: FloatArray,
    vertexCount: Int,
    vertexColors: FloatArray?,
    faces: List<IntArray>?
): ObjParseResult {
    if (!faces.isNullOrEmpty()) {
        val triPoints = ArrayList<Float>(faces.size * 9)
        val triColors = ArrayList<Float>(faces.size * 9)
        for (tri in faces) {
            if (tri.size < 3) continue
            val a = tri[0]
            val b = tri[1]
            val c = tri[2]
            if (a !in 0 until vertexCount || b !in 0 until vertexCount || c !in 0 until vertexCount) continue
            for (vid in intArrayOf(a, b, c)) {
                triPoints.add(normalizedPoints[vid * 3])
                triPoints.add(normalizedPoints[vid * 3 + 1])
                triPoints.add(normalizedPoints[vid * 3 + 2])
                if (vertexColors != null && vertexColors.size == vertexCount * 3) {
                    triColors.add(vertexColors[vid * 3])
                    triColors.add(vertexColors[vid * 3 + 1])
                    triColors.add(vertexColors[vid * 3 + 2])
                }
            }
        }
        if (triPoints.isNotEmpty()) {
            val pointsOut = FloatArray(triPoints.size) { triPoints[it] }
            val colorsOut = if (triColors.size == triPoints.size) {
                FloatArray(triColors.size) { triColors[it] }
            } else null
            return ObjParseResult(pointsOut, pointsOut.size / 3, MeshDrawMode.TRIANGLES, colorsOut)
        }
    }

    val pointColors = if (vertexColors != null && vertexColors.size == vertexCount * 3) {
        vertexColors.copyOf()
    } else null
    return ObjParseResult(normalizedPoints.copyOf(), vertexCount, MeshDrawMode.POINTS, pointColors)
}

/**
 * 라이브러리 썸네일용: PLY/OBJ를 동일한 [ObjParseResult]로 파싱합니다.
 * 과대 파일은 메모리 보호를 위해 생략합니다.
 */
fun loadModelForThumbnailMesh(file: File): ObjParseResult? {
    if (!file.exists() || !file.isFile) return null
    val maxBytes = 120L * 1024L * 1024L
    if (file.length() > maxBytes) return null
    return try {
        when {
            file.extension.equals("obj", ignoreCase = true) -> parseObjVertices(file)
            file.extension.equals("ply", ignoreCase = true) -> {
                val parsed = parsePlyPointsAny(file)
                val faces = readPlyFacesIfPresent(file)
                downsamplePreviewMesh(
                    buildPreviewMeshFromPly(parsed.points, parsed.count, parsed.vertexColors, faces)
                )
            }
            else -> null
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

private fun appendObjVertexLine(w: Appendable, points: FloatArray, index: Int) {
    w.append("v ")
    w.append(objNumber(points[index * 3])); w.append(' ')
    w.append(objNumber(points[index * 3 + 1])); w.append(' ')
    w.append(objNumber(points[index * 3 + 2]))
    if (w is java.io.Writer) {
        w.appendLine()
    } else {
        w.append('\n')
    }
}

private fun objNumber(v: Float): String = String.format(Locale.US, "%.6f", v.toDouble())

private fun color01ToByte(v: Float): Int = (v.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)

private fun buildObjMaterialNames(vertexColors: FloatArray): Array<String> {
    val count = vertexColors.size / 3
    val names = Array(count) { "" }
    val colorToName = LinkedHashMap<Int, String>()
    for (i in 0 until count) {
        val r = color01ToByte(vertexColors[i * 3])
        val g = color01ToByte(vertexColors[i * 3 + 1])
        val b = color01ToByte(vertexColors[i * 3 + 2])
        val key = (r shl 16) or (g shl 8) or b
        val name = colorToName.getOrPut(key) { "color_${colorToName.size}" }
        names[i] = name
    }
    return names
}

private fun writeObjMaterialFile(
    mtlFile: File,
    sourcePly: File,
    vertexColors: FloatArray,
    vertexMaterialNames: Array<String>
) {
    FileOutputStream(mtlFile).bufferedWriter().use { w ->
        w.appendLine("# Material file")
        w.appendLine("# Generated from ${sourcePly.name}")
        w.appendLine()
        val emitted = HashSet<String>()
        for (i in vertexMaterialNames.indices) {
            val matName = vertexMaterialNames[i]
            if (!emitted.add(matName)) continue
            val idx = i * 3
            val r = vertexColors[idx].coerceIn(0f, 1f)
            val g = vertexColors[idx + 1].coerceIn(0f, 1f)
            val b = vertexColors[idx + 2].coerceIn(0f, 1f)
            w.appendLine("newmtl $matName")
            w.appendLine("Kd ${objNumber(r)} ${objNumber(g)} ${objNumber(b)}")
            w.appendLine("Ka ${objNumber(r)} ${objNumber(g)} ${objNumber(b)}")
            w.appendLine("Ks 0.500000 0.500000 0.500000")
            w.appendLine("Ns 10.000000")
            w.appendLine()
        }
    }
}

private data class PlyProperty(val type: String, val name: String)

private data class PlyHeader(
    val format: String,
    val littleEndian: Boolean,
    val vertexCount: Int,
    val vertexProperties: List<PlyProperty>,
    val dataStartOffset: Long,
    /** 0이면 포인트 클라우드(면 없음) */
    val faceCount: Int = 0
)

private fun parsePlyHeader(raf: RandomAccessFile): PlyHeader {
    var format = ""
    var vertexCount = 0
    var faceCount = 0
    var inVertex = false
    var inFaceSection = false
    val props = ArrayList<PlyProperty>(16)

    // 첫 줄은 보통 "ply"
    val first = raf.readLine() ?: throw IllegalArgumentException("PLY 헤더를 읽을 수 없습니다.")
    if (!first.trim().equals("ply", ignoreCase = true)) {
        throw IllegalArgumentException("PLY 파일이 아닙니다(첫 줄: $first)")
    }

    while (true) {
        val line = raf.readLine() ?: throw IllegalArgumentException("PLY 헤더가 끝나기 전에 파일이 종료되었습니다.")
        val t = line.trim()
        if (t.isEmpty()) continue
        if (t.startsWith("comment")) continue

        if (t.startsWith("format")) {
            format = t
            continue
        }
        if (t.startsWith("element vertex")) {
            val parts = t.split(Regex("\\s+"))
            vertexCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
            inVertex = true
            inFaceSection = false
            continue
        }
        if (t.startsWith("element face")) {
            val parts = t.split(Regex("\\s+"))
            faceCount = parts.getOrNull(2)?.toIntOrNull() ?: 0
            inVertex = false
            inFaceSection = true
            continue
        }
        if (t.startsWith("element")) {
            // vertex/face 외 element
            inVertex = false
            inFaceSection = false
            continue
        }
        if (t.startsWith("property") && inVertex) {
            val parts = t.split(Regex("\\s+"))
            // list property는 미지원 (point cloud 목적)
            if (parts.size >= 5 && parts[1] == "list") {
                throw IllegalArgumentException("PLY vertex에 list property가 포함되어 있어 지원하지 않습니다: $t")
            }
            val type = parts.getOrNull(1) ?: ""
            val name = parts.getOrNull(2) ?: ""
            if (type.isNotBlank() && name.isNotBlank()) props.add(PlyProperty(type, name))
            continue
        }
        if (t.startsWith("property") && inFaceSection) {
            // property list uchar int vertex_indices 등 — 바이너리 face stride 계산에 쓰지 않고 고정 삼각형 가정
            continue
        }
        if (t == "end_header") break
    }

    val f = format.lowercase()
    val little = when {
        f.contains("binary_little_endian") -> true
        f.contains("binary_big_endian") -> false
        else -> true // ascii면 의미 없음
    }
    if (format.isBlank()) {
        throw IllegalArgumentException("PLY format 라인이 없습니다.")
    }
    if (vertexCount <= 0) {
        throw IllegalArgumentException("PLY vertex count가 0입니다.")
    }
    if (props.isEmpty()) {
        throw IllegalArgumentException("PLY vertex properties를 찾지 못했습니다.")
    }

    val dataOffset = raf.filePointer
    return PlyHeader(
        format = format,
        littleEndian = little,
        vertexCount = vertexCount,
        vertexProperties = props,
        dataStartOffset = dataOffset,
        faceCount = faceCount
    )
}

private fun typeSizeBytes(type: String): Int {
    return when (type.lowercase()) {
        "char", "int8", "uchar", "uint8" -> 1
        "short", "int16", "ushort", "uint16" -> 2
        "int", "int32", "uint", "uint32", "float", "float32" -> 4
        "double", "float64" -> 8
        else -> throw IllegalArgumentException("지원하지 않는 PLY property 타입: $type")
    }
}

private fun readScalarAsFloat(buf: ByteBuffer, type: String): Float {
    return when (type.lowercase()) {
        "char", "int8" -> buf.get().toInt().toFloat()
        "uchar", "uint8" -> (buf.get().toInt() and 0xFF).toFloat()
        "short", "int16" -> buf.short.toInt().toFloat()
        "ushort", "uint16" -> (buf.short.toInt() and 0xFFFF).toFloat()
        "int", "int32" -> buf.int.toFloat()
        "uint", "uint32" -> (buf.int.toLong() and 0xFFFF_FFFFL).toFloat()
        "float", "float32" -> buf.float
        "double", "float64" -> buf.double.toFloat()
        else -> throw IllegalArgumentException("지원하지 않는 PLY property 타입: $type")
    }
}

private fun plyColorFromScalar(value: Float, type: String): Float {
    return when (type.lowercase()) {
        "uchar", "uint8" -> (value.coerceIn(0f, 255f)) / 255f
        "char", "int8" -> ((value.toInt() + 128).coerceIn(0, 255)) / 255f
        "ushort", "uint16" -> (value.coerceIn(0f, 65535f)) / 65535f
        else -> {
            val v = value
            if (v > 1.5f) v / 255f else v.coerceIn(0f, 1f)
        }
    }
}

private fun plyColorFromAsciiToken(token: String, type: String): Float {
    return when (type.lowercase()) {
        "uchar", "uint8" -> ((token.toIntOrNull() ?: 0).coerceIn(0, 255)) / 255f
        "char", "int8" -> ((token.toIntOrNull() ?: 0).coerceIn(-128, 127) + 128) / 255f
        "ushort", "uint16" -> ((token.toIntOrNull() ?: 0).coerceIn(0, 65535)) / 65535f
        else -> {
            val v = token.toFloatOrNull() ?: 0f
            if (v > 1.5f) v / 255f else v.coerceIn(0f, 1f)
        }
    }
}

/**
 * ASCII / binary_little_endian / binary_big_endian PLY 지원
 * - vertex 섹션의 property 목록을 기준으로 x,y,z 및 (있으면) red,green,blue를 추출합니다.
 */
private fun parsePlyPointsAny(file: File): PlyParseResult {
    RandomAccessFile(file, "r").use { raf ->
        val header = parsePlyHeader(raf)
        val fmtLower = header.format.lowercase()

        val props = header.vertexProperties
        val xIdx = props.indexOfFirst { it.name == "x" }
        val yIdx = props.indexOfFirst { it.name == "y" }
        val zIdx = props.indexOfFirst { it.name == "z" }
        if (xIdx < 0 || yIdx < 0 || zIdx < 0) {
            throw IllegalArgumentException("PLY vertex properties에 x/y/z가 없습니다. (format: ${header.format})")
        }

        fun idxByNames(vararg names: String): Int {
            for (n in names) {
                val i = props.indexOfFirst { it.name.equals(n, ignoreCase = true) }
                if (i >= 0) return i
            }
            return -1
        }

        val redIdx = idxByNames("red", "diffuse_red", "r")
        val greenIdx = idxByNames("green", "diffuse_green", "g")
        val blueIdx = idxByNames("blue", "diffuse_blue", "b")
        val hasRgb = redIdx >= 0 && greenIdx >= 0 && blueIdx >= 0

        val rawPoints = FloatArray(header.vertexCount * 3)
        val rawColors = if (hasRgb) FloatArray(header.vertexCount * 3) else null

        val count: Int = if (fmtLower.contains("ascii")) {
            raf.seek(header.dataStartOffset)
            var v = 0
            while (v < header.vertexCount) {
                val line = raf.readLine() ?: break
                val t = line.trim()
                if (t.isEmpty()) continue
                val parts = t.split(Regex("\\s+"))
                if (parts.size < props.size) continue
                val x = parts.getOrNull(xIdx)?.toFloatOrNull()
                val y = parts.getOrNull(yIdx)?.toFloatOrNull()
                val z = parts.getOrNull(zIdx)?.toFloatOrNull()
                if (x != null && y != null && z != null) {
                    val base = v * 3
                    rawPoints[base] = x
                    rawPoints[base + 1] = y
                    rawPoints[base + 2] = z
                    if (rawColors != null) {
                        val rt = props[redIdx].type
                        val gt = props[greenIdx].type
                        val bt = props[blueIdx].type
                        rawColors[base] = plyColorFromAsciiToken(parts[redIdx], rt)
                        rawColors[base + 1] = plyColorFromAsciiToken(parts[greenIdx], gt)
                        rawColors[base + 2] = plyColorFromAsciiToken(parts[blueIdx], bt)
                    }
                    v++
                }
            }
            v
        } else if (fmtLower.contains("binary_little_endian") || fmtLower.contains("binary_big_endian")) {
            raf.seek(header.dataStartOffset)
            val order = if (header.littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            val rowStride = props.sumOf { typeSizeBytes(it.type) }
            val row = ByteArray(rowStride)

            for (i in 0 until header.vertexCount) {
                raf.readFully(row)
                val bb = ByteBuffer.wrap(row).order(order)
                var x = 0f
                var y = 0f
                var z = 0f
                var cr = 0f
                var cg = 0f
                var cb = 0f
                for ((pi, p) in props.withIndex()) {
                    val vv = readScalarAsFloat(bb, p.type)
                    when (pi) {
                        xIdx -> x = vv
                        yIdx -> y = vv
                        zIdx -> z = vv
                        redIdx -> cr = plyColorFromScalar(vv, p.type)
                        greenIdx -> cg = plyColorFromScalar(vv, p.type)
                        blueIdx -> cb = plyColorFromScalar(vv, p.type)
                    }
                }
                val base = i * 3
                rawPoints[base] = x
                rawPoints[base + 1] = y
                rawPoints[base + 2] = z
                if (rawColors != null) {
                    rawColors[base] = cr
                    rawColors[base + 1] = cg
                    rawColors[base + 2] = cb
                }
            }
            header.vertexCount
        } else {
            throw IllegalArgumentException("지원하지 않는 PLY format: ${header.format}")
        }

        if (count <= 0) {
            throw IllegalArgumentException("PLY에서 vertex를 읽지 못했습니다. (format: ${header.format})")
        }

        val vertexColors: FloatArray? = if (hasRgb && rawColors != null) {
            FloatArray(count * 3) { i -> rawColors[i].coerceIn(0f, 1f) }
        } else {
            null
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (i in 0 until count) {
            val x = rawPoints[i * 3]
            val y = rawPoints[i * 3 + 1]
            val z = rawPoints[i * 3 + 2]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        val cx = (minX + maxX) / 2f
        val cy = (minY + maxY) / 2f
        val cz = (minZ + maxZ) / 2f
        val maxDim = max(maxX - minX, max(maxY - minY, maxZ - minZ))
        val half = if (maxDim > 0f) maxDim / 2f else 1f

        for (i in 0 until count) {
            val x = rawPoints[i * 3]
            val y = rawPoints[i * 3 + 1]
            val z = rawPoints[i * 3 + 2]
            rawPoints[i * 3] = (x - cx) / half
            rawPoints[i * 3 + 1] = (y - cy) / half
            rawPoints[i * 3 + 2] = (z - cz) / half
        }

        val points = if (count == header.vertexCount) rawPoints else rawPoints.copyOf(count * 3)
        return PlyParseResult(points, count, vertexColors)
    }
}

/**
 * 일부 PLY는 면 인덱스를 1-based로 저장합니다. max 인덱스 ≥ vertexCount 이면 1을 뺍니다.
 */
private fun normalizePlyFaceIndicesIfNeeded(faces: List<IntArray>, vertexCount: Int): List<IntArray>? {
    if (faces.isEmpty()) return null
    val maxI = faces.maxOf { maxOf(it[0], maxOf(it[1], it[2])) }
    val adjusted = if (maxI >= vertexCount) {
        faces.map { intArrayOf(it[0] - 1, it[1] - 1, it[2] - 1) }
    } else {
        faces
    }
    val max2 = adjusted.maxOf { maxOf(it[0], maxOf(it[1], it[2])) }
    if (max2 >= vertexCount) return null
    if (adjusted.any { it[0] < 0 || it[1] < 0 || it[2] < 0 }) return null
    return adjusted
}

/**
 * PLY에 element face가 있으면 삼각형 인덱스(0-based) 목록을 읽습니다.
 * ASCII 정점 건너뛰기 규칙은 [parsePlyPointsAny]와 동일합니다.
 */
private fun readPlyFacesIfPresent(file: File): List<IntArray>? {
    return RandomAccessFile(file, "r").use { raf ->
        val header = parsePlyHeader(raf)
        if (header.faceCount <= 0) return@use null
        val fmtLower = header.format.lowercase()
        val props = header.vertexProperties
        val xIdx = props.indexOfFirst { it.name == "x" }
        val yIdx = props.indexOfFirst { it.name == "y" }
        val zIdx = props.indexOfFirst { it.name == "z" }
        if (xIdx < 0 || yIdx < 0 || zIdx < 0) return@use null

        if (fmtLower.contains("ascii")) {
            raf.seek(header.dataStartOffset)
            var readCount = 0
            while (readCount < header.vertexCount) {
                val line = raf.readLine() ?: return@use null
                val t = line.trim()
                if (t.isEmpty()) continue
                val parts = t.split(Regex("\\s+"))
                if (parts.size < props.size) continue
                val x = parts.getOrNull(xIdx)?.toFloatOrNull()
                val y = parts.getOrNull(yIdx)?.toFloatOrNull()
                val z = parts.getOrNull(zIdx)?.toFloatOrNull()
                if (x != null && y != null && z != null) readCount++
            }
            val out = ArrayList<IntArray>(header.faceCount)
            for (i in 0 until header.faceCount) {
                val line = raf.readLine() ?: break
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size < 4) continue
                val n = parts[0].toIntOrNull() ?: 0
                if (n < 3) continue
                val a = parts[1].toIntOrNull() ?: continue
                val b = parts[2].toIntOrNull() ?: continue
                val c = parts[3].toIntOrNull() ?: continue
                out.add(intArrayOf(a, b, c))
            }
            return@use if (out.isEmpty()) null else normalizePlyFaceIndicesIfNeeded(out, header.vertexCount)
        }

        if (fmtLower.contains("binary_little_endian") || fmtLower.contains("binary_big_endian")) {
            val order = if (header.littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            val stride = props.sumOf { typeSizeBytes(it.type) }
            raf.seek(header.dataStartOffset + header.vertexCount * stride.toLong())
            val out = ArrayList<IntArray>(header.faceCount)
            for (i in 0 until header.faceCount) {
                val nByte = raf.read()
                if (nByte < 0) break
                val n = nByte and 0xFF
                if (n < 3) {
                    raf.skipBytes(n * 4)
                    continue
                }
                val buf = ByteArray(12)
                raf.readFully(buf)
                val bb = ByteBuffer.wrap(buf).order(order)
                val a = bb.int
                val b = bb.int
                val c = bb.int
                out.add(intArrayOf(a, b, c))
                if (n > 3) {
                    raf.skipBytes((n - 3) * 4)
                }
            }
            return@use if (out.isEmpty()) null else normalizePlyFaceIndicesIfNeeded(out, header.vertexCount)
        }
        null
    }
}

private fun parseObjFaceVertexToken(tok: String): Int {
    val s = tok.substringBefore('/').trim()
    return s.toIntOrNull() ?: 0
}

private fun resolveObjVertexIndex(tok: String, vertexCount: Int): Int? {
    val idx = parseObjFaceVertexToken(tok)
    if (idx == 0) return null
    val zeroBased = if (idx > 0) idx - 1 else vertexCount + idx
    return if (zeroBased in 0 until vertexCount) zeroBased else null
}

/** OBJ 정점색 토큰(0~1 또는 0~255) → 0~1 */
private fun objColorTokenTo01(v: Float): Float =
    if (v > 1.5f) (v / 255f).coerceIn(0f, 1f) else v.coerceIn(0f, 1f)

/** OBJ 숫자 토큰(쉼표 소수점·지역 형식 대비) */
private fun parseObjFloatToken(tok: String): Float? =
    tok.trim().replace(',', '.').toFloatOrNull()

/** POINTS: OBJ 꼭짓점만 있을 때 점 클라우드 / TRIANGLES: STL 등 삼각형 메쉬 */
enum class MeshDrawMode {
    POINTS,
    TRIANGLES
}

data class ObjParseResult(
    val points: FloatArray,
    val count: Int,
    val drawMode: MeshDrawMode = MeshDrawMode.POINTS,
    /** 렌더링되는 각 정점의 RGB (0~1), 길이 = count * 3 */
    val vertexColors: FloatArray? = null
)

private data class ObjMaterialColor(
    val r: Float,
    val g: Float,
    val b: Float
)

private data class ObjFacePrimitive(
    val indices: IntArray,
    val materialColor: ObjMaterialColor?
)

private fun parseMtlFile(file: File): Map<String, ObjMaterialColor> {
    if (!file.exists() || !file.isFile) return emptyMap()
    val out = LinkedHashMap<String, ObjMaterialColor>()
    var currentName: String? = null
    file.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
        lines.forEach { line ->
            val t = line.trim().trimStart('\uFEFF')
            if (t.isEmpty() || t.startsWith("#")) return@forEach
            val parts = t.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.isEmpty()) return@forEach
            when (parts[0].lowercase()) {
                "newmtl" -> currentName = parts.getOrNull(1)
                "kd", "ka" -> {
                    val name = currentName ?: return@forEach
                    if (parts.size < 4) return@forEach
                    val r = (parseObjFloatToken(parts[1]) ?: 0f).coerceIn(0f, 1f)
                    val g = (parseObjFloatToken(parts[2]) ?: 0f).coerceIn(0f, 1f)
                    val b = (parseObjFloatToken(parts[3]) ?: 0f).coerceIn(0f, 1f)
                    out[name] = ObjMaterialColor(r, g, b)
                }
            }
        }
    }
    return out
}

private fun loadObjMaterialLibraries(objFile: File, names: List<String>): Map<String, ObjMaterialColor> {
    val merged = LinkedHashMap<String, ObjMaterialColor>()
    val baseDir = objFile.parentFile ?: return merged
    for (name in names) {
        if (name.isBlank()) continue
        val mtlFile = File(baseDir, name)
        merged.putAll(parseMtlFile(mtlFile))
    }
    return merged
}

fun parseObjVertices(file: File): ObjParseResult? {
    if (!file.exists()) return null
    return try {
        val raw = parseObjVerticesUnbounded(file) ?: return null
        try {
            downsamplePreviewMesh(raw)
        } catch (t: Throwable) {
            t.printStackTrace()
            raw
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

private fun parseObjVerticesUnbounded(file: File): ObjParseResult? {
    return try {
        val rawPos = ArrayList<Float>(1024)
        val rawCol = ArrayList<Float>(1024)
        var hasEmbeddedVertexColor = false
        val materials = LinkedHashMap<String, ObjMaterialColor>()
        /** `p` 프리미티브: 정점 인덱스(박싱 최소화). 색은 [pointRgbFlat]에 정점당 3 float로 병렬 저장 */
        val pointVertexIndices = ArrayList<Int>(4096)
        val pointRgbFlat = ArrayList<Float>(4096)
        val facePrimitives = ArrayList<ObjFacePrimitive>(256)
        var currentMaterialName: String? = null
        file.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val t = line.trim().trimStart('\uFEFF')
                if (t.isEmpty() || t.startsWith("#")) return@forEach
                val parts = t.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (parts.isEmpty()) return@forEach
                val head = parts[0].lowercase()
                when (head) {
                    "mtllib" -> materials.putAll(loadObjMaterialLibraries(file, parts.drop(1)))
                    "usemtl" -> currentMaterialName = parts.getOrNull(1)
                    "v" -> {
                        if (parts.size >= 7) {
                            hasEmbeddedVertexColor = true
                            rawPos.add(parseObjFloatToken(parts[1]) ?: 0f)
                            rawPos.add(parseObjFloatToken(parts[2]) ?: 0f)
                            rawPos.add(parseObjFloatToken(parts[3]) ?: 0f)
                            rawCol.add(objColorTokenTo01(parseObjFloatToken(parts[4]) ?: 0f))
                            rawCol.add(objColorTokenTo01(parseObjFloatToken(parts[5]) ?: 0f))
                            rawCol.add(objColorTokenTo01(parseObjFloatToken(parts[6]) ?: 0f))
                        } else if (parts.size >= 4) {
                            rawPos.add(parseObjFloatToken(parts[1]) ?: 0f)
                            rawPos.add(parseObjFloatToken(parts[2]) ?: 0f)
                            rawPos.add(parseObjFloatToken(parts[3]) ?: 0f)
                        }
                    }
                    "p" -> {
                        val material = currentMaterialName?.let { materials[it] }
                        val vertexCount = rawPos.size / 3
                        for (tok in parts.drop(1)) {
                            val index = resolveObjVertexIndex(tok, vertexCount) ?: continue
                            pointVertexIndices.add(index)
                            when {
                                material != null -> {
                                    pointRgbFlat.add(material.r)
                                    pointRgbFlat.add(material.g)
                                    pointRgbFlat.add(material.b)
                                }
                                hasEmbeddedVertexColor && rawCol.size == vertexCount * 3 -> {
                                    pointRgbFlat.add(rawCol[index * 3])
                                    pointRgbFlat.add(rawCol[index * 3 + 1])
                                    pointRgbFlat.add(rawCol[index * 3 + 2])
                                }
                                else -> {
                                    pointRgbFlat.add(0.70f)
                                    pointRgbFlat.add(0.72f)
                                    pointRgbFlat.add(0.75f)
                                }
                            }
                        }
                    }
                    "f" -> {
                        if (parts.size < 4) return@forEach
                        val vertexCount = rawPos.size / 3
                        val indices = parts.drop(1)
                            .mapNotNull { resolveObjVertexIndex(it, vertexCount) }
                        if (indices.size < 3) return@forEach
                        val material = currentMaterialName?.let { materials[it] }
                        for (i in 1 until indices.size - 1) {
                            facePrimitives.add(
                                ObjFacePrimitive(
                                    intArrayOf(indices[0], indices[i], indices[i + 1]),
                                    material
                                )
                            )
                        }
                    }
                }
            }
        }
        val count = rawPos.size / 3
        if (count <= 0) return null
        val points = FloatArray(count * 3)
        for (i in 0 until count * 3) points[i] = rawPos[i]
        val normalized = normalizeMeshPoints(points)

        if (facePrimitives.isNotEmpty()) {
            val triList = ArrayList<Float>(facePrimitives.size * 9)
            val colList = ArrayList<Float>(facePrimitives.size * 9)
            for (primitive in facePrimitives) {
                val tri = primitive.indices
                if (tri[0] !in 0 until count || tri[1] !in 0 until count || tri[2] !in 0 until count) {
                    continue
                }
                for (vi in 0..2) {
                    val vid = tri[vi]
                    triList.add(normalized[vid * 3])
                    triList.add(normalized[vid * 3 + 1])
                    triList.add(normalized[vid * 3 + 2])
                    when {
                        primitive.materialColor != null -> {
                            colList.add(primitive.materialColor.r)
                            colList.add(primitive.materialColor.g)
                            colList.add(primitive.materialColor.b)
                        }
                        hasEmbeddedVertexColor && rawCol.size == count * 3 -> {
                            colList.add(rawCol[vid * 3])
                            colList.add(rawCol[vid * 3 + 1])
                            colList.add(rawCol[vid * 3 + 2])
                        }
                        else -> {
                            colList.add(0.70f)
                            colList.add(0.72f)
                            colList.add(0.75f)
                        }
                    }
                }
            }
            if (triList.isEmpty()) {
                // 유효한 face가 하나도 없으면 point/vertex fallback으로 이어집니다.
            } else {
                val triCount = triList.size / 3
                val tv = FloatArray(triList.size) { triList[it] }
                val tc = FloatArray(colList.size) { colList[it] }
                return ObjParseResult(tv, triCount, MeshDrawMode.TRIANGLES, tc)
            }
        }

        if (pointVertexIndices.isNotEmpty()) {
            val nPts = pointVertexIndices.size
            if (pointRgbFlat.size != nPts * 3) {
                while (pointRgbFlat.size < nPts * 3) {
                    pointRgbFlat.add(0.70f)
                    pointRgbFlat.add(0.72f)
                    pointRgbFlat.add(0.75f)
                }
            }
            val pointList = ArrayList<Float>(nPts * 3)
            val pointColorsOut = ArrayList<Float>(nPts * 3)
            for (i in 0 until nPts) {
                val vid = pointVertexIndices[i]
                if (vid !in 0 until count) continue
                pointList.add(normalized[vid * 3])
                pointList.add(normalized[vid * 3 + 1])
                pointList.add(normalized[vid * 3 + 2])
                pointColorsOut.add(pointRgbFlat[i * 3])
                pointColorsOut.add(pointRgbFlat[i * 3 + 1])
                pointColorsOut.add(pointRgbFlat[i * 3 + 2])
            }
            if (pointList.isNotEmpty()) {
                val pv = FloatArray(pointList.size) { pointList[it] }
                val pc = FloatArray(pointColorsOut.size) { pointColorsOut[it] }
                return ObjParseResult(pv, pointList.size / 3, MeshDrawMode.POINTS, pc)
            }
        }

        val colors = if (hasEmbeddedVertexColor && rawCol.size == count * 3) {
            FloatArray(count * 3) { i -> rawCol[i] }
        } else null
        ObjParseResult(normalized, count, MeshDrawMode.POINTS, colors)
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

/**
 * STL(바이너리/ASCII)에서 삼각형 꼭짓점을 읽어 [ObjParseResult]로 반환합니다.
 * 좌표는 화면에 맞게 중심 정렬·스케일합니다.
 */
fun parseStlVertices(file: File): ObjParseResult? {
    if (!file.exists() || file.length() < 15L) return null
    return try {
        val bytes = file.readBytes()
        // 1) 텍스트 STL: "solid" + "facet" 등 — 바이너리로 잘못 읽으면 깨짐
        if (looksLikeAsciiStl(bytes)) {
            parseStlAscii(String(bytes, StandardCharsets.UTF_8))?.let { return it }
        }
        // 2) 바이너리 STL (헤더·파일 길이 불일치·끝 패딩 허용) — OpenSCAD 등이 내보낸 큰 메쉬에 흔함
        parseStlBinaryRelaxed(bytes)?.let { return it }
        // 3) 판별 실패 시 마지막으로 ASCII 재시도
        parseStlAscii(String(bytes, StandardCharsets.UTF_8))
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun looksLikeAsciiStl(bytes: ByteArray): Boolean {
    if (bytes.isEmpty()) return false
    var i = 0
    if (bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
    ) {
        i = 3
    }
    while (i < bytes.size && bytes[i].toInt().toChar().isWhitespace()) i++
    if (i + 5 > bytes.size) return false
    val head = String(bytes, i, minOf(80, bytes.size - i), StandardCharsets.US_ASCII)
    if (!head.trimStart().startsWith("solid", ignoreCase = true)) return false
    val sampleLen = minOf(bytes.size, 65_536)
    val sample = String(bytes, 0, sampleLen, StandardCharsets.US_ASCII)
    return sample.contains("facet", ignoreCase = true) && sample.contains("vertex", ignoreCase = true)
}

/**
 * 바이너리 STL: 헤더의 삼각형 개수와 실제 파일 길이가 다를 수 있음(패딩·메타데이터).
 * 실제로 읽을 수 있는 삼각형 수 = min(헤더값, (바이트-84)/50).
 */
private fun parseStlBinaryRelaxed(bytes: ByteArray): ObjParseResult? {
    if (bytes.size < 84) return null
    val triHeader = ByteBuffer.wrap(bytes, 80, 4).order(ByteOrder.LITTLE_ENDIAN).int
    val maxTriBySize = (bytes.size - 84) / 50
    if (maxTriBySize < 1) return null
    val triToRead = when {
        triHeader in 1 until 50_000_000 -> minOf(triHeader, maxTriBySize)
        else -> maxTriBySize
    }
    if (triToRead < 1) return null
    return parseStlBinaryExact(bytes, triToRead)
}

private fun parseStlBinaryExact(bytes: ByteArray, triCount: Int): ObjParseResult? {
    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val out = FloatArray(triCount * 9)
    var o = 0
    var off = 84
    repeat(triCount) {
        if (off + 50 > bytes.size) return null
        bb.position(off + 12)
        repeat(3) {
            repeat(3) {
                val v = bb.float
                if (!v.isFinite()) return null
                out[o++] = v
            }
        }
        off += 50
    }
    val vertexCount = triCount * 3
    val normalized = normalizeMeshPoints(out)
    return ObjParseResult(normalized, vertexCount, MeshDrawMode.TRIANGLES, null)
}

private fun parseStlAscii(text: String): ObjParseResult? {
    val raw = ArrayList<Float>(1024)
    val regex = Regex("""vertex\s+(\S+)\s+(\S+)\s+(\S+)""", RegexOption.IGNORE_CASE)
    for (m in regex.findAll(text)) {
        val x = m.groupValues[1].toFloatOrNull() ?: continue
        val y = m.groupValues[2].toFloatOrNull() ?: continue
        val z = m.groupValues[3].toFloatOrNull() ?: continue
        raw.add(x); raw.add(y); raw.add(z)
    }
    if (raw.size < 9) return null
    val arr = FloatArray(raw.size) { i -> raw[i] }
    val normalized = normalizeMeshPoints(arr)
    val count = normalized.size / 3
    return ObjParseResult(normalized, count, MeshDrawMode.TRIANGLES, null)
}

/** 메쉬를 대략 -1~1 범위로 맞춤 (기존 PLY 정규화와 동일 개념) */
private fun normalizeMeshPoints(points: FloatArray): FloatArray {
    val count = points.size / 3
    if (count <= 0) return points
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var minZ = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var maxZ = -Float.MAX_VALUE
    for (i in 0 until count) {
        val x = points[i * 3]
        val y = points[i * 3 + 1]
        val z = points[i * 3 + 2]
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) continue
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (z < minZ) minZ = z
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
        if (z > maxZ) maxZ = z
    }
    if (minX > maxX || minY > maxY || minZ > maxZ) return points
    val cx = (minX + maxX) / 2f
    val cy = (minY + maxY) / 2f
    val cz = (minZ + maxZ) / 2f
    val maxDim = max(maxX - minX, max(maxY - minY, maxZ - minZ))
    val half = if (maxDim > 0f && maxDim.isFinite()) maxDim / 2f else 1f
    val result = FloatArray(points.size)
    for (i in 0 until count) {
        result[i * 3] = (points[i * 3] - cx) / half
        result[i * 3 + 1] = (points[i * 3 + 1] - cy) / half
        result[i * 3 + 2] = (points[i * 3 + 2] - cz) / half
    }
    return result
}

private class ObjRenderer : GLSurfaceView.Renderer {
    private var programPoints = 0
    private var aPosPoints = 0
    private var uMvpPoints = 0

    private var programTri = 0
    private var aPosTri = 0
    private var uMvpTri = 0

    private var programTriColor = 0
    private var aPosTriC = 0
    private var aColorTriC = 0
    private var uMvpTriC = 0

    private var programPointsColor = 0
    private var aPosPointsC = 0
    private var aColorPointsC = 0
    private var uMvpPointsC = 0

    private var pointBuffer: FloatBuffer? = null
    private var vertexCount = 0
    private var aspectRatio = 1f
    private var drawTriangles = false
    private var triUseVertexColor = false
    private var pointsUseVertexColor = false

    private var rotationX = 0f
    private var rotationY = 0f

    /** 카메라 z 거리 (작을수록 확대). lookAt eye (0,0,z) */
    private var cameraDistance = 3f
    private val minCameraDistance = 1.05f
    private val maxCameraDistance = 28f

    fun setPoints(
        points: FloatArray,
        count: Int,
        drawMode: MeshDrawMode = MeshDrawMode.POINTS,
        vertexColors: FloatArray? = null
    ) {
        rotationX = 0f
        rotationY = 0f
        cameraDistance = 3f
        if (count <= 0 || points.size < count * 3) {
            pointBuffer = null
            vertexCount = 0
            drawTriangles = false
            triUseVertexColor = false
            pointsUseVertexColor = false
            return
        }
        drawTriangles = drawMode == MeshDrawMode.TRIANGLES
        triUseVertexColor = drawTriangles &&
            vertexColors != null &&
            vertexColors.size == count * 3
        pointsUseVertexColor = !drawTriangles &&
            vertexColors != null &&
            vertexColors.size == count * 3

        try {
            if (triUseVertexColor) {
                val bb = ByteBuffer.allocateDirect(count * 6 * 4).order(ByteOrder.nativeOrder())
                val fb = bb.asFloatBuffer()
                for (i in 0 until count) {
                    fb.put(points[i * 3])
                    fb.put(points[i * 3 + 1])
                    fb.put(points[i * 3 + 2])
                    fb.put(vertexColors!![i * 3])
                    fb.put(vertexColors[i * 3 + 1])
                    fb.put(vertexColors[i * 3 + 2])
                }
                fb.position(0)
                pointBuffer = fb
            } else if (pointsUseVertexColor) {
                val bb = ByteBuffer.allocateDirect(count * 6 * 4).order(ByteOrder.nativeOrder())
                val fb = bb.asFloatBuffer()
                for (i in 0 until count) {
                    fb.put(points[i * 3])
                    fb.put(points[i * 3 + 1])
                    fb.put(points[i * 3 + 2])
                    fb.put(vertexColors!![i * 3])
                    fb.put(vertexColors[i * 3 + 1])
                    fb.put(vertexColors[i * 3 + 2])
                }
                fb.position(0)
                pointBuffer = fb
            } else {
                val bb = ByteBuffer.allocateDirect(count * 3 * 4).order(ByteOrder.nativeOrder())
                val fb = bb.asFloatBuffer()
                for (i in 0 until count * 3) {
                    fb.put(points[i])
                }
                fb.position(0)
                pointBuffer = fb
            }
            vertexCount = count
        } catch (oom: OutOfMemoryError) {
            android.util.Log.e("ObjViewer", "setPoints OOM (vertices=$count)", oom)
            pointBuffer = null
            vertexCount = 0
            drawTriangles = false
            triUseVertexColor = false
            pointsUseVertexColor = false
        }
    }

    fun addRotation(dx: Float, dy: Float) {
        rotationY += dx
        rotationX += dy
    }

    /** 핀치 줌: scaleFactor > 1 이면 손가락 벌림 → 가까이(확대) */
    fun applyZoomScaleFactor(scaleFactor: Float) {
        if (scaleFactor <= 0f || !scaleFactor.isFinite()) return
        cameraDistance = (cameraDistance / scaleFactor).coerceIn(minCameraDistance, maxCameraDistance)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        programPoints = createProgram(VERTEX_SHADER_POINTS, FRAGMENT_SHADER_POINTS)
        aPosPoints = GLES20.glGetAttribLocation(programPoints, "aPos")
        uMvpPoints = GLES20.glGetUniformLocation(programPoints, "uMvp")

        programTri = createProgram(VERTEX_SHADER_TRI, FRAGMENT_SHADER_TRI)
        aPosTri = GLES20.glGetAttribLocation(programTri, "aPos")
        uMvpTri = GLES20.glGetUniformLocation(programTri, "uMvp")

        programTriColor = createProgram(VERTEX_SHADER_TRI_COLOR, FRAGMENT_SHADER_TRI_COLOR)
        aPosTriC = GLES20.glGetAttribLocation(programTriColor, "aPos")
        aColorTriC = GLES20.glGetAttribLocation(programTriColor, "aColor")
        uMvpTriC = GLES20.glGetUniformLocation(programTriColor, "uMvp")

        programPointsColor = createProgram(VERTEX_SHADER_POINTS_COLOR, FRAGMENT_SHADER_POINTS_COLOR)
        aPosPointsC = GLES20.glGetAttribLocation(programPointsColor, "aPos")
        aColorPointsC = GLES20.glGetAttribLocation(programPointsColor, "aColor")
        uMvpPointsC = GLES20.glGetUniformLocation(programPointsColor, "uMvp")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspectRatio = if (height == 0) 1f else width.toFloat() / height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        val buffer = pointBuffer ?: return
        if (vertexCount <= 0) return

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        val proj = FloatArray(16)
        val view = FloatArray(16)
        val model = FloatArray(16)
        val mvp = FloatArray(16)

        GLMatrix.perspectiveM(proj, 0, 45f, aspectRatio, 0.1f, 100f)
        GLMatrix.setLookAtM(view, 0, 0f, 0f, cameraDistance, 0f, 0f, 0f, 0f, 1f, 0f)
        GLMatrix.setIdentityM(model, 0)
        GLMatrix.rotateM(model, 0, rotationX, 1f, 0f, 0f)
        GLMatrix.rotateM(model, 0, rotationY, 0f, 1f, 0f)
        GLMatrix.multiplyMM(mvp, 0, view, 0, model, 0)
        GLMatrix.multiplyMM(mvp, 0, proj, 0, mvp, 0)

        if (drawTriangles && triUseVertexColor) {
            val stride = 6 * 4
            GLES20.glUseProgram(programTriColor)
            GLES20.glUniformMatrix4fv(uMvpTriC, 1, false, mvp, 0)
            buffer.position(0)
            GLES20.glEnableVertexAttribArray(aPosTriC)
            GLES20.glVertexAttribPointer(aPosTriC, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(3)
            GLES20.glEnableVertexAttribArray(aColorTriC)
            GLES20.glVertexAttribPointer(aColorTriC, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(aPosTriC)
            GLES20.glDisableVertexAttribArray(aColorTriC)
        } else if (drawTriangles) {
            GLES20.glUseProgram(programTri)
            GLES20.glUniformMatrix4fv(uMvpTri, 1, false, mvp, 0)
            GLES20.glEnableVertexAttribArray(aPosTri)
            GLES20.glVertexAttribPointer(aPosTri, 3, GLES20.GL_FLOAT, false, 0, buffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(aPosTri)
        } else if (pointsUseVertexColor) {
            val stride = 6 * 4
            GLES20.glUseProgram(programPointsColor)
            GLES20.glUniformMatrix4fv(uMvpPointsC, 1, false, mvp, 0)
            buffer.position(0)
            GLES20.glEnableVertexAttribArray(aPosPointsC)
            GLES20.glVertexAttribPointer(aPosPointsC, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(3)
            GLES20.glEnableVertexAttribArray(aColorPointsC)
            GLES20.glVertexAttribPointer(aColorPointsC, 3, GLES20.GL_FLOAT, false, stride, buffer)
            buffer.position(0)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(aPosPointsC)
            GLES20.glDisableVertexAttribArray(aColorPointsC)
        } else {
            GLES20.glUseProgram(programPoints)
            GLES20.glUniformMatrix4fv(uMvpPoints, 1, false, mvp, 0)
            GLES20.glEnableVertexAttribArray(aPosPoints)
            GLES20.glVertexAttribPointer(aPosPoints, 3, GLES20.GL_FLOAT, false, 0, buffer)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(aPosPoints)
        }
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vs: String, fs: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        return program
    }

    companion object {
        private const val VERTEX_SHADER_POINTS = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                gl_PointSize = 4.0;
            }
        """

        private const val FRAGMENT_SHADER_POINTS = """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(0.70, 0.72, 0.75, 1.0);
            }
        """

        private const val VERTEX_SHADER_POINTS_COLOR = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            attribute vec3 aColor;
            varying vec3 vColor;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                gl_PointSize = 4.0;
                vColor = aColor;
            }
        """

        private const val FRAGMENT_SHADER_POINTS_COLOR = """
            precision mediump float;
            varying vec3 vColor;
            void main() {
                gl_FragColor = vec4(clamp(vColor, 0.0, 1.0), 1.0);
            }
        """

        /** 삼각형 면 (STL) — 정점 색 없을 때: 중성 재질색 + 위치 기반 가벼운 음영(단색 연두 제거) */
        private const val VERTEX_SHADER_TRI = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            varying float vLit;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                float len2 = dot(aPos, aPos);
                vec3 n = len2 > 1e-8 ? aPos * inversesqrt(len2) : vec3(0.0, 0.0, 1.0);
                vec3 L = normalize(vec3(0.4, 0.7, 0.55));
                vLit = 0.38 + 0.62 * max(dot(n, L), 0.0);
            }
        """

        private const val FRAGMENT_SHADER_TRI = """
            precision mediump float;
            varying float vLit;
            void main() {
                vec3 base = vec3(0.70, 0.72, 0.76);
                gl_FragColor = vec4(base * vLit, 1.0);
            }
        """

        private const val VERTEX_SHADER_TRI_COLOR = """
            uniform mat4 uMvp;
            attribute vec3 aPos;
            attribute vec3 aColor;
            varying vec3 vColor;
            varying float vLit;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                vColor = aColor;
                float len2 = dot(aPos, aPos);
                vec3 n = len2 > 1e-8 ? aPos * inversesqrt(len2) : vec3(0.0, 0.0, 1.0);
                vec3 L = normalize(vec3(0.4, 0.7, 0.55));
                vLit = 0.45 + 0.55 * max(dot(n, L), 0.0);
            }
        """

        private const val FRAGMENT_SHADER_TRI_COLOR = """
            precision mediump float;
            varying vec3 vColor;
            varying float vLit;
            void main() {
                gl_FragColor = vec4(vColor * vLit, 1.0);
            }
        """
    }
}

class ObjSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer = ObjRenderer()
    private var lastX = 0f
    private var lastY = 0f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderer.applyZoomScaleFactor(detector.scaleFactor)
                return true
            }
        }
    )

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun loadModel(file: File) {
        Thread {
            try {
                val parsed = parseObjVertices(file)
                if (parsed != null) {
                    queueEvent {
                        renderer.setPoints(parsed.points, parsed.count, parsed.drawMode, parsed.vertexColors)
                        requestRender()
                    }
                }
            } catch (t: Throwable) {
                android.util.Log.e("ObjViewer", "loadModel failed: ${file.name}", t)
            }
        }.start()
    }

    /** STL / OBJ — 확장자에 따라 파싱. [onLoaded]는 메쉬 적용(또는 파싱 실패) 후 메인 스레드에서 호출 */
    fun loadMeshFile(file: File, onLoaded: (() -> Unit)? = null) {
        val main = Handler(Looper.getMainLooper())
        Thread {
            try {
                val parsed = when {
                    file.extension.equals("stl", true) -> parseStlVertices(file)
                    file.extension.equals("ply", true) -> loadColoredPlyMesh(file)
                    file.extension.equals("obj", true) -> parseObjVertices(file)
                    else -> parseObjVertices(file)
                }
                if (parsed != null) {
                    queueEvent {
                        renderer.setPoints(parsed.points, parsed.count, parsed.drawMode, parsed.vertexColors)
                        main.post { onLoaded?.invoke() }
                    }
                } else {
                    main.post { onLoaded?.invoke() }
                }
            } catch (t: Throwable) {
                android.util.Log.e("ObjViewer", "loadMeshFile failed: ${file.name}", t)
                main.post { onLoaded?.invoke() }
            }
        }.start()
    }

    /** 이미 파싱된 메쉬를 GL 스레드에 적용 (Compose에서 IO 후 전달할 때 사용) */
    fun applyParsedMesh(parsed: ObjParseResult) {
        queueEvent {
            try {
                renderer.setPoints(parsed.points, parsed.count, parsed.drawMode, parsed.vertexColors)
            } catch (t: Throwable) {
                android.util.Log.e("ObjViewer", "applyParsedMesh", t)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    renderer.addRotation(dx * 0.5f, dy * 0.5f)
                    lastX = event.x
                    lastY = event.y
                }
            }
        }
        return true
    }
}

