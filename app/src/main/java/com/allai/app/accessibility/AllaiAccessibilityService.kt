package com.allai.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.allai.app.model.ToolResult
import com.allai.app.plugin.PhonePlugin
import java.util.concurrent.ConcurrentHashMap

class AllaiAccessibilityService : AccessibilityService() {
    private val actionCallbacks = ConcurrentHashMap<String, (AccessibilityNodeInfo) -> Unit>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Store recent events for screen reading
        AccessibilityEventStore.addEvent(event)
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    fun getScreenText(): String {
        val root = rootInActiveWindow
        if (root == null) return "No accessible content"
        return extractText(root, 0)
    }

    private fun extractText(node: AccessibilityNodeInfo, depth: Int): String {
        if (node == null) return ""
        val sb = StringBuilder()
        val text = node.text
        if (text != null && text.isNotEmpty()) {
            sb.append("  ".repeat(depth)).append(text).append("\n")
        }
        for (i in 0 until node.childCount) {
            sb.append(extractText(node.getChild(i), depth + 1))
        }
        return sb.toString()
    }

    fun clickAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun clickNode(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun longClickAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun scroll(node: AccessibilityNodeInfo, direction: Int) {
        node.performAction(direction)
    }

    fun focusNode(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    fun clearFocus(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
    }

    fun setText(node: AccessibilityNodeInfo, text: String) {
        val bundle = Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    companion object {
        private var instance: AllaiAccessibilityService? = null
        fun getInstance(): AllaiAccessibilityService? = instance
    }

    init {
        AllaiAccessibilityService.instance = this
    }
}

object AccessibilityEventStore {
    private val events = java.util.concurrent.ConcurrentLinkedQueue<AccessibilityEvent>()
    fun addEvent(event: AccessibilityEvent) {
        events.add(event)
        while (events.size > 100) events.poll()
    }
    fun getRecentEvents(): List<AccessibilityEvent> = events.toList()
}

// Accessibility plugin - bridges AccessibilityService to the plugin system
class AccessibilityPlugin : PhonePlugin {
    override val name = "accessibility"
    override val description = "Read screen content and interact with any app UI."
    override fun execute(arguments: Map<String, Any>): ToolResult {
        val service = AllaiAccessibilityService.getInstance()
        if (service == null) {
            return ToolResult("", "", "Accessibility service not running", false)
        }
        val action = arguments["action"] as? String ?: return ToolResult("", "", "Missing 'action' argument", false)
        return when (action) {
            "read_screen" -> {
                val text = service.getScreenText()
                ToolResult(callId = "", output = text, ok = true)
            }
            "click_at" -> {
                val x = (arguments["x"] as? Number)?.toFloat() ?: return ToolResult("", "", "Missing 'x' argument", false)
                val y = (arguments["y"] as? Number)?.toFloat() ?: return ToolResult("", "", "Missing 'y' argument", false)
                service.clickAt(x, y)
                ToolResult(callId = "", output = "Clicked at ($x, $y)", ok = true)
            }
            "click_node" -> {
                val desc = arguments["description"] as? String ?: return ToolResult("", "", "Missing 'description' argument", false)
                val root = service.rootInActiveWindow
                val node = findNodeByDescription(root, desc)
                if (node != null) {
                    service.clickNode(node)
                    ToolResult(callId = "", output = "Clicked: $desc", ok = true)
                } else {
                    ToolResult(callId = "", output = "", error = "Node not found: $desc", ok = false)
                }
            }
            "set_text" -> {
                val desc = arguments["description"] as? String ?: return ToolResult("", "", "Missing 'description' argument", false)
                val text = arguments["text"] as? String ?: return ToolResult("", "", "Missing 'text' argument", false)
                val root = service.rootInActiveWindow
                val node = findNodeByDescription(root, desc)
                if (node != null) {
                    service.setText(node, text)
                    ToolResult(callId = "", output = "Set text: $text", ok = true)
                } else {
                    ToolResult(callId = "", output = "", error = "Node not found: $desc", ok = false)
                }
            }
            "scroll" -> {
                val direction = arguments["direction"] as? Int ?: 0
                val root = service.rootInActiveWindow
                root?.performAction(direction)
                ToolResult(callId = "", output = "Scrolled", ok = true)
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }

    private fun findNodeByDescription(node: AccessibilityNodeInfo?, description: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val contentDesc = node.contentDescription?.toString()
        val text = node.text?.toString()
        if ((contentDesc?.contains(description) == true) || (text?.contains(description) == true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val result = findNodeByDescription(node.getChild(i), description)
            if (result != null) return result
        }
        return null
    }
}
