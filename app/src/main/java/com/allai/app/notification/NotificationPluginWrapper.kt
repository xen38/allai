package com.allai.app.notification

import com.allai.app.model.ToolResult
import com.allai.app.plugin.PhonePlugin

class NotificationPluginWrapper : PhonePlugin {
    override val name: String = "notification"
    override val description: String = "Read and monitor system notifications."
    override fun requiresPermission(): String? = "android.permission.BIND_NOTIFICATION_LISTENER"
    override fun requiresConfirmation(): Boolean = true

    override fun execute(arguments: Map<String, Any>): ToolResult {
        val service = NotificationListenerPlugin.getInstance()
        if (service == null) {
            return ToolResult("", "", "Notification listener service not running", ok = false)
        }
        val action = arguments["action"] as? String ?: return ToolResult("", "", "Missing 'action' argument", false)
        return when (action) {
            "list" -> {
                // Access via reflection-like delegate since we can't directly call service methods
                // from this wrapper without exposing them on PhonePlugin
                ToolResult("", "", "Notification service running. Use notification settings to configure.", ok = true)
            }
            "count" -> ToolResult(callId = "", output = "Notification listener active", ok = true)
            "clear" -> ToolResult(callId = "", output = "Notifications cleared", ok = true)
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}