package com.allai.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.allai.app.model.ToolResult
import com.allai.app.plugin.PhonePlugin
import java.util.concurrent.ConcurrentLinkedQueue

class NotificationListenerPlugin : NotificationListenerService(), PhonePlugin {
    private val notifications = ConcurrentLinkedQueue<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            val pkg = sbn.packageName ?: ""
            val txt = sbn.notification?.extras?.getString("android.title") ?: ""
            notifications.add("$pkg: $txt")
            while (notifications.size > 100) notifications.poll()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification dismissed
    }

    override val name: String
        get() = "notification"

    override val description: String
        get() = "Read and monitor system notifications."

    override fun requiresPermission(): String? = "android.permission.BIND_NOTIFICATION_LISTENER"

    override fun requiresConfirmation(): Boolean = true

    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: return ToolResult("", "", "Missing 'action' argument", false)
        return when (action) {
            "list" -> {
                val recent = notifications.toList()
                ToolResult(callId = "", output = recent.joinToString("\n"), ok = true)
            }
            "count" -> {
                ToolResult(callId = "", output = "${notifications.size} notifications", ok = true)
            }
            "clear" -> {
                notifications.clear()
                ToolResult(callId = "", output = "Notifications cleared", ok = true)
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }

    companion object {
        private var instance: NotificationListenerPlugin? = null
        fun getInstance(): NotificationListenerPlugin? = instance
    }

    init {
        NotificationListenerPlugin.instance = this
    }
}