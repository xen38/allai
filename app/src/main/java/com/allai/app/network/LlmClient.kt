package com.allai.app.network

import com.google.gson.Gson
import java.io.IOException
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LlmClient {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    data class ChatRequest(val model: String, val messages: List<ChatMessage>)
    data class ChatMessage(val role: String, val content: String)
    data class ChatResponse(val choices: List<Choice>)
    data class Choice(val message: ChatMessage)

    suspend fun sendMessage(messages: List<com.allai.app.model.Message>): String {
        return withContext(Dispatchers.IO) {
            val apiKey = "TODO_API_KEY" // inject from Settings
            val baseUrl = "https://api.openai.com/v1/chat/completions"
            val body = ChatRequest(
                model = "gpt-4o-mini",
                messages = messages.map { ChatMessage(if (it.role == com.allai.app.model.Role.User) "user" else "assistant", it.text) }
            )
            val requestBody = RequestBody.create(null, gson.toJson(body))
            val request = Request.Builder()
                .url(baseUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json; charset=utf-8")
                .post(requestBody)
                .build()
            try {
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: ""
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $json")
                val parsed = gson.fromJson(json, ChatResponse::class.java)
                parsed.choices.firstOrNull()?.message?.content ?: "No response"
            } catch (e: Exception) {
                throw e
            }
        }
    }
}
