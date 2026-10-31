package com.allai.app

import android.app.Application

class AllaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.allai.app.plugin.PluginManager.init(this)
    }
}
