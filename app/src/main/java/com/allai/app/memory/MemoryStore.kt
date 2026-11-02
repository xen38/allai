package com.allai.app.memory

import android.content.Context
import android.content.SharedPreferences
import com.allai.app.model.Memory
import com.allai.app.model.LearnedAction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MemoryStore(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("allai_memories", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun addMemory(memory: Memory) {
        val memories = getMemories().toMutableList()
        memories.add(memory)
        prefs.edit().putString("memories", gson.toJson(memories)).apply()
    }

    fun getMemories(): List<Memory> {
        val json = prefs.getString("memories", "") ?: return emptyList()
        return if (json.isBlank()) emptyList()
        else gson.fromJson(json, object : TypeToken<List<Memory>>() {}.type) ?: emptyList()
    }

    fun deleteMemory(id: String) {
        val memories = getMemories().toMutableList().filter { it.id != id }
        prefs.edit().putString("memories", gson.toJson(memories)).apply()
    }

    fun toggleFavorite(id: String) {
        val memories = getMemories().toMutableList()
        val index = memories.indexOfFirst { it.id == id }
        if (index >= 0) {
            memories[index] = memories[index].copy(favorite = !memories[index].favorite)
            prefs.edit().putString("memories", gson.toJson(memories)).apply()
        }
    }

    fun updateMemory(id: String, newTitle: String, newDesc: String) {
        val memories = getMemories().toMutableList()
        val index = memories.indexOfFirst { it.id == id }
        if (index >= 0) {
            memories[index] = memories[index].copy(title = newTitle, description = newDesc)
            prefs.edit().putString("memories", gson.toJson(memories)).apply()
        }
    }

    fun clearMemories() {
        prefs.edit().putString("memories", "[]").apply()
    }

    fun addLearnedAction(action: LearnedAction) {
        val actions = getLearnedActions().toMutableList()
        val existing = actions.indexOfFirst { it.name == action.name }
        if (existing >= 0) {
            actions[existing] = actions[existing].copy(
                timesUsed = actions[existing].timesUsed + 1,
                lastUsed = System.currentTimeMillis()
            )
        } else {
            actions.add(action)
        }
        prefs.edit().putString("learned_actions", gson.toJson(actions)).apply()
    }

    fun getLearnedActions(): List<LearnedAction> {
        val json = prefs.getString("learned_actions", "") ?: return emptyList()
        return if (json.isBlank()) emptyList()
        else gson.fromJson(json, object : TypeToken<List<LearnedAction>>() {}.type) ?: emptyList()
    }

    fun recordAction(name: String, description: String, tool: String, arguments: Map<String, Any>) {
        addLearnedAction(
            LearnedAction(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                tool = tool,
                arguments = arguments
            )
        )
        addMemory(
            Memory(
                id = java.util.UUID.randomUUID().toString(),
                title = name,
                description = description,
                action = tool,
                arguments = arguments.toString()
            )
        )
    }

    companion object {
        private var instance: MemoryStore? = null
        fun getInstance(context: Context): MemoryStore {
            return instance ?: MemoryStore(context.applicationContext).also { instance = it }
        }
    }
}
