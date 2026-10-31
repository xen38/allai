package com.allai.app.model

enum class Role { User, Agent, Tool }

data class Message(val role: Role, val text: String, val timestamp: Long = System.currentTimeMillis())

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>,
    val status: ToolStatus = ToolStatus.Pending
)

enum class ToolStatus { Pending, Running, Completed, Failed }

data class ToolResult(
    val callId: String,
    val output: String,
    val error: String = "",
    val ok: Boolean = true
)
