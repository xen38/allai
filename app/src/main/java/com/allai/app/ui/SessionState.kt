package com.allai.app.ui

import com.allai.app.model.Message

data class SessionInfo(
    val id: String,
    val title: String,
    val messageCount: Int,
    val timestamp: Long,
    val messages: List<Message> = emptyList()
)

object SessionState {
    var sessions: MutableList<SessionInfo> = mutableListOf()
    var activeSessionId: String? = null

    fun loadFromPrefs(prefs: android.content.SharedPreferences, gson: com.google.gson.Gson): List<SessionInfo> {
        val json = prefs.getString("allai_sessions", "[]") ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<List<SessionInfo>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveToPrefs(prefs: android.content.SharedPreferences, gson: com.google.gson.Gson, sessions: List<SessionInfo>) {
        prefs.edit().putString("allai_sessions", gson.toJson(sessions)).apply()
    }

    fun getSession(id: String): SessionInfo? = sessions.firstOrNull { it.id == id }

    fun setMessages(id: String, messages: List<Message>) {
        val idx = sessions.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val current = sessions[idx]
            sessions[idx] = current.copy(
                messages = messages,
                messageCount = messages.size,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun loadMessages(id: String): List<Message> {
        activeSessionId = id
        return getSession(id)?.messages ?: emptyList()
    }

    fun createSession(id: String, title: String): SessionInfo {
        val s = SessionInfo(
            id = id,
            title = title,
            messageCount = 0,
            timestamp = System.currentTimeMillis(),
            messages = emptyList()
        )
        sessions.add(s)
        return s
    }
}
