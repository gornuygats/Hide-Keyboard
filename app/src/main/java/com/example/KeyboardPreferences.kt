package com.example

import android.content.Context
import android.content.SharedPreferences

enum class KeyboardMode(val id: String) {
    COMPLETELY_HIDDEN("hidden"),
    TRANSPARENT_BAR("bar"),
    FLOATING_BUTTON("floating");

    companion object {
        fun fromId(id: String?): KeyboardMode {
            return entries.find { it.id == id } ?: COMPLETELY_HIDDEN
        }
    }
}

class KeyboardPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode: KeyboardMode
        get() = KeyboardMode.fromId(prefs.getString(KEY_MODE, KeyboardMode.COMPLETELY_HIDDEN.id))
        set(value) = prefs.edit().putString(KEY_MODE, value.id).apply()

    var hardwarePassthrough: Boolean
        get() = prefs.getBoolean(KEY_HARDWARE_PASSTHROUGH, true)
        set(value) = prefs.edit().putBoolean(KEY_HARDWARE_PASSTHROUGH, value).apply()

    var showNotification: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NOTIFICATION, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_NOTIFICATION, value).apply()

    companion object {
        const val PREFS_NAME = "hideable_keyboard_prefs"
        const val KEY_MODE = "pref_keyboard_mode"
        const val KEY_HARDWARE_PASSTHROUGH = "pref_hardware_passthrough"
        const val KEY_SHOW_NOTIFICATION = "pref_show_notification"
    }
}
