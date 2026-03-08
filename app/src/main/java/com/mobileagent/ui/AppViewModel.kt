package com.mobileagent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.mobileagent.agent.AgentController
import com.mobileagent.agent.AgentLoop
import com.mobileagent.agent.ToolExecutor
import com.mobileagent.agent.ToolRegistry
import com.mobileagent.llm.AnthropicProvider
import com.mobileagent.llm.LLMClient
import com.mobileagent.llm.LiteLLMProvider
import com.mobileagent.llm.MockProvider
import com.mobileagent.llm.OpenAIProvider
import com.mobileagent.llm.OpenRouterProvider
import com.mobileagent.llm.ProviderConfig
import com.mobileagent.models.AgentState
import com.mobileagent.models.Message
import com.mobileagent.storage.ConversationDatabase
import com.mobileagent.storage.FileRepository
import com.mobileagent.storage.WorkspaceManager
import com.mobileagent.tools.ListFilesTool
import com.mobileagent.tools.ReadFileTool
import com.mobileagent.tools.SearchCodeTool
import com.mobileagent.tools.WriteFileTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val workspace = WorkspaceManager()
    private val files = FileRepository(workspace)
    private val dao = ConversationDatabase.get(app).messageDao()
    private val llm = LLMClient(MockProvider())
    private val controller = AgentController(
        AgentLoop(
            llm,
            dao,
            ToolExecutor(ToolRegistry(listOf(
                ReadFileTool(files),
                WriteFileTool(files),
                ListFilesTool(files),
                SearchCodeTool(files)
            )))
        )
    )

    private val _chat = MutableStateFlow<List<Message>>(emptyList())
    val chat: StateFlow<List<Message>> = _chat

    private val _editorText = MutableStateFlow("// Select file from explorer")
    val editorText: StateFlow<String> = _editorText

    private val _fileList = MutableStateFlow<List<String>>(emptyList())
    val fileList: StateFlow<List<String>> = _fileList

    private val _agentState = MutableStateFlow(AgentState())
    val agentState: StateFlow<AgentState> = _agentState

    val securePrefs by lazy {
        val key = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "api_keys",
            key,
            app,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        refreshFiles()
    }

    fun refreshFiles() {
        _fileList.value = runCatching { files.list(".") }.getOrDefault(emptyList())
    }

    fun openFile(path: String) {
        _editorText.value = runCatching { files.read(path) }.getOrDefault("Failed to open $path")
    }

    fun saveFile(path: String, content: String) {
        runCatching { files.write(path, content) }
        refreshFiles()
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank()) return
        _chat.value = _chat.value + Message(role = "user", content = prompt)
        _agentState.value = _agentState.value.copy(isRunning = true, lastError = null)

        viewModelScope.launch {
            var buffer = ""
            controller.submit(prompt).collect { delta ->
                buffer += delta
                upsertAssistantMessage(buffer)
            }
            _agentState.value = _agentState.value.copy(isRunning = false)
        }
    }

    private fun upsertAssistantMessage(content: String) {
        val messages = _chat.value.toMutableList()
        val last = messages.lastOrNull()
        if (last?.role == "assistant") {
            messages[messages.lastIndex] = last.copy(content = content)
        } else {
            messages += Message(role = "assistant", content = content)
        }
        _chat.value = messages
    }

    fun saveApiKey(provider: String, key: String) {
        securePrefs.edit().putString("${provider}_key", key).apply()
    }

    fun switchProvider(provider: String, model: String, baseUrl: String) {
        val key = securePrefs.getString("${provider}_key", "").orEmpty()
        val config = ProviderConfig(baseUrl = baseUrl, model = model, apiKey = key, provider = provider)
        val selected = when (provider) {
            "openai" -> OpenAIProvider(config)
            "anthropic" -> AnthropicProvider(config)
            "openrouter" -> OpenRouterProvider(config)
            "litellm" -> LiteLLMProvider(config)
            else -> MockProvider()
        }
        llm.updateProvider(selected)
        _agentState.value = _agentState.value.copy(currentProvider = provider, currentModel = model)
    }
}
