package com.allai.app.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.allai.app.terminal.TerminalBridge
import com.allai.app.theme.*

@Composable
fun TerminalScreen() {
    val log = remember { mutableStateListOf<String>() }
    var command by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberLazyListState()
    var isRunning by remember { mutableStateOf(false) }
    var currentJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Auto-scroll to bottom when new content is added
    LaunchedEffect(log.size) {
        scrollState.scrollToItem(log.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(AllaiBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Terminal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isRunning) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .padding(8.dp)
                .glassCard(),
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(log, key = { it }) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (line.startsWith("ERR")) Color(0xFFFF4D4D)
                    else if (line.startsWith(">")) Color(0xFF00D4FF)
                    else Color(0xFFE8EAF6),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            BasicTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(vertical = 10.dp)
                    .background(Color(0xFF1A102E), MaterialTheme.shapes.large)
                    .padding(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (isRunning) {
                IconButton(onClick = {
                    currentJob?.cancel()
                    isRunning = false
                    currentJob = null
                    log.add("[Process cancelled by user]")
                }, enabled = isRunning) {
                    Text(text = "\u23F9", color = Color(0xFFFF4D4D))
                }
            } else {
                IconButton(
                    onClick = {
                        if (command.isNotBlank()) {
                            val cmd = command
                            log.add("> $cmd")
                            command = ""
                            isRunning = true
                            currentJob = scope.launch {
                                try {
                                    val result = TerminalBridge.run(cmd)
                                    if (result.output.isNotEmpty()) log.add(result.output)
                                    if (result.error.isNotEmpty()) log.add("ERR: ${result.error}")
                                    log.add("[Exit code: ${result.exitCode}]")
                                } catch (e: Exception) {
                                    log.add("ERR: ${e.message}")
                                } finally {
                                    isRunning = false
                                    currentJob = null
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    enabled = command.isNotBlank() && !isRunning
                ) {
                    Text(text = "\u25B6", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Button(
                onClick = {
                    log.clear()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                enabled = log.isNotEmpty()
            ) {
                Text(text = "Clear Terminal")
            }
        }
    }
}