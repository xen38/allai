package com.allai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.allai.app.memory.MemoryStore
import com.allai.app.model.Memory
import com.allai.app.model.LearnedAction
import com.allai.app.plugin.PluginManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.allai.app.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen() {
    val context = LocalContext.current
    val store = MemoryStore.getInstance(context)
    val memoryList = remember { mutableStateListOf<Memory>().apply { addAll(store.getMemories()) } }
    val actionList = remember { mutableStateListOf<LearnedAction>().apply { addAll(store.getLearnedActions()) } }
    var showingAdd by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newAction by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(AllaiBackground)) {
        TopAppBar(
            title = { Text(text = "Memories", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AllaiBackground),
            modifier = Modifier.fillMaxWidth()
        )

        PrimaryTabRow(
            selectedTabIndex = tab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(text = "Memories", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(text = "Learned Actions", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }

        when (tab) {
            0 -> MemoriesTab(store, memoryList)
            1 -> LearnedActionsTab(store, actionList)
        }

        if (tab == 0 && showingAdd) {
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

        if (tab == 0) {
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
        }
    }
}

@Composable
private fun MemoriesTab(
    store: MemoryStore,
    memoryList: MutableList<Memory>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = memoryList as List<Memory>,
            key = { it.id }
        ) { memory: Memory ->
            MemoryCard(memory, store, { memoryList.clear(); memoryList.addAll(store.getMemories()) })
        }
    }
}

@Composable
private fun LearnedActionsTab(
    store: MemoryStore,
    actionList: MutableList<LearnedAction>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Learned Actions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Actions Allai has learned from your phone usage and can replicate.",
            style = MaterialTheme.typography.bodyMedium,
            color = AllaiOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = actionList as List<LearnedAction>,
                key = { it.id }
            ) { action: LearnedAction ->
                LearnedActionCard(action, store) { actionList.clear(); actionList.addAll(store.getLearnedActions()) }
            }
        }
    }
}

@Composable
fun MemoryCard(
    memory: Memory,
    store: MemoryStore,
    onRefresh: () -> Unit
) {
    var fav by remember { mutableStateOf(memory.favorite) }
    var showingEdit by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(memory.title) }
    var editDesc by remember { mutableStateOf(memory.description) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A102E).copy(alpha = 0.8f),
                        Color(0xFF0F0A1E).copy(alpha = 0.9f)
                    )
                ),
                MaterialTheme.shapes.large
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7B2CBF).copy(alpha = 0.4f),
                        Color(0xFF00D4FF).copy(alpha = 0.4f)
                    )
                ),
                MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = memory.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AllaiOnSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        store.toggleFavorite(memory.id)
                        fav = !fav
                        onRefresh()
                    }) {
                        Text(text = if (fav) "★" else "☆", color = Color(0xFFFF4D4D))
                    }
                    IconButton(onClick = {
                        editTitle = memory.title
                        editDesc = memory.description
                        showingEdit = true
                    }) {
                        Text(text = "✏", color = AllaiSecondary)
                    }
                    IconButton(onClick = {
                        store.deleteMemory(memory.id)
                        onRefresh()
                    }) {
                        Text(text = "🗑", color = Color(0xFFFF4D4D))
                    }
                }
            }
            Text(
                text = memory.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AllaiOnSurfaceVariant
            )
        }
    }

    if (showingEdit) {
        EditMemoryDialog(
            onDismiss = { showingEdit = false },
            onSave = { title, desc ->
                store.updateMemory(memory.id, title, desc)
                showingEdit = false
                onRefresh()
            },
            title = editTitle,
            desc = editDesc,
            onTitleChange = { editTitle = it },
            onDescChange = { editDesc = it }
        )
    }
}

@Composable
fun LearnedActionCard(
    action: LearnedAction,
    store: MemoryStore,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A102E).copy(alpha = 0.8f),
                        Color(0xFF0F0A1E).copy(alpha = 0.9f)
                    )
                ),
                MaterialTheme.shapes.large
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7B2CBF).copy(alpha = 0.4f),
                        Color(0xFF00D4FF).copy(alpha = 0.4f)
                    )
                ),
                MaterialTheme.shapes.large
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A102E))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = action.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AllaiOnSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        val result = PluginManager.run(action.tool, action.arguments)
                        if (result.ok) {
                            store.recordAction(action.name, action.description, action.tool, action.arguments)
                            onRefresh()
                        }
                    }) {
                        Text(text = "▶", color = AllaiSecondary)
                    }
                    IconButton(onClick = {
                        store.deleteMemory(action.id)
                        onRefresh()
                    }) {
                        Text(text = "🗑", color = Color(0xFFFF4D4D))
                    }
                }
            }
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AllaiOnSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Tool: ${action.tool}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AllaiTertiary
                )
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(action.lastUsed)),
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

@Composable
private fun EditMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    title: String,
    desc: String,
    onTitleChange: (String) -> Unit,
    onDescChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Memory") },
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onSave(title, desc)
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
