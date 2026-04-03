package com.example.app_01

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** 대화 스레드 목록을 SharedPreferences + JSON 으로 영구 저장 */
object ChatThreadStorage {
    private const val PREFS = "chat_threads_v1"
    private const val KEY   = "threads_json"
    private val gson = Gson()

    fun loadAll(context: Context): List<ConversationThread> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ConversationThread>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, thread: ConversationThread) {
        val list = loadAll(context).toMutableList()
        val idx  = list.indexOfFirst { it.id == thread.id }
        if (idx >= 0) list[idx] = thread else list.add(thread)
        list.sortByDescending { it.updatedAt }
        persist(context, list)
    }

    fun delete(context: Context, threadId: String) {
        persist(context, loadAll(context).filter { it.id != threadId })
    }

    private fun persist(context: Context, threads: List<ConversationThread>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, gson.toJson(threads)).apply()
    }
}
