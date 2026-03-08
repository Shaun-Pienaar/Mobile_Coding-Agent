package com.mobileagent.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole { system, user, assistant, tool }

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, String>
)

data class ToolResult(
    val callId: String,
    val name: String,
    val output: String,
    val success: Boolean
)

data class AgentState(
    val isRunning: Boolean = false,
    val currentProvider: String = "openai",
    val currentModel: String = "gpt-4o-mini",
    val cycle: Int = 0,
    val lastError: String? = null
)

data class LlmChunk(
    val textDelta: String = "",
    val toolCall: ToolCall? = null,
    val done: Boolean = false
)

data class ProviderSelection(
    val provider: String,
    val model: String,
    val baseUrl: String
)
