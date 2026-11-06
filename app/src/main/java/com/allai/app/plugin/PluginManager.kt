package com.allai.app.plugin

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import com.allai.app.accessibility.AccessibilityPlugin
import com.allai.app.analysis.ProjectPlugin
import com.allai.app.notification.NotificationPluginWrapper
import com.allai.app.self.SelfPlugin
import com.allai.app.self.SkillPlugin
import com.allai.app.AllaiApplication
import com.allai.app.model.ToolResult
import java.io.File
import androidx.core.content.ContextCompat

interface PhonePlugin {
    val name: String
    val description: String
    fun toolDefinition(): com.allai.app.model.ToolDefinition = com.allai.app.model.ToolDefinition(
        name = name,
        description = description,
        parameters = emptyList()
    )
    fun requiresConfirmation(): Boolean = false
    fun requiresPermission(): String? = null
    fun execute(arguments: Map<String, Any>): ToolResult
}

data class ToolExecutionResult(
    val result: ToolResult,
    val confirmationRequired: Boolean = false,
    val confirmationMessage: String? = null
)

object PluginManager {
    private val plugins = mutableMapOf<String, PhonePlugin>()

    fun init(context: Context) {
        plugins.clear()
        register(ShellPlugin(context))
        register(FilePlugin(context))
        register(ContactsPlugin(context))
        register(SmsPlugin(context))
        register(LauncherPlugin(context))
        register(McpPlugin())
        register(AccessibilityPlugin())
        register(SelfPlugin(context))
        register(SkillPlugin(context))
        register(NotificationPluginWrapper())
        register(ProjectPlugin(context))
    }

    fun register(plugin: PhonePlugin) {
        plugins[plugin.name] = plugin
    }

    fun list(): List<PhonePlugin> = plugins.values.toList()

    fun toolDefinitions(): List<com.allai.app.model.ToolDefinition> =
        plugins.values.map { it.toolDefinition() }

    fun toolNames(): List<String> = plugins.keys.toList()

    fun run(name: String, arguments: Map<String, Any>): ToolResult {
        return plugins[name]?.execute(arguments) ?: ToolResult(
            callId = "",
            output = "",
            error = "Plugin not found: $name",
            ok = false
        )
    }

    fun runChecked(context: Context, name: String, arguments: Map<String, Any>): ToolExecutionResult {
        val plugin = plugins[name] ?: return ToolExecutionResult(
            result = ToolResult("", "", "Plugin not found: $name", ok = false)
        )

        val perm = plugin.requiresPermission()
        if (perm != null) {
            val hasPerm = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                return ToolExecutionResult(
                    result = ToolResult("", "", "Permission required: $perm", ok = false),
                    confirmationRequired = true,
                    confirmationMessage = "Grant the '$perm' permission to use the '$name' tool."
                )
            }
        }

        if (plugin.requiresConfirmation()) {
            return ToolExecutionResult(
                result = ToolResult("", "", "Confirmation required", ok = false),
                confirmationRequired = true,
                confirmationMessage = "Execute '$name' with arguments: ${arguments.entries.joinToString(", ") { "${it.key}=${it.value}" }}?"
            )
        }

        return ToolExecutionResult(
            result = plugin.execute(arguments),
            confirmationRequired = false
        )
    }

    fun checkPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

// Shell plugin - run terminal commands
class ShellPlugin(private val context: Context) : PhonePlugin {
    override val name = "shell"
    override val description = "Execute shell commands on the Android device."
    override fun requiresConfirmation(): Boolean = true
    override fun execute(arguments: Map<String, Any>): ToolResult {
        val cmd = arguments["command"] as? String ?: return ToolResult("", "", "Missing 'command' argument", false)
        val timeoutMs = (arguments["timeout_ms"] as? Number)?.toInt() ?: 10000
        return com.allai.app.terminal.TerminalBridge.run(cmd, timeoutMs).let { result ->
            ToolResult(
                callId = "",
                output = result.output,
                error = result.error,
                ok = result.exitCode == 0
            )
        }
    }
}

// File plugin - read/write files
class FilePlugin(private val context: Context) : PhonePlugin {
    override val name = "file"
    override val description = "Read and write files on the device storage."
    override fun requiresPermission(): String? = android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
    override fun requiresConfirmation(): Boolean = true
    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: return ToolResult("", "", "Missing 'action' argument", false)
        return when (action) {
            "read" -> {
                val path = arguments["path"] as? String ?: return ToolResult("", "", "Missing 'path' argument", false)
                try {
                    val content = File(path).readText()
                    ToolResult(callId = "", output = content, ok = true)
                } catch (e: Exception) {
                    ToolResult(callId = "", output = "", error = e.message ?: "Unknown error", ok = false)
                }
            }
            "write" -> {
                val path = arguments["path"] as? String ?: return ToolResult("", "", "Missing 'path' argument", false)
                val content = arguments["content"] as? String ?: return ToolResult("", "", "Missing 'content' argument", false)
                try {
                    File(path).parentFile?.mkdirs()
                    File(path).writeText(content)
                    ToolResult(callId = "", output = "Written to $path", ok = true)
                } catch (e: Exception) {
                    ToolResult(callId = "", output = "", error = e.message ?: "Unknown error", ok = false)
                }
            }
            "list" -> {
                val path = arguments["path"] as? String ?: return ToolResult("", "", "Missing 'path' argument", false)
                try {
                    val file = File(path)
                    val entries = file.listFiles()?.map { it.name + (if (it.isDirectory()) "/" else "") } ?: emptyList()
                    ToolResult(callId = "", output = entries.joinToString("\n"), ok = true)
                } catch (e: Exception) {
                    ToolResult(callId = "", output = "", error = e.message ?: "Unknown error", ok = false)
                }
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}

// Contacts plugin
class ContactsPlugin(private val context: Context) : PhonePlugin {
    override val name = "contacts"
    override val description = "Read phone contacts."
    override fun requiresPermission(): String? = android.Manifest.permission.READ_CONTACTS
    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: "list"
        return when (action) {
            "list" -> {
                val limit = (arguments["limit"] as? Number)?.toInt() ?: 50
                val cursor = context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null
                )
                val contacts = mutableListOf<String>()
                cursor?.use { cur ->
                    while (cur.moveToNext() && contacts.size < limit) {
                        val id = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        val name = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        val hasPhone = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) == "1"
                        var phones = ""
                        if (hasPhone) {
                            val phonesCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            phonesCursor?.use { pc ->
                                val phoneList = mutableListOf<String>()
                                while (pc.moveToNext()) {
                                    phoneList.add(pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)))
                                }
                                phones = phoneList.joinToString(", ")
                            }
                        }
                        contacts.add("$name: $phones")
                    }
                }
                ToolResult(callId = "", output = contacts.joinToString("\n"), ok = true)
            }
            "search" -> {
                val query = arguments["query"] as? String ?: return ToolResult("", "", "Missing 'query' argument", false)
                val cursor = context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    null,
                    ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?",
                    arrayOf("%$query%"),
                    null
                )
                val results = mutableListOf<String>()
                cursor?.use { cur ->
                    while (cur.moveToNext()) {
                        val name = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        results.add(name)
                    }
                }
                ToolResult(callId = "", output = results.joinToString("\n"), ok = true)
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}

// SMS plugin
class SmsPlugin(private val context: Context) : PhonePlugin {
    override val name = "sms"
    override val description = "Send SMS messages."
    override fun requiresPermission(): String? = android.Manifest.permission.SEND_SMS
    override fun requiresConfirmation(): Boolean = true
    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: "send"
        return when (action) {
            "send" -> {
                val phone = arguments["phone"] as? String ?: return ToolResult("", "", "Missing 'phone' argument", false)
                val message = arguments["message"] as? String ?: return ToolResult("", "", "Missing 'message' argument", false)
                try {
                    val smsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(phone, null, message, null, null)
                    ToolResult(callId = "", output = "SMS sent to $phone", ok = true)
                } catch (e: Exception) {
                    ToolResult(callId = "", output = "", error = e.message ?: "Unknown error", ok = false)
                }
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}

// App launcher plugin
class LauncherPlugin(private val context: Context) : PhonePlugin {
    override val name = "launcher"
    override val description = "Launch apps by package name."
    override fun requiresConfirmation(): Boolean = true
    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: "launch"
        return when (action) {
            "launch" -> {
                val packageName = arguments["package"] as? String ?: return ToolResult("", "", "Missing 'package' argument", false)
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        // Auto-launch bubble mode so Allai hovers while interacting with the other app
                        try {
                            com.allai.app.floating.FloatingWindowService.start(context)
                        } catch (e: Exception) { }
                        ToolResult(callId = "", output = "Launched $packageName", ok = true)
                    } else {
                        ToolResult(callId = "", output = "", error = "App not found: $packageName", ok = false)
                    }
                } catch (e: Exception) {
                    ToolResult(callId = "", output = "", error = e.message ?: "Unknown error", ok = false)
                }
            }
            "close" -> {
                val packageName = arguments["package"] as? String ?: return ToolResult("", "", "Missing 'package' argument", false)
                try {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses(packageName)
                    ToolResult(callId = "", output = "Closed/killed $packageName", ok = true)
                } catch (e: Exception) {
                    ToolResult(callId = "", output = "", error = e.message ?: "Unknown error", ok = false)
                }
            }
            "list" -> {
                val packages = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .map { it.packageName }
                    .sorted()
                ToolResult(callId = "", output = packages.joinToString("\n"), ok = true)
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}

// MCP plugin - bridge to MCP servers
class McpPlugin : PhonePlugin {
    override val name = "mcp"
    override val description = "Connect to and call MCP servers."
    override fun requiresConfirmation(): Boolean = true
    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: "call"
        return when (action) {
            "connect" -> {
                val command = arguments["command"] as? String ?: return ToolResult("", "", "Missing 'command' argument", false)
                val args = (arguments["args"] as? List<*>)?.map { it.toString() } ?: emptyList()
                val fullCommand = listOf(command) + args
                com.allai.app.mcp.McpClient.connect(fullCommand)
                ToolResult(callId = "", output = "MCP client connected", ok = true)
            }
            "call" -> {
                val method = arguments["method"] as? String ?: return ToolResult("", "", "Missing 'method' argument", false)
                val params = HashMap<String, Any>()
                val raw = arguments["params"]
                if (raw is Map<*, *>) {
                    for ((k, v) in raw) {
                        params[(k as? String) ?: k.toString()] = v as Any
                    }
                }
                val response = com.allai.app.mcp.McpClient.call(method, params)
                ToolResult(
                    callId = "",
                    output = com.google.gson.Gson().toJson(response?.result),
                    error = response?.error?.message ?: "",
                    ok = response?.error == null
                )
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}
