package com.allai.app.mcp

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.concurrent.ConcurrentLinkedQueue

data class McpRequest(val jsonrpc: String = "2.0", val id: Any?, val method: String, val params: Map<String, Any>? = null)
data class McpResponse(val jsonrpc: String = "2.0", val id: Any?, val result: Any? = null, val error: McpError? = null)
data class McpError(val code: Int, val message: String, val data: Any? = null)

object McpClient {
    private val gson = Gson()
    private val pending = ConcurrentLinkedQueue<McpResponse>()
    private var currentProcess: java.lang.Process? = null
    private var currentWriter: PrintWriter? = null

    fun connect(command: List<String>) {
        disconnect()
        try {
            val processBuilder = java.lang.ProcessBuilder(*command.toTypedArray())
            processBuilder.redirectErrorStream(false)
            currentProcess = processBuilder.start()

            val writer = PrintWriter(OutputStreamWriter(currentProcess!!.outputStream), true)
            currentWriter = writer

            java.lang.Thread {
                val reader = BufferedReader(InputStreamReader(currentProcess!!.inputStream))
                reader.use { r ->
                    r.forEachLine { line ->
                        try {
                            val response = gson.fromJson(line, McpResponse::class.java)
                            pending.add(response)
                        } catch (e: Exception) { }
                    }
                }
            }.start()

            java.lang.Thread {
                val reader = BufferedReader(InputStreamReader(currentProcess!!.errorStream))
                reader.use { r -> r.forEachLine { } }
            }.start()

        } catch (e: Exception) { }
    }

    fun disconnect() {
        currentWriter = null
        currentProcess?.destroyForcibly()
        currentProcess = null
        pending.clear()
    }

    fun call(method: String, params: Map<String, Any>? = null): McpResponse? {
        val writer = currentWriter ?: return McpResponse(id = null, error = McpError(-32603, "MCP client not connected", null))

        val id = System.currentTimeMillis()
        val request = McpRequest(id = id, method = method, params = params)
        writer.println(gson.toJson(request))

        var waited = 0
        while (waited < 300) {
            val response = pending.poll()
            if (response != null && response.id == id) {
                return response
            }
            waited++
            try { java.lang.Thread.sleep(100) } catch (e: Exception) { }
        }
        return McpResponse(id = id, error = McpError(-32603, "MCP request timeout", null))
    }

    fun listTools(): McpResponse? = call("tools/list")

    fun callTool(name: String, arguments: Map<String, Any>? = null): McpResponse? {
        val paramsMap: MutableMap<String, Any> = HashMap()
        paramsMap["name"] = name
        paramsMap["arguments"] = arguments ?: emptyMap<String, Any>()
        return call("tools/call", paramsMap)
    }
}
