package com.mobileagent.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobileagent.editor.CodeEditorComponent

@Composable
fun MainApp(vm: AppViewModel) {
    var selectedPath by remember { mutableStateOf("main.kt") }
    var leftWeight by remember { mutableFloatStateOf(0.22f) }
    var centerWeight by remember { mutableFloatStateOf(0.78f) }
    var showSettings by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mobile Coding Agent")
            Button(onClick = { showSettings = !showSettings }) { Text(if (showSettings) "Workspace" else "Settings") }
        }
        if (showSettings) {
            SettingsScreen(vm)
            return@Column
        }
        Row(Modifier.weight(1f)) {
            FileExplorerScreen(
                files = vm.fileList.collectAsStateWithLifecycle().value,
                onOpen = {
                    selectedPath = it
                    vm.openFile(it)
                },
                onRefresh = vm::refreshFiles,
                modifier = Modifier.weight(leftWeight).fillMaxHeight()
            )
            EditorScreen(
                path = selectedPath,
                text = vm.editorText.collectAsStateWithLifecycle().value,
                onSave = { vm.saveFile(selectedPath, it) },
                modifier = Modifier.weight(centerWeight).fillMaxHeight()
            )
        }

        Slider(
            value = leftWeight,
            onValueChange = {
                leftWeight = it.coerceIn(0.15f, 0.5f)
                centerWeight = 1f - leftWeight
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        ChatScreen(vm)
    }
}

@Composable
fun FileExplorerScreen(
    files: List<String>,
    onOpen: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Files")
            Button(onClick = onRefresh) { Text("Refresh") }
        }
        HorizontalDivider()
        LazyColumn {
            items(files) { file ->
                Text(file, Modifier.clickable { onOpen(file) }.padding(4.dp))
            }
        }
    }
}

@Composable
fun EditorScreen(path: String, text: String, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(8.dp)) {
        Text("Editor: $path")
        CodeEditorComponent(content = text, onSave = onSave)
    }
}

@Composable
fun ChatScreen(vm: AppViewModel) {
    var prompt by remember { mutableStateOf("") }
    val chat = vm.chat.collectAsStateWithLifecycle().value
    val state = vm.agentState.collectAsStateWithLifecycle().value

    Column(
        Modifier.fillMaxWidth().height(280.dp).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Agent Console (${state.currentProvider}:${state.currentModel})")
        LazyColumn(Modifier.weight(1f)) {
            items(chat) { msg -> Text("${msg.role}: ${msg.content}") }
        }
        Row {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.weight(1f),
                label = { Text(if (state.isRunning) "Running..." else "Ask the agent") }
            )
            Button(onClick = { vm.sendPrompt(prompt); prompt = "" }, enabled = !state.isRunning) {
                Text("Send")
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    var provider by remember { mutableStateOf("openai") }
    var model by remember { mutableStateOf("gpt-4o-mini") }
    var baseUrl by remember { mutableStateOf("https://api.openai.com/v1/chat/completions") }
    var apiKey by remember { mutableStateOf("") }

    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Provider Settings")
        OutlinedTextField(provider, { provider = it }, label = { Text("Provider: openai/anthropic/openrouter/litellm") })
        OutlinedTextField(model, { model = it }, label = { Text("Model") })
        OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL") })
        OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.saveApiKey(provider, apiKey) }) { Text("Save Key") }
            Button(onClick = { vm.switchProvider(provider, model, baseUrl) }) { Text("Activate") }
        }
    }
}
