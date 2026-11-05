package com.allai.app.network

import com.allai.app.model.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LlmClient {
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String> = emptyList(),
        temperature: Double = 0.7
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = if (provider.isOpenAiCompatible) {
                    sendOpenAiCompatible(provider, messages, tools, temperature)
                } else {
                    sendGeneric(provider, messages, tools, temperature)
                }
                response
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun sendMessageStreaming(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String> = emptyList(),
        temperature: Double = 0.7,
        onChunk: (String) -> Unit
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = if (provider.isOpenAiCompatible) {
                    sendOpenAiCompatibleStreaming(provider, messages, tools, temperature, onChunk)
                } else {
                    sendGenericStreaming(provider, messages, tools, temperature, onChunk)
                }
                response
            } catch (e: Exception) {
                throw e
            }
        }
    }

    fun sendMessageStreamingFlow(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String> = emptyList(),
        temperature: Double = 0.7
    ): Flow<String> = callbackFlow {
        val scope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.Job()
        )
        scope.launch {
            try {
                if (provider.isOpenAiCompatible) {
                    sendOpenAiCompatibleStreaming(
                        provider = provider,
                        messages = messages,
                        tools = tools,
                        temperature = temperature,
                        onChunk = { chunk ->
                            try { trySend(chunk) } catch (e: Exception) { }
                        }
                    )
                } else {
                    sendGenericStreaming(
                        provider = provider,
                        messages = messages,
                        tools = tools,
                        temperature = temperature,
                        onChunk = { chunk ->
                            try { trySend(chunk) } catch (e: Exception) { }
                        }
                    )
                }
            } catch (e: Exception) {
                close(e)
            }
        }
    }

    suspend fun fetchModels(provider: LlmProvider): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val base = provider.baseUrl.trim()
                val baseUrl = base.takeIf { it.endsWith("/") } ?: base + "/"
                val modelsUrl = when {
                    provider.name.equals("openai", ignoreCase = true) -> baseUrl + "models"
                    provider.name.equals("anthropic", ignoreCase = true) -> baseUrl
                    provider.name.equals("google", ignoreCase = true) -> baseUrl
                    else -> baseUrl + "models"
                }
                val request = Request.Builder()
                    .url(modelsUrl)
                    .header("Authorization", "Bearer ${provider.apiKey}")
                    .header("Content-Type", "application/json")
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    emptyList()
                } else {
                    val json = response.body?.string() ?: ""
                    when {
                        provider.name.equals("anthropic", ignoreCase = true) -> {
                            parseAnthropicModels(json)
                        }
                        provider.name.equals("google", ignoreCase = true) -> {
                            parseGoogleModels(json)
                        }
                        else -> {
                            parseOpenAiModels(json)
                        }
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun parseOpenAiModels(json: String): List<String> {
        val parsed = JsonParser.parseString(json).asJsonObject
        val data = parsed.getAsJsonArray("data")
        return if (data == null) emptyList()
        else data.map { it.asJsonObject.get("id").asString }
    }

    private suspend fun parseAnthropicModels(json: String): List<String> {
        val parsed = JsonParser.parseString(json).asJsonObject
        val data = parsed.getAsJsonArray("data")
        return if (data == null) emptyList()
        else data.map { it.asJsonObject.get("id").asString }
    }

    private suspend fun parseGoogleModels(json: String): List<String> {
        val parsed = JsonParser.parseString(json).asJsonObject
        val models = parsed.getAsJsonArray("models")
        return if (models == null) emptyList()
        else models.map { it.asJsonObject.get("name").asString.removePrefix("models/") }
    }

    private suspend fun sendOpenAiCompatible(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double
    ): String {
        val base = provider.baseUrl.trim()
        val url = when {
            base.isEmpty() -> "https://api.openai.com/v1/chat/completions"
            base.endsWith("/chat/completions") -> base
            base.endsWith("/") -> base + "chat/completions"
            else -> base + "/chat/completions"
        }

        val jsonMessages = messages.map { msg ->
            JsonObject().apply {
                addProperty("role", when (msg.role) {
                    Role.System -> "system"
                    Role.User -> "user"
                    Role.Agent -> "assistant"
                    Role.Tool -> "tool"
                })
                addProperty("content", msg.text)
            }
        }

        val body = JsonObject().apply {
            addProperty("model", provider.model)
            add("messages", gson.toJsonTree(jsonMessages))
            addProperty("temperature", temperature)
            addProperty("stream", false)
            if (tools.isNotEmpty()) {
                add("tools", gson.toJsonTree(tools.map { toolName ->
                    JsonObject().apply {
                        addProperty("type", "function")
                        add("function", JsonObject().apply {
                            addProperty("name", toolName)
                            addProperty("description", "Available tool: $toolName")
                            add("parameters", JsonObject().apply {
                                addProperty("type", "object")
                                add("properties", JsonObject())
                                addProperty("additionalProperties", true)
                            })
                        })
                    }
                }))
                addProperty("tool_choice", "auto")
            }
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), gson.toJson(body))
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${provider.apiKey}")
            .header("Content-Type", "application/json; charset=utf-8")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: ""
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $json")

        val parsed = JsonParser.parseString(json).asJsonObject
        val choices = parsed.getAsJsonArray("choices")
        if (choices == null || choices.size() == 0) throw IOException("No choices in response")

        val message = choices[0].asJsonObject.getAsJsonObject("message")
        val content = message.get("content")?.asString ?: ""
        val toolCalls = message.getAsJsonArray("tool_calls")

        if (toolCalls != null && toolCalls.size() > 0) {
            val results = mutableListOf<String>()
            for (tc in toolCalls) {
                val tcObj = tc.asJsonObject
                val func = tcObj.getAsJsonObject("function")
                val name = func.get("name").asString
                val argsStr = func.get("arguments").asString
                results.add("TOOL_CALL:$name:$argsStr")
            }
            return results.joinToString("\n")
        }

        return content
    }

    private suspend fun sendGeneric(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double
    ): String {
        return sendOpenAiCompatible(provider, messages, tools, temperature)
    }

    private suspend fun sendOpenAiCompatibleStreaming(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double,
        onChunk: (String) -> Unit
    ): String {
        val base = provider.baseUrl.trim()
        val url = when {
            base.isEmpty() -> "https://api.openai.com/v1/chat/completions"
            base.endsWith("/chat/completions") -> base
            base.endsWith("/") -> base + "chat/completions"
            else -> base + "/chat/completions"
        }

        val jsonMessages = messages.map { msg ->
            JsonObject().apply {
                addProperty("role", when (msg.role) {
                    Role.System -> "system"
                    Role.User -> "user"
                    Role.Agent -> "assistant"
                    Role.Tool -> "tool"
                })
                addProperty("content", msg.text)
            }
        }

        val body = JsonObject().apply {
            addProperty("model", provider.model)
            add("messages", gson.toJsonTree(jsonMessages))
            addProperty("temperature", temperature)
            addProperty("stream", true)
            if (tools.isNotEmpty()) {
                add("tools", gson.toJsonTree(tools.map { toolName ->
                    JsonObject().apply {
                        addProperty("type", "function")
                        add("function", JsonObject().apply {
                            addProperty("name", toolName)
                            addProperty("description", "Available tool: $toolName")
                            add("parameters", JsonObject().apply {
                                addProperty("type", "object")
                                add("properties", JsonObject())
                                addProperty("additionalProperties", true)
                            })
                        })
                    }
                }))
                addProperty("tool_choice", "auto")
            }
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), gson.toJson(body))
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${provider.apiKey}")
            .header("Content-Type", "application/json; charset=utf-8")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            throw IOException("HTTP ${response.code}: $errorBody")
        }

        val responseBody = response.body
        if (responseBody == null) throw IOException("Empty response body")

        var fullContent = ""
        val toolCalls = mutableListOf<ToolCall>()

        try {
            responseBody.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8LineStrict() ?: continue
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || !trimmed.startsWith("data:")) continue

                    val data = trimmed.substringAfter("data:").trim()
                    if (data == "[DONE]") break

                    try {
                        val parsed = JsonParser.parseString(data).asJsonObject
                        val choices = parsed.getAsJsonArray("choices")
                        if (choices == null || choices.size() == 0) continue

                        val delta = choices[0].asJsonObject.getAsJsonObject("delta")
                        val content = delta.get("content")?.asString ?: ""
                        if (content.isNotEmpty()) {
                            fullContent += content
                            onChunk(content)
                        }

                        val tcArray = delta.getAsJsonArray("tool_calls")
                        if (tcArray != null && tcArray.size() > 0) {
                            for (tc in tcArray) {
                                val tcObj = tc.asJsonObject
                                val index = tcObj.get("index").asInt
                                val func = tcObj.getAsJsonObject("function")
                                val name = func.get("name")?.asString ?: ""
                                val args = func.get("arguments")?.asString ?: ""

                                while (toolCalls.size <= index) {
                                    toolCalls.add(ToolCall(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = "",
                                        arguments = emptyMap()
                                    ))
                                }
                                toolCalls[index] = toolCalls[index].copy(
                                    name = if (name.isNotEmpty()) name else toolCalls[index].name,
                                    arguments = if (args.isNotEmpty()) {
                                        toolCalls[index].arguments + mapOf("__partial_args_$index" to args)
                                    } else toolCalls[index].arguments
                                )
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        } catch (e: IOException) {
            if (fullContent.isEmpty()) throw e
        }

        if (toolCalls.isNotEmpty()) {
            val results = mutableListOf<String>()
            for (tc in toolCalls) {
                if (tc.name.isNotEmpty()) {
                    val argsStr = tc.arguments.entries.joinToString(", ") { entry -> "${entry.key}=${entry.value}" }
                    results.add("TOOL_CALL:${tc.name}:$argsStr")
                }
            }
            return results.joinToString("\n")
        }

        return fullContent
    }

    private suspend fun sendGenericStreaming(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double,
        onChunk: (String) -> Unit
    ): String {
        return sendOpenAiCompatibleStreaming(provider, messages, tools, temperature, onChunk)
    }
}