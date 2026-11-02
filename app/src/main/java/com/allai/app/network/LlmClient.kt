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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LlmClient {
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
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

    suspend fun fetchModels(provider: LlmProvider): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = provider.baseUrl.takeIf { it.endsWith("/") } ?: provider.baseUrl + "/"
                val modelsUrl = baseUrl + "models"
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
                    val parsed = JsonParser.parseString(json).asJsonObject
                    val data = parsed.getAsJsonArray("data")
                    if (data == null) emptyList()
                    else {
                        data.map { it.asJsonObject.get("id").asString }
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun sendOpenAiCompatible(
        provider: LlmProvider,
        messages: List<Message>,
        tools: List<String>,
        temperature: Double
    ): String {
        val url = if (provider.baseUrl.endsWith("/chat/completions")) {
            provider.baseUrl
        } else {
            provider.baseUrl.ifEmpty { "https://api.openai.com/v1/chat/completions" }
                .takeIf { it.endsWith("/") }?.plus("chat/completions") ?: "$provider.baseUrl/chat/completions"
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
}
