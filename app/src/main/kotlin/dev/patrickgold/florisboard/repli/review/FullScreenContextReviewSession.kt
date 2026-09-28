package dev.patrickgold.florisboard.repli.review

import dev.patrickgold.florisboard.repli.capture.ReplyEditor

/** Scopes the one IME-hide exception to a user-opened full-screen review round trip. */
object FullScreenContextReviewSession {
    private data class Session(val requestId: String, val returning: Boolean)

    @Volatile private var session: Session? = null

    fun begin(requestId: String) {
        session = requestId.takeIf(String::isNotBlank)?.let { Session(it, returning = false) }
    }

    fun markReturning(requestId: String) {
        if (session?.requestId == requestId) session = Session(requestId, returning = true)
    }

    fun markRestored(requestId: String) {
        if (session?.requestId == requestId) session = Session(requestId, returning = false)
    }

    fun isActive(requestId: String): Boolean = session?.requestId == requestId

    fun isReturning(requestId: String): Boolean = session?.let {
        it.requestId == requestId && it.returning
    } == true

    /** The host app may recreate its composer while this activity is closing, changing the
     * field ID/name even though Android is returning to the same chat app. Package-only matching
     * is allowed exactly during this request-scoped return; ordinary input starts remain strict. */
    fun acceptsEditor(
        requestId: String,
        original: ReplyEditor,
        candidate: ReplyEditor?,
        imePackage: String,
    ): Boolean {
        val current = session ?: return false
        if (current.requestId != requestId || candidate == null) return false
        return candidate == original ||
            (current.returning && candidate.packageName == original.packageName &&
                candidate.packageName != imePackage)
    }

    fun end(requestId: String) {
        if (session?.requestId == requestId) session = null
    }
}
