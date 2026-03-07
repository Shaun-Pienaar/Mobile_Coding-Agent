# Mobile Coding Agent (Android)

Cloud-first mobile AI coding agent for Android, inspired by Claude Code style loops and nanoclaw concepts. Built in Kotlin with Jetpack Compose.

## What is included
- Native Android app (`minSdk 26`) with Compose UI.
- Mobile workspace rooted at:
  - `/storage/emulated/0/Android/data/com.mobileagent.workspace/`
- Claude-style agent loop:
  1. user prompt
  2. send prompt + history to LLM
  3. LLM may request tool
  4. tool executes
  5. result goes back into context
  6. loop continues until assistant final answer
- Structured tool calls:
  - `read_file(path)`
  - `write_file(path, content)`
  - `list_files(directory)`
  - `search_code(query)`
- Multi-provider LLM support:
  - OpenAI
  - Anthropic
  - OpenRouter
  - LiteLLM proxy
- Streaming output handling.
- Room conversation storage + summarization for large history.
- Encrypted API key storage (`EncryptedSharedPreferences`).

## UI layout (mobile-first)
- Top: workspace/settings switch.
- Main panel: resizable file explorer + editor split.
- Bottom panel: chat/agent console.

## Project structure
- `app/src/main/java/com/mobileagent/ui`
  - `ChatScreen`, `EditorScreen`, `FileExplorerScreen`, `SettingsScreen`
- `app/src/main/java/com/mobileagent/agent`
  - `AgentController`, `AgentLoop`, `ToolRegistry`, `ToolExecutor`
- `app/src/main/java/com/mobileagent/tools`
  - `ReadFileTool`, `WriteFileTool`, `ListFilesTool`, `SearchCodeTool`
- `app/src/main/java/com/mobileagent/llm`
  - `LLMClient`, `OpenAIProvider`, `AnthropicProvider`, `OpenRouterProvider`, `LiteLLMProvider`
- `app/src/main/java/com/mobileagent/storage`
  - `WorkspaceManager`, `FileRepository`, `ConversationDatabase`
- `app/src/main/java/com/mobileagent/editor`
  - `CodeEditorComponent`, `SyntaxHighlighter`
- `app/src/main/java/com/mobileagent/models`
  - `Message`, `ToolCall`, `ToolResult`, `AgentState`

## Example prompts
See `app/src/main/assets/example_prompts.txt`.

## Build APK
1. Install Android Studio + Android SDK 34.
2. Open project.
3. (Optional) create `local.properties` with `sdk.dir=...`.
4. Build debug APK:
   - `gradle assembleDebug`
5. Output:
   - `app/build/outputs/apk/debug/app-debug.apk`

## Notes for low-end devices
- Streaming token UI avoids large in-memory responses.
- History summarization protects context window and RAM.
- Large file reads are truncated for safer prompts.
- File search/list operation results are bounded.
