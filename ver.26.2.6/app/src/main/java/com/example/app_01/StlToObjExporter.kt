package com.example.app_01

import java.io.File

/**
 * 바이너리 STL → Wavefront OBJ (다이어그램: 포맷 변환 .obj).
 */
object StlToObjExporter {

    fun writeObjFromBinaryStl(stlBytes: ByteArray, outFile: File): Result<Unit> = runCatching {
        val positions = StlToGlbConverter.readBinaryStlTrianglePositions(stlBytes)
            ?: throw IllegalArgumentException("STL 파싱 실패")
        val processed = if (AiCadMeshPostProcessor.shouldApply(positions)) {
            AiCadMeshPostProcessor.centerAndNormalizeExtent(positions, targetMaxExtent = 2f)
        } else {
            positions
        }
        outFile.parentFile?.mkdirs()
        outFile.bufferedWriter(Charsets.UTF_8).use { w ->
            w.appendLine("# Generated from STL")
            w.appendLine("o mesh")
            var i = 0
            while (i < processed.size) {
                w.append("v ")
                w.append(processed[i].toString())
                w.append(' ')
                w.append(processed[i + 1].toString())
                w.append(' ')
                w.appendLine(processed[i + 2].toString())
                i += 3
            }
            val triCount = processed.size / 9
            repeat(triCount) { t ->
                val a = t * 3 + 1
                val b = t * 3 + 2
                val c = t * 3 + 3
                w.append("f ")
                w.append(a.toString()); w.append(' ')
                w.append(b.toString()); w.append(' ')
                w.appendLine(c.toString())
            }
        }
    }
}
