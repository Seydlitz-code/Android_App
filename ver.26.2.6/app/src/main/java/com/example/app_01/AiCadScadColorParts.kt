package com.example.app_01

import kotlin.math.max

/**
 * OpenSCAD 소스에서 `color([r,g,b]) { ... }` 또는 `color([r,g,b]) primitive();` 형태를 찾아
 * 부위별로 분리합니다. (중첩 color는 지원하지 않음 — LLM은 형제 블록만 사용)
 */
object AiCadScadColorParts {

    data class ColorPart(val rgb: FloatArray, val bodyScad: String)

    private val colorRe = Regex(
        """color\s*\(\s*\[\s*([0-9.]+)\s*,\s*([0-9.]+)\s*,\s*([0-9.]+)\s*\]\s*\)""",
        RegexOption.IGNORE_CASE
    )

    fun extractFnPrefix(source: String): String? =
        source.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("\$fn") }

    fun extractColorParts(source: String): List<ColorPart> {
        val out = mutableListOf<ColorPart>()
        var searchStart = 0
        while (true) {
            val m = colorRe.find(source, searchStart) ?: break
            val r0 = m.groupValues[1].toFloat()
            val g0 = m.groupValues[2].toFloat()
            val b0 = m.groupValues[3].toFloat()
            val (r, g, b) = normalizeRgbTriplet(r0, g0, b0)
            val rgb = floatArrayOf(r, g, b)
            var i = m.range.last + 1
            while (i < source.length && source[i].isWhitespace()) i++
            if (i >= source.length) break
            val endBody: Int
            val body: String
            if (source[i] == '{') {
                val (inner, endIdx) = extractBalanced(source, i)
                body = inner.trim()
                endBody = endIdx
            } else {
                endBody = findEndOfStatement(source, i)
                body = source.substring(i, endBody).trim().trimEnd(';').trim()
            }
            if (body.isNotBlank()) {
                out.add(ColorPart(rgb, body))
            }
            searchStart = max(endBody, m.range.last + 1)
        }
        return out
    }

    private fun normalizeRgbTriplet(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        return if (r > 1f || g > 1f || b > 1f) {
            Triple(
                (r / 255f).coerceIn(0f, 1f),
                (g / 255f).coerceIn(0f, 1f),
                (b / 255f).coerceIn(0f, 1f)
            )
        } else {
            Triple(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        }
    }

    private fun extractBalanced(source: String, start: Int): Pair<String, Int> {
        require(source.getOrNull(start) == '{')
        var depth = 0
        var i = start
        while (i < source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return Pair(source.substring(start + 1, i).trim(), i + 1)
                    }
                }
            }
            i++
        }
        return Pair("", start + 1)
    }

    private fun findEndOfStatement(source: String, start: Int): Int {
        var paren = 0
        var brace = 0
        var i = start
        while (i < source.length) {
            when (source[i]) {
                '(' -> paren++
                ')' -> paren = max(0, paren - 1)
                '{' -> brace++
                '}' -> brace = max(0, brace - 1)
                ';' -> if (paren == 0 && brace == 0) return i + 1
            }
            i++
        }
        return source.length
    }

    fun wrapPartForScad(body: String, fnPrefix: String?): String {
        val b = body.trim()
        val sb = StringBuilder()
        fnPrefix?.takeIf { it.isNotBlank() }?.let { sb.appendLine(it) }
        sb.appendLine("union() {")
        sb.appendLine(b)
        sb.appendLine("}")
        return sb.toString()
    }
}
