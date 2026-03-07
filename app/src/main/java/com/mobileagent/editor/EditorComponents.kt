package com.mobileagent.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

class SyntaxHighlighter {
    fun highlight(input: String): String = input
}

@Composable
fun CodeEditorComponent(
    content: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(content) { mutableStateOf(content) }
    val horizontal = rememberScrollState()

    Column(modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { onSave(text) }) { Text("Save") }
        }
        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(horizontal)
        ) {
            Text(
                text = (1..(text.count { it == '\n' } + 1)).joinToString("\n"),
                style = TextStyle(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(end = 8.dp)
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
