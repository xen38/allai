package com.allai.app.mcp

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue

data class McpRequest(val jsonrpc: String = "2.0", val id: Any?, val method: String, val params: Map<String, Any>? = null)
data class McpResponse(val jsonrpc: String = "2.0", val id: Any?, val result: Any? = null, val error: McpError? = null)
data class McpError(val code: Int, val message: String, val data: Any? = null)

object McpClient {
    private val gson = Gson()
    private val pending = ConcurrentLinkedQueue<McpResponse>()

    fun connect(command: List<String>) {
        // Stub: spawns a stdio MCP server process and reads JSON-RPC lines.
        // Real implementation wires stdin/stdout to the process.
        try {
            val process = Runtime.getRuntime().exec(command.toTypedArray())
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.forEachLine { line ->
                    pending.add(gson.fromJson(line, McpResponse::class.java))
                }
            }
        } catch (e: Exception) {
            // Ignore in stub
        }
    }

    fun call(method: String, params: Map<String, Any>? = null): McpResponse? {
        // Stub: returns an empty result until a real transport is wired.
        return McpResponse(id = System.currentTimeMillis(), result = mapOf("ok" to true))
    }
}
