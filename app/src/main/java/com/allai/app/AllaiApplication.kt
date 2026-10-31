package com.allai.app

import android.app.Application

class AllaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Register default plugins
        com.allai.app.plugin.PluginManager.register(com.allai.app.plugin.ContactsPlugin())
        com.allai.app.plugin.PluginManager.register(com.allai.app.plugin.SmsPlugin())
        com.allai.app.plugin.PluginManager.register(com.allai.app.plugin.AppLauncherPlugin())
    }
}
