package com.example.app_01

import java.util.UUID

/** 지속 저장용 단일 메시지 */
data class PersistedMessage(
    val text: String,
    val isUser: Boolean,
    val imageUriStrings: List<String> = emptyList()
)

/**
 * 한 번의 대화 흐름을 나타내는 스레드.
 * [modeName]: "CLAUDE" or "AI_CAD"
 */
data class ConversationThread(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val modeName: String,
    val messages: List<PersistedMessage>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
