package com.mobileagent.storage

import java.io.File

class WorkspaceManager {
    val rootDir: File = File("/storage/emulated/0/Android/data/com.mobileagent.workspace/")

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    fun resolve(path: String): File = File(rootDir, path).canonicalFile

    fun safeWithinWorkspace(file: File): Boolean = file.path.startsWith(rootDir.canonicalPath)
}
