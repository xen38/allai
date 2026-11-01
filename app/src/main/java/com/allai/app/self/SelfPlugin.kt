package com.allai.app.self

import android.content.Context
import com.allai.app.model.ToolResult
import com.allai.app.plugin.PhonePlugin
import com.allai.app.memory.MemoryStore
import com.allai.app.model.Memory

class SelfPlugin(private val context: Context) : PhonePlugin {
    override val name = "self"
    override val description = "Interact with Allai's own systems: memories, settings, skills."

    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: return ToolResult("", "", "Missing 'action' argument", false)
        return when (action) {
            "read_memories" -> {
                val store = MemoryStore.getInstance(context)
                val memories = store.getMemories()
                ToolResult(callId = "", output = memories.joinToString("\n") { "${it.title}: ${it.description}" }, ok = true)
            }
            "add_memory" -> {
                val title = arguments["title"] as? String ?: return ToolResult("", "", "Missing 'title' argument", false)
                val desc = arguments["description"] as? String ?: return ToolResult("", "", "Missing 'description' argument", false)
                val actName = arguments["action_name"] as? String ?: "self"
                val store = MemoryStore.getInstance(context)
                store.addMemory(
                    Memory(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        description = desc,
                        action = actName,
                        arguments = arguments.toString()
                    )
                )
                ToolResult(callId = "", output = "Memory added: $title", ok = true)
            }
            "learn_action" -> {
                val name = arguments["name"] as? String ?: return ToolResult("", "", "Missing 'name' argument", false)
                val desc = arguments["description"] as? String ?: return ToolResult("", "", "Missing 'description' argument", false)
                val tool = arguments["tool"] as? String ?: return ToolResult("", "", "Missing 'tool' argument", false)
                val store = MemoryStore.getInstance(context)
                val typedArgs: MutableMap<String, Any> = mutableMapOf()
                val rawArgs = arguments["arguments"]
                if (rawArgs is Map<*, *>) {
                    for ((k, v) in rawArgs) {
                        typedArgs[k.toString()] = v!!
                    }
                }
                store.recordAction(name, desc, tool, typedArgs)
                ToolResult(callId = "", output = "Learned action: $name", ok = true)
            }
            "get_learned_actions" -> {
                val store = MemoryStore.getInstance(context)
                val actions = store.getLearnedActions()
                ToolResult(callId = "", output = actions.joinToString("\n") { "${it.name} (${it.tool}): ${it.description} - used ${it.timesUsed}x" }, ok = true)
            }
            "get_provider" -> {
                val settings = com.allai.app.settings.SettingsState(context)
                ToolResult(callId = "", output = "Provider: ${settings.providerName}\nModel: ${settings.model}\nBase URL: ${settings.baseUrl}", ok = true)
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}
