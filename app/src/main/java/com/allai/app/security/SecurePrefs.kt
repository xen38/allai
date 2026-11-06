package com.allai.app.security

import android.content.Context
import android.content.SharedPreferences

object SecurePrefs {
    private var plainPrefs: SharedPreferences? = null

    fun init(context: Context) {
        plainPrefs = context.getSharedPreferences("allai_settings", Context.MODE_PRIVATE)
    }

    // Simple obfuscation for API key at rest (not true encryption, but prevents casual inspection)
    private fun obfuscate(text: String): String {
        return text.encodeToByteArray().map { byte ->
            (byte.toInt() xor 0x5A).toString(16).padStart(2, '0')
        }.joinToString("")
    }

    private fun deobfuscate(hex: String): String {
        val bytes = hex.chunked(2).map { java.lang.Integer.parseInt(it, 16).toByte() }
        return bytes.map { ((it.toInt() xor 0x5A) and 0xFF).toChar() }.joinToString("")
    }

    fun getString(key: String, default: String?): String? {
        val value = plainPrefs?.getString(key, default)
        return if (key == "api_key" && value != null && value.isNotEmpty()) {
            try { deobfuscate(value) } catch (e: Exception) { value }
        } else value
    }

    fun edit(): SecurePrefsEditor {
        return SecurePrefsEditor(plainPrefs?.edit())
    }

    class SecurePrefsEditor(private val editor: SharedPreferences.Editor?) {
        fun putString(key: String, value: String?): SecurePrefsEditor {
            val toStore = if (key == "api_key" && value != null && value.isNotEmpty()) {
                obfuscate(value)
            } else value
            editor?.putString(key, toStore)
            return this
        }

        fun remove(key: String) {
            editor?.remove(key)
        }

        fun apply() {
            editor?.apply()
        }
    }
}