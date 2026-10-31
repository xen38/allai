package com.allai.app.terminal

import java.io.BufferedReader
import java.io.InputStreamReader

data class TerminalResult(
    val output: String,
    val error: String,
    val exitCode: Int
)

object TerminalBridge {
    fun run(command: String): TerminalResult {
        val outBuf = StringBuilder()
        val errBuf = StringBuilder()
        var exitCode = -1
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            outReader.use { out ->
                out.forEachLine { outBuf.append(it).append("\n") }
            }
            errReader.use { err ->
                err.forEachLine { errBuf.append(it).append("\n") }
            }
            exitCode = process.waitFor()
        } catch (e: Exception) {
            errBuf.append(e.message).append("\n")
        }
        return TerminalResult(outBuf.toString(), errBuf.toString(), exitCode)
    }
}
