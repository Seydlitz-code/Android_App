package com.example.app_01

import android.util.Log

/**
 * AI CAD: LLM이 만든 OpenSCAD를 WASM 렌더 전에 한 번에 정리합니다.
 *
 * 설계 원칙
 * - [OpenCadSanitizer]의 펜스/마크다운 제거·color 제거는 그대로 사용
 * - LLM이 자주 내는 **주석 없는 영어/한글 설명 한 줄**은 OpenSCAD에서 구문 오류 → `//`로 승격
 * - 파이프라인 진입점은 [prepareForRender] 하나로 통일
 */
object AiCadScadPreprocessor {
    private const val TAG = "AiCadScadPreprocessor"

    /**
     * WASM·검증 단계에 넘기기 직전 소스.
     * 순서: sanitize(펜스) → **망가진 설명 줄 보정** → stripColor(WASM)
     */
    fun prepareForRender(raw: String): String {
        val fenced = OpenCadSanitizer.sanitizeForScad(raw)
        val fixed = commentOrphanDescriptionLines(fenced)
        val out = OpenCadSanitizer.stripColorForWasm(fixed).trim()
        if (fixed != fenced) {
            Log.i(TAG, "설명 줄을 주석으로 보정했습니다.")
        }
        return out
    }

    /**
     * `= ; ( { [` 등 구조 문자가 없고, OpenSCAD 문이 아닌 것으로 보이는 줄을 `//` 처리합니다.
     * 예: `including bezel`, `전체 두께` 처럼 설명만 덩그러니 있는 경우
     */
    fun commentOrphanDescriptionLines(scad: String): String {
        val lines = scad.lines()
        val out = ArrayList<String>(lines.size)
        for (line in lines) {
            val t = line.trim()
            if (t.isEmpty()) {
                out.add(line)
                continue
            }
            if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")) {
                out.add(line)
                continue
            }
            if (hasStructuralOpenScadChars(t)) {
                out.add(line)
                continue
            }
            if (t.startsWith("$")) {
                out.add(line)
                continue
            }
            if (t.startsWith("#")) {
                val u = t.uppercase()
                if (u.startsWith("#INCLUDE") || u.startsWith("#ASSERT")) {
                    out.add(line)
                    continue
                }
                if ("(" in t || "[" in t) {
                    out.add(line)
                    continue
                }
            }
            if (t.startsWith("include ", ignoreCase = true) ||
                t.startsWith("use <", ignoreCase = true) ||
                t.startsWith("use\"", ignoreCase = true)
            ) {
                out.add(line)
                continue
            }
            if (t.startsWith("module ", ignoreCase = true) ||
                t.startsWith("function ", ignoreCase = true)
            ) {
                out.add(line)
                continue
            }
            // 한글만/한글+공백 위주 → 설명으로 간주
            if (looksLikeHangulDescription(t)) {
                out.add(indentPreservingComment(line, t))
                continue
            }
            // 영어 단어 2개 이상 나열(예: including bezel) — 설명으로 간주
            if (looksLikeEnglishProseLine(t)) {
                out.add(indentPreservingComment(line, t))
                continue
            }
            out.add(line)
        }
        return out.joinToString("\n")
    }

    private fun indentPreservingComment(originalLine: String, trimmed: String): String {
        val idx = originalLine.indexOf(trimmed)
        val prefix = if (idx > 0) originalLine.substring(0, idx) else ""
        return "$prefix// $trimmed"
    }

    private fun hasStructuralOpenScadChars(t: String): Boolean {
        for (c in t) {
            when (c) {
                '=', ';', '(', '{', '[', ']', '}' -> return true
            }
            if (c == ')' && t.length > 1) return true
        }
        return false
    }

    private fun looksLikeHangulDescription(t: String): Boolean {
        var hangul = 0
        var otherLetter = 0
        for (ch in t) {
            if (ch in '\uAC00'..'\uD7A3') hangul++
            else if (ch.isLetter()) otherLetter++
        }
        if (hangul == 0) return false
        return hangul >= otherLetter
    }

    /** `including bezel`처럼 변수/호출이 아닌 영어 설명(단어 2개 이상, 연산자 없음) */
    private fun looksLikeEnglishProseLine(t: String): Boolean {
        if (t.any { it in '\uAC00'..'\uD7A3' }) return false
        val noSemi = t.removeSuffix(";").trim()
        val parts = noSemi.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return false
        for (p in parts) {
            if (!p.matches(Regex("^[a-zA-Z][a-zA-Z0-9_-]*$"))) return false
        }
        return true
    }
}
