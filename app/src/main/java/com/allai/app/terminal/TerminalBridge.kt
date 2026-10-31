package com.allai.app.terminal

import java.io.BufferedReader
import java.io.InputStreamReader

data class TerminalResult(
    val output: String,
    val error: String,
    val exitCode: Int
)

object TerminalBridge {
    fun run(command: String, timeoutMs: Int = 10000): TerminalResult {
        val outBuf = StringBuilder()
        val errBuf = StringBuilder()
        var exitCode = -1
        try {
            val process = java.lang.Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            // Read streams in parallel
            val outThread = java.lang.Thread {
                outReader.use { r -> r.forEachLine { outBuf.append(it).append("\n") } }
            }
            val errThread = java.lang.Thread {
                errReader.use { r -> r.forEachLine { errBuf.append(it).append("\n") } }
            }
            outThread.start()
            errThread.start()

            val finished = process.waitFor(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                errBuf.append("Command timed out after ${timeoutMs}ms\n")
            }
            exitCode = process.exitValue()

            outThread.join(1000)
            errThread.join(1000)
        } catch (e: Exception) {
            errBuf.append(e.message ?: "Unknown error").append("\n")
        }
        return TerminalResult(outBuf.toString(), errBuf.toString(), exitCode)
    }
}
