package com.allai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.allai.app.model.ToolResult

@Composable
fun TerminalScreen() {
    val log by remember { mutableStateListOf<String>() }
    val command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Terminal", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(log) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = { Text(text = "> ") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                if (command.isNotBlank()) {
                    log.add("$command")
                    scope.launch {
                        try {
                            val result = com.allai.app.terminal.TerminalBridge.run(command)
                            log.add(result.output)
                            if (result.error.isNotEmpty()) log.add("ERR: ${result.error}")
                        } catch (e: Exception) {
                            log.add("ERR: ${e.message}")
                        }
                    }
                    command = ""
                }
            }) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
