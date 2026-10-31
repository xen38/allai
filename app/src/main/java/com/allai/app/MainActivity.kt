package com.allai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.allai.app.settings.SettingsState
import com.allai.app.theme.AllaiTheme
import com.allai.app.ui.AgentScreen
import com.allai.app.ui.TerminalScreen
import com.allai.app.ui.SettingsScreen
import com.allai.app.ui.Scaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsState(this)
        setContent {
            AllaiTheme {
                MainNav(settings)
            }
        }
    }
}

@Composable
private fun MainNav(settings: SettingsState) {
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selected) {
                0 -> AgentScreen(settings)
                1 -> TerminalScreen()
                else -> SettingsScreen(settings)
            }
        }

        BottomBar(selected = selected, onTabSelected = { selected = it })
    }
}

@Composable
private fun BottomBar(selected: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTab(
            text = "Chat",
            selected = selected == 0,
            onClick = { onTabSelected(0) }
        )
        BottomTab(
            text = "Terminal",
            selected = selected == 1,
            onClick = { onTabSelected(1) }
        )
        BottomTab(
            text = "Settings",
            selected = selected == 2,
            onClick = { onTabSelected(2) }
        )
    }
}

@Composable
private fun BottomTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selected) {
            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
