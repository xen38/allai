package com.allai.app.agent

import com.allai.app.model.*
import com.allai.app.network.LlmClient
import com.allai.app.plugin.PluginManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgentLoop {
    private val llm = LlmClient

    data class AgentConfig(
        val provider: LlmProvider,
        val maxToolIterations: Int = 10,
        val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
    )

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """You are Allai, an AI agent running natively on an Android device. You have access to tools that let you interact with the phone and run commands.

Available tools are provided in the conversation. When you need to use a tool, respond with exactly:
TOOL_CALL:tool_name:{"arg1": "value1", "arg2": "value2"}

After receiving a tool result, continue reasoning or provide your final answer.

Guidelines:
- Be helpful, concise, and accurate
- Use tools when needed to complete tasks on the device
- If a tool fails, explain the error and try an alternative approach
- Never make up tool results
- When you have enough information, provide your final answer directly"""
    }

    suspend fun run(config: AgentConfig, messages: List<Message>): String {
        return withContext(Dispatchers.Default) {
            var currentMessages = messages.toMutableList()
            var iteration = 0

            while (iteration < config.maxToolIterations) {
                iteration++

                val allTools = PluginManager.toolDefinitions()
                val response = llm.sendMessage(
                    provider = config.provider,
                    messages = currentMessages,
                    tools = allTools,
                    temperature = 0.7
                )

                val toolCalls = parseToolCalls(response)
                if (toolCalls.isEmpty()) {
                    currentMessages.add(Message(Role.Agent, response))
                    return@withContext response
                }

                currentMessages.add(Message(Role.Agent, response))

                for (toolCall in toolCalls) {
                    currentMessages.add(Message(Role.Tool, "", System.currentTimeMillis()))
                    val result = PluginManager.run(toolCall.name, toolCall.arguments)
                    val resultText = if (result.ok) result.output else "ERROR: ${result.error}"
                    currentMessages.add(Message(Role.Tool, "TOOL_RESULT:${toolCall.name}:$resultText"))
                }
            }

            val finalResponse = llm.sendMessage(
                provider = config.provider,
                messages = currentMessages + Message(Role.User, "Please provide your final answer based on the tool results above."),
                tools = emptyList(),
                temperature = 0.5
            )
            return@withContext finalResponse
        }
    }

    suspend fun runStreaming(
        config: AgentConfig,
        messages: List<Message>,
        onChunk: (String) -> Unit
    ): String {
        return withContext(Dispatchers.Default) {
            var currentMessages = messages.toMutableList()
            var iteration = 0

            while (iteration < config.maxToolIterations) {
                iteration++

                val allTools = PluginManager.toolDefinitions()
                val response = llm.sendMessageStreaming(
                    provider = config.provider,
                    messages = currentMessages,
                    tools = allTools,
                    temperature = 0.7,
                    onChunk = onChunk
                )

                val toolCalls = parseToolCalls(response)
                if (toolCalls.isEmpty()) {
                    currentMessages.add(Message(Role.Agent, response))
                    return@withContext response
                }

                currentMessages.add(Message(Role.Agent, response))

                for (toolCall in toolCalls) {
                    currentMessages.add(Message(Role.Tool, "", System.currentTimeMillis()))
                    val result = PluginManager.run(toolCall.name, toolCall.arguments)
                    val resultText = if (result.ok) result.output else "ERROR: ${result.error}"
                    currentMessages.add(Message(Role.Tool, "TOOL_RESULT:${toolCall.name}:$resultText"))
                }
            }

            val finalResponse = llm.sendMessage(
                provider = config.provider,
                messages = currentMessages + Message(Role.User, "Please provide your final answer based on the tool results above."),
                tools = emptyList(),
                temperature = 0.5
            )
            onChunk(finalResponse)
            return@withContext finalResponse
        }
    }

    private fun parseToolCalls(response: String): List<ToolCall> {
        val calls = mutableListOf<ToolCall>()
        val lines = response.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("TOOL_CALL:")) {
                val rest = trimmed.substringAfter("TOOL_CALL:")
                val colonIdx = rest.indexOf(':')
                if (colonIdx > 0) {
                    val name = rest.substring(0, colonIdx)
                    val argsStr = rest.substring(colonIdx + 1)
                    val args = try {
                        val obj = com.google.gson.JsonParser.parseString(argsStr).asJsonObject
                        obj.entrySet().associate { entry ->
                            val value = when {
                                entry.value.isJsonPrimitive -> entry.value.asJsonPrimitive.asString
                                entry.value.isJsonArray -> entry.value.asJsonArray.toString()
                                entry.value.isJsonObject -> entry.value.asJsonObject.toString()
                                else -> entry.value.toString()
                            }
                            entry.key to value
                        }
                    } catch (e: Exception) {
                        mapOf("raw" to argsStr)
                    }
                    calls.add(ToolCall(id = java.util.UUID.randomUUID().toString(), name = name, arguments = args))
                }
            }
        }
        return calls
    }
}