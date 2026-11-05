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

    data class ConnectionResult(
        val success: Boolean,
        val error: String? = null,
        val models: List<String> = emptyList()
    )

    suspend fun sendMessage(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String> = emptyList(),
        temperature: Double = 0.7
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = when (provider.name.lowercase()) {
                    "anthropic" -> sendAnthropic(provider, messages, tools, temperature)
                    "google" -> sendGoogle(provider, messages, tools, temperature)
                    else -> sendOpenAiCompatible(provider, messages, tools, temperature)
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
                val response = when (provider.name.lowercase()) {
                    "anthropic" -> sendAnthropicStreaming(provider, messages, tools, temperature, onChunk)
                    "google" -> sendGoogleStreaming(provider, messages, tools, temperature, onChunk)
                    else -> sendOpenAiCompatibleStreaming(provider, messages, tools, temperature, onChunk)
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
                when (provider.name.lowercase()) {
                    "anthropic" -> {
                        sendAnthropicStreaming(
                            provider = provider,
                            messages = messages,
                            tools = tools,
                            temperature = temperature,
                            onChunk = { chunk -> try { trySend(chunk) } catch (e: Exception) { } }
                        )
                    }
                    "google" -> {
                        sendGoogleStreaming(
                            provider = provider,
                            messages = messages,
                            tools = tools,
                            temperature = temperature,
                            onChunk = { chunk -> try { trySend(chunk) } catch (e: Exception) { } }
                        )
                    }
                    else -> {
                        sendOpenAiCompatibleStreaming(
                            provider = provider,
                            messages = messages,
                            tools = tools,
                            temperature = temperature,
                            onChunk = { chunk -> try { trySend(chunk) } catch (e: Exception) { } }
                        )
                    }
                }
            } catch (e: Exception) {
                close(e)
            }
        }
    }

    suspend fun testConnection(provider: LlmProvider): ConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                val models = fetchModels(provider)
                if (models.isNotEmpty()) {
                    ConnectionResult(success = true, models = models)
                } else {
                    // Model discovery may not be supported, try a minimal request
                    val testResult = sendMessage(
                        provider = provider,
                        messages = listOf(Message(Role.User, "Hello")),
                        temperature = 0.1
                    )
                    if (testResult.isNotEmpty() && !testResult.startsWith("Error") && !testResult.startsWith("TOOL_CALL")) {
                        ConnectionResult(success = true)
                    } else {
                        ConnectionResult(success = false, error = "Connection test failed: unexpected response")
                    }
                }
            } catch (e: Exception) {
                ConnectionResult(success = false, error = e.message ?: "Unknown connection error")
            }
        }
    }

    suspend fun fetchModels(provider: LlmProvider): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val base = provider.baseUrl.trim()
                val baseUrl = base.takeIf { it.endsWith("/") } ?: base + "/"
                val modelsUrl = when (provider.name.lowercase()) {
                    "openai" -> baseUrl + "models"
                    "anthropic" -> baseUrl
                    "google" -> baseUrl
                    else -> baseUrl + "models"
                }

                val requestBuilder = Request.Builder()
                    .url(modelsUrl)
                    .header("Content-Type", "application/json")
                val request = when (provider.name.lowercase()) {
                    "anthropic" -> requestBuilder
                        .header("x-api-key", provider.apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .build()
                    "google" -> requestBuilder.build()
                    else -> requestBuilder
                        .header("Authorization", "Bearer ${provider.apiKey}")
                        .build()
                }

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    emptyList()
                } else {
                    val json = response.body?.string() ?: ""
                    when (provider.name.lowercase()) {
                        "anthropic" -> parseAnthropicModels(json)
                        "google" -> parseGoogleModels(json)
                        else -> parseOpenAiModels(json)
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

    // OpenAI-compatible provider (non-streaming)
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

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), body.toString())
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

    // OpenAI-compatible provider (streaming)
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

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), body.toString())
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
                                if (name.isNotEmpty()) {
                                    toolCalls[index] = toolCalls[index].copy(name = name)
                                }
                                if (args.isNotEmpty()) {
                                    try {
                                        val argsObj = JsonParser.parseString(args).asJsonObject
                                        val currentArgs = toolCalls[index].arguments.toMutableMap()
                                        argsObj.entrySet().forEach { entry ->
                                            currentArgs[entry.key] = when {
                                                entry.value.isJsonPrimitive -> entry.value.asString
                                                entry.value.isJsonArray -> entry.value.toString()
                                                entry.value.isJsonObject -> entry.value.toString()
                                                else -> entry.value.toString()
                                            }
                                        }
                                        toolCalls[index] = toolCalls[index].copy(arguments = currentArgs)
                                    } catch (e: Exception) {
                                        // Ignore partial arg parse errors
                                    }
                                }
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

    // Generic (falls back to OpenAI-compatible)
    private suspend fun sendGeneric(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double
    ): String {
        return sendOpenAiCompatible(provider, messages, tools, temperature)
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

    // Anthropic provider (non-streaming)
    private suspend fun sendAnthropic(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double
    ): String {
        val base = provider.baseUrl.trim()
        val baseUrl = base.takeIf { it.endsWith("/") } ?: base + "/"
        val url = when {
            base.isEmpty() || base.contains("anthropic") -> "https://api.anthropic.com/v1/messages"
            base.endsWith("/messages") -> base
            base.endsWith("/") -> base + "messages"
            else -> base + "/messages"
        }

        val systemMsg = messages.filter { it.role == Role.System }.map { it.text }.firstOrNull()
        val userMsgs = messages.filter { it.role != Role.System }

        val jsonMessages = userMsgs.map { msg ->
            JsonObject().apply {
                addProperty("role", when (msg.role) {
                    Role.User -> "user"
                    Role.Agent -> "assistant"
                    Role.Tool -> "user"
                    else -> "user"
                })
                addProperty("content", msg.text)
            }
        }

        val body = JsonObject().apply {
            addProperty("model", provider.model)
            addProperty("max_tokens", 4096)
            addProperty("temperature", temperature)
            add("messages", gson.toJsonTree(jsonMessages))
            if (systemMsg?.isNotEmpty() == true) {
                addProperty("system", systemMsg)
            }
            if (tools.isNotEmpty()) {
                add("tools", gson.toJsonTree(tools.map { toolName ->
                    JsonObject().apply {
                        addProperty("name", toolName)
                        addProperty("description", "Available tool: $toolName")
                        add("input_schema", JsonObject().apply {
                            addProperty("type", "object")
                            add("properties", JsonObject())
                            addProperty("additionalProperties", true)
                        })
                    }
                }))
            }
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), body.toString())
        val request = Request.Builder()
            .url(url)
            .header("x-api-key", provider.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json; charset=utf-8")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: ""
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $json")

        val parsed = JsonParser.parseString(json).asJsonObject
        val contentArray = parsed.getAsJsonArray("content")
        if (contentArray == null || contentArray.size() == 0) throw IOException("No content in response")

        val textParts = mutableListOf<String>()
        val toolCalls = mutableListOf<String>()

        for (block in contentArray) {
            val blockObj = block.asJsonObject
            val type = blockObj.get("type").asString
            when (type) {
                "text" -> {
                    textParts.add(blockObj.get("text")?.asString ?: "")
                }
                "tool_use" -> {
                    val name = blockObj.get("name").asString
                    val input = blockObj.getAsJsonObject("input")
                    toolCalls.add("TOOL_CALL:$name:${gson.toJson(input)}")
                }
            }
        }

        val result = mutableListOf<String>()
        result.addAll(textParts)
        result.addAll(toolCalls)
        return result.joinToString("\n")
    }

    // Anthropic provider (streaming)
    private suspend fun sendAnthropicStreaming(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double,
        onChunk: (String) -> Unit
    ): String {
        val base = provider.baseUrl.trim()
        val baseUrl = base.takeIf { it.endsWith("/") } ?: base + "/"
        val url = when {
            base.isEmpty() || base.contains("anthropic") -> "https://api.anthropic.com/v1/messages"
            base.endsWith("/messages") -> base
            base.endsWith("/") -> base + "messages"
            else -> base + "/messages"
        }

        val systemMsg = messages.filter { it.role == Role.System }.map { it.text }.firstOrNull()
        val userMsgs = messages.filter { it.role != Role.System }

        val jsonMessages = userMsgs.map { msg ->
            JsonObject().apply {
                addProperty("role", when (msg.role) {
                    Role.User -> "user"
                    Role.Agent -> "assistant"
                    Role.Tool -> "user"
                    else -> "user"
                })
                addProperty("content", msg.text)
            }
        }

        val body = JsonObject().apply {
            addProperty("model", provider.model)
            addProperty("max_tokens", 4096)
            addProperty("temperature", temperature)
            addProperty("stream", true)
            add("messages", gson.toJsonTree(jsonMessages))
            if (systemMsg?.isNotEmpty() == true) {
                addProperty("system", systemMsg)
            }
            if (tools.isNotEmpty()) {
                add("tools", gson.toJsonTree(tools.map { toolName ->
                    JsonObject().apply {
                        addProperty("name", toolName)
                        addProperty("description", "Available tool: $toolName")
                        add("input_schema", JsonObject().apply {
                            addProperty("type", "object")
                            add("properties", JsonObject())
                            addProperty("additionalProperties", true)
                        })
                    }
                }))
            }
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), body.toString())
        val request = Request.Builder()
            .url(url)
            .header("x-api-key", provider.apiKey)
            .header("anthropic-version", "2023-06-01")
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
                        val eventType = parsed.get("type")?.asString ?: ""

                        when (eventType) {
                            "content_block_delta" -> {
                                val delta = parsed.getAsJsonObject("delta")
                                if (delta != null) {
                                    val deltaType = delta.get("type")?.asString ?: ""
                                    if (deltaType == "text_delta") {
                                        val text = delta.get("text")?.asString ?: ""
                                        if (text.isNotEmpty()) {
                                            fullContent += text
                                            onChunk(text)
                                        }
                                    }
                                }
                            }
                            "message_delta" -> {
                                // Message ended
                            }
                            else -> { /* ignore other event types */ }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        } catch (e: IOException) {
            if (fullContent.isEmpty()) throw e
        }

        return fullContent
    }

    // Google provider (non-streaming)
    private suspend fun sendGoogle(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double
    ): String {
        val base = provider.baseUrl.trim()
        val baseUrl = base.takeIf { it.endsWith("/") } ?: base + "/"
        val url = when {
            base.isEmpty() || base.contains("googleapis") -> "https://generativelanguage.googleapis.com/v1beta/models/${provider.model}:generateContent?key=${provider.apiKey}"
            base.endsWith("/") -> base + "${provider.model}:generateContent?key=${provider.apiKey}"
            else -> base + "/${provider.model}:generateContent?key=${provider.apiKey}"
        }

        val systemMsg = messages.filter { it.role == Role.System }.map { it.text }.firstOrNull()
        val userMsgs = messages.filter { it.role != Role.System }

        val jsonMessages = userMsgs.map { msg ->
            JsonObject().apply {
                addProperty("role", when (msg.role) {
                    Role.User -> "user"
                    Role.Agent -> "model"
                    Role.Tool -> "user"
                    else -> "user"
                })
                add("parts", gson.toJsonTree(listOf(mapOf("text" to msg.text))))
            }
        }

        val body = JsonObject().apply {
            add("contents", gson.toJsonTree(jsonMessages))
            if (systemMsg?.isNotEmpty() == true) {
                add("systemInstruction", JsonObject().apply {
                    add("parts", gson.toJsonTree(listOf(mapOf("text" to systemMsg))))
                })
            }
            addProperty("temperature", temperature)
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), body.toString())
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json; charset=utf-8")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: ""
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $json")

        val parsed = JsonParser.parseString(json).asJsonObject
        val candidates = parsed.getAsJsonArray("candidates")
        if (candidates == null || candidates.size() == 0) throw IOException("No candidates in response")

        val content = candidates[0].asJsonObject.getAsJsonObject("content")
        val parts = content.getAsJsonArray("parts")
        if (parts == null || parts.size() == 0) throw IOException("No parts in response")

        val text = parts[0].asJsonObject.get("text")?.asString ?: ""
        return text
    }

    // Google provider (streaming)
    private suspend fun sendGoogleStreaming(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double,
        onChunk: (String) -> Unit
    ): String {
        // Google streaming via SSE - similar to non-streaming but with stream flag
        val base = provider.baseUrl.trim()
        val baseUrl = base.takeIf { it.endsWith("/") } ?: base + "/"
        val url = when {
            base.isEmpty() || base.contains("googleapis") -> "https://generativelanguage.googleapis.com/v1beta/models/${provider.model}:streamGenerateContent?key=${provider.apiKey}"
            base.endsWith("/") -> base + "${provider.model}:streamGenerateContent?key=${provider.apiKey}"
            else -> base + "/${provider.model}:streamGenerateContent?key=${provider.apiKey}"
        }

        val systemMsg = messages.filter { it.role == Role.System }.map { it.text }.firstOrNull()
        val userMsgs = messages.filter { it.role != Role.System }

        val jsonMessages = userMsgs.map { msg ->
            JsonObject().apply {
                addProperty("role", when (msg.role) {
                    Role.User -> "user"
                    Role.Agent -> "model"
                    Role.Tool -> "user"
                    else -> "user"
                })
                add("parts", gson.toJsonTree(listOf(mapOf("text" to msg.text))))
            }
        }

        val body = JsonObject().apply {
            add("contents", gson.toJsonTree(jsonMessages))
            if (systemMsg?.isNotEmpty() == true) {
                add("systemInstruction", JsonObject().apply {
                    add("parts", gson.toJsonTree(listOf(mapOf("text" to systemMsg))))
                })
            }
            addProperty("temperature", temperature)
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), body.toString())
        val request = Request.Builder()
            .url(url)
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
                        val candidates = parsed.getAsJsonArray("candidates")
                        if (candidates == null || candidates.size() == 0) continue

                        val content = candidates[0].asJsonObject.getAsJsonObject("content")
                        val parts = content.getAsJsonArray("parts")
                        if (parts == null || parts.size() == 0) continue

                        val text = parts[0].asJsonObject.get("text")?.asString ?: ""
                        if (text.isNotEmpty()) {
                            fullContent += text
                            onChunk(text)
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
            }
        } catch (e: IOException) {
            if (fullContent.isEmpty()) throw e
        }

        return fullContent
    }
}