package dev.patrickgold.florisboard.repli.data

import android.content.Context

class AutoScrollPreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) { preferences.edit().putBoolean(KEY_ENABLED, value).apply() }

    private companion object {
        const val FILE_NAME = "reply_auto_scroll"
        const val KEY_ENABLED = "enabled"
    }
}
