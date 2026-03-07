package com.mobileagent.storage

import java.io.File

class FileRepository(private val workspaceManager: WorkspaceManager) {
    fun read(path: String, maxChars: Int = 200_000): String {
        val file = workspaceFile(path)
        val data = file.readText()
        return if (data.length > maxChars) data.take(maxChars) + "\n...[truncated]" else data
    }

    fun write(path: String, content: String) {
        val file = workspaceFile(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun list(directory: String): List<String> {
        val dir = workspaceFile(directory)
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown()
            .maxDepth(4)
            .filter { it.isFile }
            .map { it.relativeTo(workspaceManager.rootDir).path }
            .sorted()
            .toList()
    }

    fun search(query: String): List<String> = workspaceManager.rootDir.walkTopDown()
        .filter { it.isFile }
        .mapNotNull { file: File ->
            val text = runCatching { file.readText() }.getOrDefault("")
            if (text.contains(query, ignoreCase = true)) file.relativeTo(workspaceManager.rootDir).path else null
        }
        .take(100)
        .toList()

    private fun workspaceFile(path: String): File {
        val sanitized = path.removePrefix("/")
        val f = workspaceManager.resolve(sanitized)
        require(workspaceManager.safeWithinWorkspace(f)) { "Path escapes workspace" }
        return f
    }
}
