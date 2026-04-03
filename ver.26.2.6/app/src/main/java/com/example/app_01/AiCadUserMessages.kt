package com.example.app_01

/**
 * AI CAD 저장/렌더 실패 시 토스트·로그용 문구 정리.
 * 숫자만 있는 메시지(내부 코드 등)는 사용자에게 읽기 쉬운 설명을 덧붙입니다.
 */
fun Throwable.userMessageForAiCad(): String {
    val raw = message?.trim().orEmpty()
    if (raw.isEmpty()) {
        return localizedMessage?.takeIf { it.isNotBlank() }
            ?: (javaClass.simpleName + if (cause != null) ": ${cause?.message}" else "")
    }
    if (raw.matches(Regex("^\\d{5,}$"))) {
        return "OpenSCAD 처리 중 오류(내부 코드: $raw). " +
            "영어/한글 설명은 반드시 // 주석으로만 적고, 변수는 `이름 = 값;` 형태인지 확인하세요."
    }
    return raw.take(900)
}
