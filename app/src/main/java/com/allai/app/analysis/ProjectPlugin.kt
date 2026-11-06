package com.allai.app.analysis

import com.allai.app.model.ToolResult
import com.allai.app.plugin.PhonePlugin
import java.io.File

class ProjectPlugin(private val context: android.content.Context) : PhonePlugin {
    override val name: String = "project"
    override val description: String = "Analyze and debug code projects on the device."
    override fun requiresConfirmation(): Boolean = false

    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: return ToolResult("", "", "Missing 'action' argument", false)
        return when (action) {
            "analyze" -> {
                val path = arguments["path"] as? String ?: return ToolResult("", "", "Missing 'path' argument", false)
                val projectDir = File(path)
                if (!projectDir.exists() || !projectDir.isDirectory) {
                    return ToolResult("", "", "Project directory not found: $path", ok = false)
                }
                val files = collectSourceFiles(projectDir)
                val summary = analyzeProject(files)
                ToolResult(callId = "", output = summary, ok = true)
            }
            "explore" -> {
                val path = arguments["path"] as? String ?: "."
                val dir = File(path)
                if (!dir.exists()) {
                    return ToolResult("", "", "Path not found: $path", ok = false)
                }
                val entries = dir.listFiles()?.map { it.name + (if (it.isDirectory) "/" else "") }?.joinToString("\n") ?: "empty"
                ToolResult(callId = "", output = entries, ok = true)
            }
            "read" -> {
                val path = arguments["path"] as? String ?: return ToolResult("", "", "Missing 'path' argument", false)
                try {
                    val content = File(path).readText()
                    ToolResult(callId = "", output = content, ok = true)
                } catch (e: Exception) {
                    ToolResult("", "", "Error reading $path: ${e.message}", ok = false)
                }
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }

    private fun collectSourceFiles(dir: File): List<File> {
        val files = mutableListOf<File>()
        val extensions = listOf(".kt", ".java", ".xml", ".json", ".gradle", ".properties")
        collectRecursive(dir, files, extensions)
        return files
    }

    private fun collectRecursive(dir: File, files: MutableList<File>, extensions: List<String>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (file.name != ".git" && file.name != "build" && file.name != ".gradle") {
                    collectRecursive(file, files, extensions)
                }
            } else if (extensions.any { file.name.endsWith(it) }) {
                files.add(file)
            }
        }
    }

    private fun analyzeProject(files: List<File>): String {
        val sb = StringBuilder()
        sb.append("Project Analysis\n")
        sb.append("================\n")
        sb.append("Total source files: ${files.size}\n\n")

        val ktFiles = files.count { it.name.endsWith(".kt") }
        val javaFiles = files.count { it.name.endsWith(".java") }
        val xmlFiles = files.count { it.name.endsWith(".xml") }
        val gradleFiles = files.count { it.name.endsWith(".gradle") }

        sb.append("Kotlin files: $ktFiles\n")
        sb.append("Java files: $javaFiles\n")
        sb.append("XML files: $xmlFiles\n")
        sb.append("Gradle files: $gradleFiles\n\n")

        // Detect project type
        val hasAndroidManifest = files.any { it.name == "AndroidManifest.xml" }
        val hasBuildGradle = files.any { it.name == "build.gradle" || it.name == "build.gradle.kts" }
        val hasSettingsGradle = files.any { it.name == "settings.gradle" || it.name == "settings.gradle.kts" }

        sb.append("Project Type:\n")
        if (hasAndroidManifest) sb.append("- Android project (AndroidManifest.xml found)\n")
        if (hasBuildGradle) sb.append("- Gradle build system\n")
        if (hasSettingsGradle) sb.append("- Multi-module or single project (settings.gradle found)\n")
        if (!hasAndroidManifest && !hasBuildGradle) sb.append("- Generic code project\n")

        sb.append("\nLargest files:\n")
        val sorted = files.sortedByDescending { it.length() }.take(5)
        sorted.forEach { file ->
            sb.append("- ${file.name}: ${file.length()} bytes\n")
        }

        return sb.toString()
    }
}