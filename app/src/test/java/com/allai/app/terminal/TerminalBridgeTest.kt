package com.allai.app.terminal

import junit.framework.TestCase

class TerminalBridgeTest : TestCase() {
    fun testTerminalResultDataClass() {
        val result = TerminalResult(output = "hello", error = "", exitCode = 0)
        assertEquals("hello", result.output)
        assertEquals("", result.error)
        assertEquals(0, result.exitCode)
    }

    fun testTerminalResultWithError() {
        val result = TerminalResult(output = "", error = "boom", exitCode = 1)
        assertEquals(1, result.exitCode)
        assertEquals("boom", result.error)
    }

    fun testRunReturnsResultStructure() {
        val result = TerminalBridge.run("echo hello")
        assertNotNull(result)
        assertNotNull(result.output)
        assertNotNull(result.error)
    }
}
