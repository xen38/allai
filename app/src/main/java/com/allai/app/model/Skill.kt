package com.allai.app.model

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val instructions: String,
    val tools: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val timesUsed: Int = 0
)