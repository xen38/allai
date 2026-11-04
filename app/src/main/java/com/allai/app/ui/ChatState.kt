package com.allai.app.ui

import androidx.compose.runtime.mutableStateListOf
import com.allai.app.model.Message

object ChatState {
    val messages = mutableStateListOf<Message>()
}
