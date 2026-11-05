package com.allai.app.self

import android.content.Context
import com.allai.app.model.Skill
import com.allai.app.model.ToolResult
import com.allai.app.plugin.PhonePlugin
import com.allai.app.memory.SkillStore

class SkillPlugin(private val context: Context) : PhonePlugin {
    override val name = "skill"
    override val description = "Manage and execute skills - reusable capabilities taught to Allai."

    override fun toolDefinition(): com.allai.app.model.ToolDefinition =
        com.allai.app.model.ToolDefinition(
            name = "skill",
            description = "Execute a named skill",
            parameters = listOf(
                com.allai.app.model.ToolParameter(
                    name = "skill_name",
                    type = "string",
                    description = "Name of the skill to execute",
                    required = true
                )
            )
        )

    override fun execute(arguments: Map<String, Any>): ToolResult {
        val action = arguments["action"] as? String ?: return ToolResult("", "", "Missing 'action' argument", false)
        val store = SkillStore.getInstance(context)
        return when (action) {
            "list" -> {
                val skills = store.getSkills()
                ToolResult(callId = "", output = skills.joinToString("\n") { "${it.name}: ${it.description} (used ${it.timesUsed}x)" }, ok = true)
            }
            "execute" -> {
                val skillName = arguments["skill_name"] as? String ?: return ToolResult("", "", "Missing 'skill_name' argument", false)
                val skill = store.getSkills().firstOrNull { it.name == skillName }
                if (skill == null) {
                    ToolResult(callId = "", output = "", error = "Skill not found: $skillName", ok = false)
                } else {
                    store.incrementUsage(skill.id)
                    ToolResult(callId = "", output = "Executed skill: ${skill.name}\n${skill.instructions}", ok = true)
                }
            }
            "create" -> {
                val name = arguments["name"] as? String ?: return ToolResult("", "", "Missing 'name' argument", false)
                val desc = arguments["description"] as? String ?: return ToolResult("", "", "Missing 'description' argument", false)
                val instructions = arguments["instructions"] as? String ?: return ToolResult("", "", "Missing 'instructions' argument", false)
                val tools = (arguments["tools"] as? List<*>)?.map { it.toString() } ?: emptyList()
                val skill = Skill(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    description = desc,
                    instructions = instructions,
                    tools = tools
                )
                store.addSkill(skill)
                ToolResult(callId = "", output = "Skill created: $name", ok = true)
            }
            "delete" -> {
                val id = arguments["id"] as? String ?: return ToolResult("", "", "Missing 'id' argument", false)
                store.deleteSkill(id)
                ToolResult(callId = "", output = "Skill deleted: $id", ok = true)
            }
            else -> ToolResult("", "", "Unknown action: $action", false)
        }
    }
}