package com.allai.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.app.ForegroundServiceCompat

class AgentService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForegroundService(this, Intent(this, AgentService::class.java))
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
