package dev.patrickgold.florisboard.repli.capture

/** One bounded sequence of cropped chat images held until automatic AI reading begins.
 * Images remain in the exact oldest-to-newest order captured by the user.
 * Every terminal path overwrites all PNG bytes. */
data class PendingVisionCapture internal constructor(
    val requestId: String,
    val images: List<ByteArray>,
    val baseTurns: List<ConversationTurn>,
    val localTurns: List<ConversationTurn>,
) {
    fun eraseImages() = images.forEach { it.fill(0) }
}

object PendingVisionCaptureStore {
    private var pending: PendingVisionCapture? = null

    @Synchronized
    fun put(value: PendingVisionCapture) {
        pending?.eraseImages()
        pending = value
    }

    @Synchronized
    fun peek(requestId: String): PendingVisionCapture? = pending?.takeIf { it.requestId == requestId }

    @Synchronized
    fun take(requestId: String): PendingVisionCapture? {
        val value = pending?.takeIf { it.requestId == requestId } ?: return null
        pending = null
        return value
    }

    @Synchronized
    fun discard(requestId: String? = null) {
        val value = pending ?: return
        if (requestId != null && value.requestId != requestId) return
        pending = null
        value.eraseImages()
    }
}
