package com.allai.app.model

enum class Role { User, Agent, Tool, System }

data class Message(
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrls: List<String> = emptyList()
)

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

data class StreamChunk(
    val content: String,
    val done: Boolean = false,
    val toolCalls: List<ToolCall> = emptyList(),
    val error: String? = null
)

data class LlmProvider(
    val name: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val isOpenAiCompatible: Boolean = true
)

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String = "",
    val required: Boolean = false,
    val enumValues: List<String> = emptyList()
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList(),
    val strict: Boolean = false,
    val confirmationRequired: Boolean = false,
    val permissionRequired: String? = null
)
