package com.example.app_01

/**
 * LLM 출력 OpenSCAD 코드 검증 (파이프라인: 코드 검증).
 * OpenSCAD 파서는 WASM 단계에서 재검증되므로 여기서는 휴리스틱만 수행합니다.
 */
object AiCadCodeVerifier {

    fun verify(raw: String): Result<String> {
        val code = raw.trim().removePrefix("\uFEFF").trim()
        if (code.length < 6) {
            return Result.failure(IllegalArgumentException("코드가 너무 짧습니다."))
        }
        if (!basicBracketBalance(code)) {
            return Result.failure(IllegalArgumentException("괄호 { } ( ) [ ] 균형이 맞지 않습니다."))
        }
        return Result.success(code)
    }

    private fun basicBracketBalance(s: String): Boolean {
        var depthCurly = 0
        var depthRound = 0
        var depthSquare = 0
        var inLineComment = false
        var inBlockComment = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val next = s.getOrNull(i + 1)
            if (inLineComment) {
                if (c == '\n') inLineComment = false
                i++
                continue
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false
                    i += 2
                    continue
                }
                i++
                continue
            }
            if (c == '/' && next == '/') {
                inLineComment = true
                i += 2
                continue
            }
            if (c == '/' && next == '*') {
                inBlockComment = true
                i += 2
                continue
            }
            when (c) {
                '{' -> depthCurly++
                '}' -> depthCurly--
                '(' -> depthRound++
                ')' -> depthRound--
                '[' -> depthSquare++
                ']' -> depthSquare--
            }
            if (depthCurly < 0 || depthRound < 0 || depthSquare < 0) return false
            i++
        }
        return depthCurly == 0 && depthRound == 0 && depthSquare == 0
    }
}
