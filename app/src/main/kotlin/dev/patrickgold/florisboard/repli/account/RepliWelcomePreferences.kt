package dev.patrickgold.florisboard.repli.account

import android.content.Context

/** Show the product tour once, including for people who signed in before opening the app. */
object RepliWelcomePreferences {
    private const val NAME = "repli_welcome"
    private const val COMPLETED = "completed"

    fun shouldShow(context: Context): Boolean =
        !context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(COMPLETED, false)

    fun complete(context: Context) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(COMPLETED, true).apply()
    }
}
