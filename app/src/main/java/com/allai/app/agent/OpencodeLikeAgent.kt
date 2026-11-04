package com.allai.app.agent

import com.allai.app.model.LlmProvider
import com.allai.app.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpencodeLikeAgent(private val provider: LlmProvider) {
    private val loop = AgentLoop()

    suspend fun run(messages: List<Message>): String {
        return withContext(Dispatchers.Default) {
            val config = AgentLoop.AgentConfig(
                provider = provider,
                systemPrompt = AgentLoop.DEFAULT_SYSTEM_PROMPT
            )
            val reply = loop.run(config, messages)
            reply
        }
    }
}
