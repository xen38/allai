package com.allai.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.allai.app.theme.AllaiTheme

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    AllaiTheme {
        Column(modifier = modifier) {
            val padding = contentPaddingValues()
            Box(modifier = Modifier.fillMaxSize()) {
                content(padding)
            }
            bottomBar()
        }
    }
}

private fun contentPaddingValues(): PaddingValues = PaddingValues(16.dp)

@Composable
fun EmptyScreen(title: String, subtitle: String, icon: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
