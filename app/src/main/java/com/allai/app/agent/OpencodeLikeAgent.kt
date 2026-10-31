package com.allai.app

import com.allai.app.model.Message
import com.allai.app.model.Role

class OpencodeLikeAgent {
    fun run(messages: List<Message>): String {
        // Stub agent loop: echo with tool-use scaffolding.
        val last = messages.lastOrNull()?.text ?: ""
        return "Allai agent received: '$last'. Tool-use and MCP loops are stubbed — wire LlmClient + PluginManager here."
    }
}
