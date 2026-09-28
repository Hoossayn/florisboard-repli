package dev.patrickgold.florisboard.repli.data

import android.content.Context

/** The remote path is off until the user accepts the disclosure in the main app. */
class RemoteGenerationPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) { preferences.edit().putBoolean(KEY_ENABLED, value).apply() }

    private companion object {
        const val PREFERENCES = "remote_reply_generation"
        const val KEY_ENABLED = "enabled"
    }
}
