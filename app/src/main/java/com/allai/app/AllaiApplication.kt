package com.allai.app

import android.app.Application
import com.allai.app.security.SecurePrefs

class AllaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SecurePrefs.init(this)
        com.allai.app.plugin.PluginManager.init(this)
    }
}
