package com.allai.app.floating

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allai.app.AllaiApplication
import com.allai.app.theme.AllaiTheme
import com.allai.app.settings.SettingsState
import com.allai.app.agent.AgentLoop
import com.allai.app.model.Message
import com.allai.app.model.Role
import com.allai.app.theme.*

class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private var params: WindowManager.LayoutParams? = null
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var initialParamsX: Int = 0
    private var initialParamsY: Int = 0

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params!!.gravity = Gravity.TOP or Gravity.START
        params!!.x = 20
        params!!.y = 100
        params!!.width = 360
        params!!.height = 560

        overlayView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AllaiTheme {
                    FloatingChatOverlay(onClose = { stopSelf() })
                }
            }
        }

        overlayView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialParamsX = params!!.x
                    initialParamsY = params!!.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params!!.x = (initialParamsX + (event.rawX - initialTouchX)).toInt()
                    params!!.y = (initialParamsY + (event.rawY - initialTouchY)).toInt()
                    windowManager.updateViewLayout(overlayView, params!!)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, params!!)
    }

    private fun hideOverlay() {
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) { }
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, FloatingWindowService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }
    }
}

@Composable
private fun FloatingChatOverlay(onClose: () -> Unit) {
    val context = LocalContext.current
    val settings = SettingsState(context)
    val messages = mutableStateListOf<String>()
    val inputState = remember { mutableStateOf<String>("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .width(340.dp)
            .height(540.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🫧 Allai",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) {
                Text(text = "✕")
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                items = messages,
                key = { message -> message.hashCode().toString() }
            ) { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.startsWith("You:")) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(8.dp, 6.dp)
                            .background(
                                if (message.startsWith("You:")) Color(0xFF4A148C) else Color(0xFF003366),
                                MaterialTheme.shapes.medium
                            )
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputState.value,
                onValueChange = { newValue -> inputState.value = newValue },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
                    .background(Color(0xFF1A102E), MaterialTheme.shapes.medium)
                    .padding(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = {
                if (inputState.value.isNotBlank()) {
                    messages.add("You: ${inputState.value}")
                    inputState.value = ""
                    scope.launch {
                        try {
                            val agent = AgentLoop()
                            val config = AgentLoop.AgentConfig(
                                provider = settings.provider,
                                systemPrompt = settings.systemPrompt
                            )
                            val reply = agent.run(config, messages.map { Message(Role.User, it) }.toList())
                            messages.add("Allai: $reply")
                        } catch (e: Exception) {
                            messages.add("Allai: Error: ${e.message}")
                        }
                    }
                }
            }) {
                Text(text = "➤", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
