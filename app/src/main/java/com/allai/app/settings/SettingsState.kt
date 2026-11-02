package com.allai.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.allai.app.model.LlmProvider

class SettingsState(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("allai_settings", Context.MODE_PRIVATE)

    var providerName: String
        get() = prefs.getString("provider_name", "openai") ?: "openai"
        set(value) = prefs.edit().putString("provider_name", value).apply()

    var apiKey: String
        get() = prefs.getString("api_key", "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") ?: "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"
        set(value) = prefs.edit().putString("api_key", value).apply()

    var model: String
        get() = prefs.getString("model", "meta/llama-3.1-8b-instruct") ?: "meta/llama-3.1-8b-instruct"
        set(value) = prefs.edit().putString("model", value).apply()

    var baseUrl: String
        get() = prefs.getString("base_url", "https://integrate.api.nvidia.com/v1/chat/completions") ?: "https://integrate.api.nvidia.com/v1/chat/completions"
        set(value) = prefs.edit().putString("base_url", value).apply()

    var systemPrompt: String
        get() = prefs.getString("system_prompt", DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    val provider: LlmProvider
        get() = LlmProvider(
            name = providerName,
            baseUrl = baseUrl,
            model = model,
            apiKey = apiKey,
            isOpenAiCompatible = providerName != "anthropic" && providerName != "google"
        )

    fun save(
        providerName: String,
        apiKey: String,
        model: String,
        baseUrl: String,
        systemPrompt: String
    ) {
        this.providerName = providerName
        this.apiKey = apiKey
        this.model = model
        this.baseUrl = baseUrl
        this.systemPrompt = systemPrompt
    }

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
}
