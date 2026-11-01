package com.allai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.allai.app.memory.MemoryStore
import com.allai.app.model.Memory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val AllaiSurface = Color(0xFF0F172A)
val AllaiOnSurface = Color(0xFFF8FAFC)
val AllaiOnSurfaceVariant = Color(0xFFCBD5E1)
val AllaiPrimary = Color(0xFF6366F1)

@Composable
fun MemoriesScreen() {
    val context = LocalContext.current
    val store = MemoryStore.getInstance(context)
    val memoryList = remember { mutableStateListOf<Memory>().apply { addAll(store.getMemories()) } }
    var showingAdd by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newAction by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(AllaiSurface)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = memoryList,
                key = { it.id }
            ) { memory ->
                MemoryCard(memory, store, { memoryList.clear(); memoryList.addAll(store.getMemories()) })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${memoryList.size} memories",
                style = MaterialTheme.typography.bodySmall,
                color = AllaiOnSurfaceVariant
            )
            Button(onClick = { showingAdd = true }) {
                Text(text = "Add Memory")
            }
        }

        if (showingAdd) {
            AddMemoryDialog(
                onDismiss = { showingAdd = false },
                onSave = { title, desc, action ->
                    store.addMemory(
                        Memory(
                            id = java.util.UUID.randomUUID().toString(),
                            title = title,
                            description = desc,
                            action = action,
                            arguments = ""
                        )
                    )
                    showingAdd = false
                    memoryList.clear()
                    memoryList.addAll(store.getMemories())
                },
                title = newTitle,
                desc = newDesc,
                action = newAction,
                onTitleChange = { newTitle = it },
                onDescChange = { newDesc = it },
                onActionChange = { newAction = it }
            )
        }
    }
}

@Composable
private fun MemoryCard(
    memory: Memory,
    store: MemoryStore,
    onRefresh: () -> Unit
) {
    var fav by remember { mutableStateOf(memory.favorite) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = AllaiSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = memory.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AllaiOnSurface
                )
                IconButton(onClick = {
                    store.toggleFavorite(memory.id)
                    fav = !fav
                    onRefresh()
                }) {
                    Text(text = if (fav) "★" else "☆", color = Color(0xFFEF4444))
                }
                IconButton(onClick = {
                    store.deleteMemory(memory.id)
                    onRefresh()
                }) {
                    Text(text = "🗑", color = Color(0xFFEF4444))
                }
            }
            Text(
                text = memory.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AllaiOnSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Action: ${memory.action}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AllaiPrimary
                )
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(memory.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = AllaiOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    title: String,
    desc: String,
    action: String,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onActionChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Memory") },
        text = {
            Column(modifier = Modifier.width(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(text = "Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = onDescChange,
                    label = { Text(text = "Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = action,
                    onValueChange = onActionChange,
                    label = { Text(text = "Action (e.g. shell, sms, launcher)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onSave(title, desc, action)
            }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
