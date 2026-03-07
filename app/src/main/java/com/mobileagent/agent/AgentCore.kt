package com.mobileagent.agent

import com.mobileagent.llm.LLMClient
import com.mobileagent.models.LlmChunk
import com.mobileagent.models.Message
import com.mobileagent.models.ToolCall
import com.mobileagent.models.ToolResult
import com.mobileagent.storage.MessageDao
import com.mobileagent.tools.AgentTool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.util.UUID

class ToolRegistry(tools: List<AgentTool>) {
    private val map = tools.associateBy { it.name }
    fun get(name: String): AgentTool? = map[name]
}

class ToolExecutor(private val registry: ToolRegistry) {
    fun execute(call: ToolCall): ToolResult {
        val tool = registry.get(call.name)
            ?: return ToolResult(call.id, call.name, "unknown tool", false)
        return tool.execute(call.id, call.arguments)
    }
}

class AgentLoop(
    private val llmClient: LLMClient,
    private val dao: MessageDao,
    private val toolExecutor: ToolExecutor
) {
    fun run(userPrompt: String): Flow<String> = flow {
        dao.insert(Message(role = "user", content = userPrompt))
        var continueLoop = true
        var cycle = 0
        while (continueLoop && cycle < 10) {
            cycle++
            val history = summarizeIfTooLarge(dao.allMessages())
            var textBuffer = StringBuilder()
            var pendingToolCall: ToolCall? = null

            llmClient.streamCompletion(history).collect { chunk: LlmChunk ->
                if (chunk.textDelta.isNotBlank()) {
                    textBuffer.append(chunk.textDelta)
                    emit(chunk.textDelta)
                }
                if (chunk.toolCall != null) pendingToolCall = chunk.toolCall
            }

            val toolCall = pendingToolCall
            if (toolCall != null) {
                val result = toolExecutor.execute(toolCall)
                val output = "tool_result(${result.name}) success=${result.success}\n${result.output.take(8_000)}"
                dao.insert(Message(role = "tool", content = output))
            } else {
                dao.insert(Message(role = "assistant", content = textBuffer.toString().trim()))
                continueLoop = false
            }
        }
    }

    private fun summarizeIfTooLarge(messages: List<Message>): List<Message> {
        val totalChars = messages.sumOf { it.content.length }
        if (totalChars < 16_000) return messages
        val recent = messages.takeLast(24)
        val summary = Message(
            role = "system",
            content = "Conversation compressed for mobile context window. Keep answers concise and action-oriented."
        )
        return listOf(summary) + recent
    }
}

class AgentController(private val loop: AgentLoop) {
    fun submit(prompt: String): Flow<String> = loop.run(prompt)

    companion object {
        fun makeToolCall(name: String, arguments: Map<String, String>): ToolCall =
            ToolCall(id = UUID.randomUUID().toString(), name = name, arguments = arguments)
    }
}
