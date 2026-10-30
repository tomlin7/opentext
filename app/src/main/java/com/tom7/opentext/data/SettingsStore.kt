package com.tom7.opentext.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("opentext_settings", Context.MODE_PRIVATE)

    private val _wordWrap = mutableStateOf(prefs.getBoolean("word_wrap", false))
    var wordWrap: Boolean
        get() = _wordWrap.value
        set(value) {
            _wordWrap.value = value
            prefs.edit().putBoolean("word_wrap", value).apply()
        }

    private val _showLineNumbers = mutableStateOf(prefs.getBoolean("show_line_numbers", true))
    var showLineNumbers: Boolean
        get() = _showLineNumbers.value
        set(value) {
            _showLineNumbers.value = value
            prefs.edit().putBoolean("show_line_numbers", value).apply()
        }

    private val _fontSize = mutableStateOf(prefs.getFloat("font_size", 14f))
    var fontSize: Float
        get() = _fontSize.value
        set(value) {
            _fontSize.value = value
            prefs.edit().putFloat("font_size", value).apply()
        }

    private val _tabSize = mutableStateOf(prefs.getInt("tab_size", 4))
    var tabSize: Int
        get() = _tabSize.value
        set(value) {
            _tabSize.value = value
            prefs.edit().putInt("tab_size", value).apply()
        }
}
