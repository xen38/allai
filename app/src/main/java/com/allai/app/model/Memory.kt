package com.allai.app.model

data class Memory(
    val id: String,
    val title: String,
    val description: String,
    val action: String,
    val arguments: String,
    val timestamp: Long = System.currentTimeMillis(),
    var favorite: Boolean = false
)

data class LearnedAction(
    val id: String,
    val name: String,
    val description: String,
    val tool: String,
    val arguments: Map<String, Any>,
    val timesUsed: Int = 0,
    val lastUsed: Long = 0
)
