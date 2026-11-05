package com.allai.app.memory

import android.content.Context
import android.content.SharedPreferences
import com.allai.app.model.Skill
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SkillStore(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("allai_skills", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun addSkill(skill: Skill) {
        val skills = getSkills().toMutableList()
        val existingIdx = skills.indexOfFirst { it.id == skill.id }
        if (existingIdx >= 0) {
            skills[existingIdx] = skill.copy(updatedAt = System.currentTimeMillis())
        } else {
            skills.add(skill)
        }
        prefs.edit().putString("skills", gson.toJson(skills)).apply()
    }

    fun getSkills(): List<Skill> {
        val json = prefs.getString("skills", "") ?: return emptyList()
        return if (json.isBlank()) emptyList()
        else gson.fromJson(json, object : TypeToken<List<Skill>>() {}.type) ?: emptyList()
    }

    fun deleteSkill(id: String) {
        val skills = getSkills().toMutableList().filter { it.id != id }
        prefs.edit().putString("skills", gson.toJson(skills)).apply()
    }

    fun incrementUsage(id: String) {
        val skills = getSkills().toMutableList()
        val idx = skills.indexOfFirst { it.id == id }
        if (idx >= 0) {
            skills[idx] = skills[idx].copy(
                timesUsed = skills[idx].timesUsed + 1,
                updatedAt = System.currentTimeMillis()
            )
            prefs.edit().putString("skills", gson.toJson(skills)).apply()
        }
    }

    fun clearSkills() {
        prefs.edit().putString("skills", "[]").apply()
    }

    companion object {
        private var instance: SkillStore? = null
        fun getInstance(context: Context): SkillStore {
            return instance ?: SkillStore(context.applicationContext).also { instance = it }
        }
    }
}