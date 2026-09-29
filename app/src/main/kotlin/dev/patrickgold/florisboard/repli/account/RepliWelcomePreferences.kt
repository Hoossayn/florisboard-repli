package dev.patrickgold.florisboard.repli.account

import android.content.Context

/** The account invitation appears once; the keyboard can still be used without cloud replies. */
object RepliWelcomePreferences {
    private const val NAME = "repli_welcome"
    private const val COMPLETED = "completed"

    fun shouldShow(context: Context): Boolean =
        !context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(COMPLETED, false) &&
            !RepliFirebaseAccountManager.state.value.signedIn

    fun complete(context: Context) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(COMPLETED, true).apply()
    }
}
