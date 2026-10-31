package com.allai.app.plugin

import com.allai.app.model.ToolResult

interface PhonePlugin {
    val name: String
    val description: String
    fun execute(arguments: Map<String, Any>): ToolResult
}

object PluginManager {
    private val plugins = mutableMapOf<String, PhonePlugin>()

    fun register(plugin: PhonePlugin) {
        plugins[plugin.name] = plugin
    }

    fun list(): List<PhonePlugin> = plugins.values.toList()

    fun run(name: String, arguments: Map<String, Any>): ToolResult {
        return plugins[name]?.execute(arguments) ?: ToolResult(
            callId = "",
            output = "",
            error = "Plugin not found: $name",
            ok = false
        )
    }
}

class ContactsPlugin : PhonePlugin {
    override val name = "contacts"
    override val description = "Read and search phone contacts."
    override fun execute(arguments: Map<String, Any>): ToolResult {
        return ToolResult(callId = "", output = "Contacts plugin stub — wire to ContactsContract.", ok = true)
    }
}

class SmsPlugin : PhonePlugin {
    override val name = "sms"
    override val description = "Send and read SMS."
    override fun execute(arguments: Map<String, Any>): ToolResult {
        return ToolResult(callId = "", output = "SMS plugin stub — wire to SmsManager.", ok = true)
    }
}

class AppLauncherPlugin : PhonePlugin {
    override val name = "launcher"
    override val description = "Launch other apps by package name."
    override fun execute(arguments: Map<String, Any>): ToolResult {
        return ToolResult(callId = "", output = "Launcher plugin stub — wire to PackageManager.", ok = true)
    }
}
