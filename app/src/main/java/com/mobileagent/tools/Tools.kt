package com.mobileagent.tools

import com.mobileagent.models.ToolResult
import com.mobileagent.storage.FileRepository

interface AgentTool {
    val name: String
    fun execute(callId: String, args: Map<String, String>): ToolResult
}

class ReadFileTool(private val files: FileRepository) : AgentTool {
    override val name = "read_file"
    override fun execute(callId: String, args: Map<String, String>): ToolResult {
        val path = args["path"] ?: return ToolResult(callId, name, "missing path", false)
        return runCatching { ToolResult(callId, name, files.read(path), true) }
            .getOrElse { ToolResult(callId, name, it.message ?: "read error", false) }
    }
}

class WriteFileTool(private val files: FileRepository) : AgentTool {
    override val name = "write_file"
    override fun execute(callId: String, args: Map<String, String>): ToolResult {
        val path = args["path"] ?: return ToolResult(callId, name, "missing path", false)
        val content = args["content"] ?: ""
        return runCatching {
            files.write(path, content)
            ToolResult(callId, name, "wrote $path", true)
        }.getOrElse { ToolResult(callId, name, it.message ?: "write error", false) }
    }
}

class ListFilesTool(private val files: FileRepository) : AgentTool {
    override val name = "list_files"
    override fun execute(callId: String, args: Map<String, String>): ToolResult {
        val directory = args["directory"] ?: "."
        return runCatching { ToolResult(callId, name, files.list(directory).joinToString("\n"), true) }
            .getOrElse { ToolResult(callId, name, it.message ?: "list error", false) }
    }
}

class SearchCodeTool(private val files: FileRepository) : AgentTool {
    override val name = "search_code"
    override fun execute(callId: String, args: Map<String, String>): ToolResult {
        val query = args["query"] ?: return ToolResult(callId, name, "missing query", false)
        return runCatching { ToolResult(callId, name, files.search(query).joinToString("\n"), true) }
            .getOrElse { ToolResult(callId, name, it.message ?: "search error", false) }
    }
}
