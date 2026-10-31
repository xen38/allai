package com.allai.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.allai.app.terminal.TerminalBridge

@Composable
fun TerminalScreen() {
    val log = remember { mutableStateListOf<String>() }
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Terminal", style = MaterialTheme.typography.titleLarge)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(log, key = { it }) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = { Text(text = "> ") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                if (command.isNotBlank()) {
                    val cmd = command
                    log.add("$cmd")
                    scope.launch {
                        try {
                            val result = TerminalBridge.run(cmd)
                            log.add(result.output)
                            if (result.error.isNotEmpty()) log.add("ERR: ${result.error}")
                        } catch (e: Exception) {
                            log.add("ERR: ${e.message}")
                        }
                    }
                    command = ""
                }
            }) {
                Text(text = "Send")
            }
        }
    }
}
