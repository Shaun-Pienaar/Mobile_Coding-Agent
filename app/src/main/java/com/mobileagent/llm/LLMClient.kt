package com.mobileagent.llm

import com.mobileagent.models.LlmChunk
import com.mobileagent.models.Message
import com.mobileagent.models.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ProviderConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val provider: String
)

interface LLMProvider {
    fun stream(messages: List<Message>): Flow<LlmChunk>
}

class LLMClient(private var provider: LLMProvider) {
    fun updateProvider(newProvider: LLMProvider) {
        provider = newProvider
    }

    fun streamCompletion(messages: List<Message>): Flow<LlmChunk> = provider.stream(messages)
}

open class HttpStreamingProvider(
    private val config: ProviderConfig,
    private val client: OkHttpClient = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
) : LLMProvider {
    override fun stream(messages: List<Message>): Flow<LlmChunk> = flow {
        val payload = JSONObject().apply {
            put("model", config.model)
            put("stream", true)
            put("messages", JSONArray(messages.map {
                JSONObject().put("role", it.role).put("content", it.content.takeLast(6_000))
            }))
            put(
                "tools", JSONArray()
                    .put(JSONObject().put("name", "read_file"))
                    .put(JSONObject().put("name", "write_file"))
                    .put(JSONObject().put("name", "list_files"))
                    .put(JSONObject().put("name", "search_code"))
            )
        }

        val builder = Request.Builder()
            .url(config.baseUrl)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))

        if (config.apiKey.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer ${config.apiKey}")
        }
        if (config.provider == "anthropic") {
            builder.addHeader("x-api-key", config.apiKey)
            builder.addHeader("anthropic-version", "2023-06-01")
        }

        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                emit(LlmChunk(textDelta = "LLM error ${resp.code}: ${resp.message}"))
                emit(LlmChunk(done = true))
                return@use
            }

            val body = resp.body?.string().orEmpty()
            parsePseudoStream(body).forEach { emit(it) }
            emit(LlmChunk(done = true))
        }
    }.flowOn(Dispatchers.IO)

    protected open fun parsePseudoStream(body: String): List<LlmChunk> {
        val text = body.trim()
        if (text.isEmpty()) return emptyList()

        return runCatching {
            val obj = JSONObject(text)
            val tool = obj.optJSONObject("tool_call")
            if (tool != null) {
                listOf(
                    LlmChunk(
                        toolCall = ToolCall(
                            id = tool.optString("id", UUID.randomUUID().toString()),
                            name = tool.optString("name"),
                            arguments = tool.optJSONObject("arguments")?.let { args ->
                                args.keys().asSequence().associateWith { key -> args.optString(key) }
                            }.orEmpty()
                        )
                    )
                )
            } else {
                obj.optString("content").chunked(128).map { LlmChunk(textDelta = it) }
            }
        }.getOrElse {
            text.chunked(128).map { chunk -> LlmChunk(textDelta = chunk) }
        }
    }
}

class OpenAIProvider(config: ProviderConfig) : HttpStreamingProvider(config)
class AnthropicProvider(config: ProviderConfig) : HttpStreamingProvider(config)
class OpenRouterProvider(config: ProviderConfig) : HttpStreamingProvider(config)
class LiteLLMProvider(config: ProviderConfig) : HttpStreamingProvider(config)

class MockProvider : LLMProvider {
    override fun stream(messages: List<Message>): Flow<LlmChunk> = flow {
        val prompt = messages.lastOrNull()?.content.orEmpty()
        if (prompt.contains("list", ignoreCase = true)) {
            emit(LlmChunk(toolCall = ToolCall(UUID.randomUUID().toString(), "list_files", mapOf("directory" to "."))))
            emit(LlmChunk(done = true))
            return@flow
        }

        "Ready to help with your workspace. Ask me to read, write, list, or search files."
            .chunked(16)
            .forEach {
                emit(LlmChunk(textDelta = it))
                delay(35)
            }
        emit(LlmChunk(done = true))
    }
}
