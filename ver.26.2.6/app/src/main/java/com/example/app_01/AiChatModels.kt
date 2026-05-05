package com.example.app_01

import android.net.Uri

/** AI 탭(3DGS 분석)으로 넘겨 자동 전송할 페이로드 */
data class Pending3dgsServerAutoSend(
    val nonce: Long,
    val promptText: String,
    val imageUris: List<Uri>,
    /**
     * true: AI 탭으로 전환 후 현재 채팅 UI에서 전송.
     * false: 라이브러리 등 다른 화면 유지, 백그라운드에서 새 대화 스레드로 분석만 저장.
     */
    val switchToAiTab: Boolean = true,
    /** 서버 task와 매칭해 동일 작업에 대한 LLM 중복 실행을 피한다 */
    val sourceServerTaskId: String? = null,
)
