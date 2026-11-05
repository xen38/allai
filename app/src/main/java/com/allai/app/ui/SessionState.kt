package com.allai.app.ui

import com.allai.app.model.Message

data class SessionInfo(
    val id: String,
    val title: String,
    val messageCount: Int,
    val timestamp: Long,
    val messages: List<Message> = emptyList()
)

data class SessionCheckpoint(
    val id: String,
    val sessionId: String,
    val name: String,
    val timestamp: Long,
    val messages: List<Message>
)

object SessionState {
    var sessions: MutableList<SessionInfo> = mutableListOf()
    var activeSessionId: String? = null
    var checkpoints: MutableMap<String, MutableList<SessionCheckpoint>> = mutableMapOf()

    fun loadFromPrefs(prefs: android.content.SharedPreferences, gson: com.google.gson.Gson): List<SessionInfo> {
        val json = prefs.getString("allai_sessions", "[]") ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<List<SessionInfo>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun saveToPrefs(prefs: android.content.SharedPreferences, gson: com.google.gson.Gson, sessions: List<SessionInfo>) {
        prefs.edit().putString("allai_sessions", gson.toJson(sessions)).apply()
    }

    fun loadCheckpointsFromPrefs(prefs: android.content.SharedPreferences, gson: com.google.gson.Gson) {
        val json = prefs.getString("allai_checkpoints", "{}") ?: "{}"
        val mapType = object : com.google.gson.reflect.TypeToken<Map<String, List<SessionCheckpoint>>>() {}.type
        val map = gson.fromJson(json, mapType) ?: emptyMap<String, List<SessionCheckpoint>>()
        checkpoints.clear()
        for ((sessionId, cps) in map) {
            checkpoints[sessionId] = cps.toMutableList()
        }
    }

    fun saveCheckpointsToPrefs(prefs: android.content.SharedPreferences, gson: com.google.gson.Gson) {
        val json = gson.toJson(checkpoints)
        prefs.edit().putString("allai_checkpoints", json).apply()
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
        checkpoints[s.id] = mutableListOf()
        return s
    }

    fun createCheckpoint(sessionId: String, name: String): SessionCheckpoint {
        val session = getSession(sessionId) ?: throw IllegalArgumentException("Session not found: $sessionId")
        val checkpoint = SessionCheckpoint(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = sessionId,
            name = name,
            timestamp = System.currentTimeMillis(),
            messages = session.messages
        )
        checkpoints.getOrPut(sessionId) { mutableListOf() }.add(checkpoint)
        return checkpoint
    }

    fun restoreCheckpoint(sessionId: String, checkpointId: String): List<Message> {
        val checkpoint = checkpoints[sessionId]?.firstOrNull { it.id == checkpointId }
            ?: throw IllegalArgumentException("Checkpoint not found: $checkpointId")
        activeSessionId = sessionId
        setMessages(sessionId, checkpoint.messages)
        return checkpoint.messages
    }

    fun deleteCheckpoint(sessionId: String, checkpointId: String) {
        checkpoints[sessionId]?.removeIf { it.id == checkpointId }
    }

    fun getCheckpoints(sessionId: String): List<SessionCheckpoint> {
        return checkpoints[sessionId] ?: emptyList()
    }
}