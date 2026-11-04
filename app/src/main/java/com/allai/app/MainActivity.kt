package com.allai.app

import android.content.SharedPreferences
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.allai.app.settings.SettingsState
import com.allai.app.theme.AllaiTheme
import com.allai.app.theme.AllaiBackground
import com.allai.app.ui.AgentScreen
import com.allai.app.ui.MemoriesScreen
import com.allai.app.ui.OnboardingScreen
import com.allai.app.ui.SettingsTabsScreen
import com.allai.app.ui.TerminalScreen

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
    val context = LocalContext.current
    val prefs: SharedPreferences = context.getSharedPreferences("allai_onboarding", android.content.Context.MODE_PRIVATE)
    var onboardingComplete by remember { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }

    if (!onboardingComplete || settings.apiKey.isBlank()) {
        OnboardingScreen(onComplete = {
            prefs.edit().putBoolean("onboarding_complete", true).apply()
            onboardingComplete = true
        })
    } else {
        var selected by remember { mutableIntStateOf(0) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (selected) {
                    0 -> AgentScreen(settings)
                    1 -> TerminalScreen()
                    2 -> SessionsScreen()
                    3 -> SettingsTabsScreen(settings, { selected = it })
                }
            }

            BottomBar(selected = selected, onTabSelected = { selected = it })
        }
    }
}

@Composable
private fun SessionsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(AllaiBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Sessions",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
        )
        androidx.compose.material3.Text(
            text = "Your chat sessions will appear here.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BottomBar(selected: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Chat",
            selected = selected == 0,
            onClick = { onTabSelected(0) }
        )
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Terminal",
            selected = selected == 1,
            onClick = { onTabSelected(1) }
        )
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Sessions",
            selected = selected == 2,
            onClick = { onTabSelected(2) }
        )
        BottomTab(
            modifier = Modifier.weight(1f),
            text = "Settings",
            selected = selected == 3,
            onClick = { onTabSelected(3) }
        )
    }
}


@Composable
private fun BottomTab(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
}
