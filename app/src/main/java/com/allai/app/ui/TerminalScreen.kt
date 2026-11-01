package com.allai.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import com.allai.app.terminal.TerminalBridge
import com.allai.app.theme.*

@Composable
fun TerminalScreen() {
    val log = remember { mutableStateListOf<String>() }
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize().background(AllaiBackground)) {
        Text(
            text = "Terminal",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(8.dp)
                .glassCard(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(log, key = { it }) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (line.startsWith("ERR")) Color(0xFFFF4D4D) else Color(0xFF00D4FF),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(vertical = 8.dp)
                    .background(Color(0xFF1A102E), MaterialTheme.shapes.medium)
                    .padding(12.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
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
                Text(text = "▶", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
