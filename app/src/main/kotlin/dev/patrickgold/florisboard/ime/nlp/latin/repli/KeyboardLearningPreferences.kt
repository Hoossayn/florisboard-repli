package dev.patrickgold.florisboard.ime.nlp.latin.repli

import android.content.Context

class KeyboardLearningPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, true)
        set(value) { preferences.edit().putBoolean(KEY_ENABLED, value).apply() }

    val revision: Int get() = preferences.getInt(KEY_REVISION, 0)

    fun markCleared() {
        preferences.edit().putInt(KEY_REVISION, revision + 1).apply()
    }

    private companion object {
        const val PREFERENCES = "keyboard_learning"
        const val KEY_ENABLED = "enabled"
        const val KEY_REVISION = "revision"
    }
}
