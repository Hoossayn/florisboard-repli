package dev.patrickgold.florisboard.repli.capture

enum class ManualCaptureDecision {
    CONTINUE,
    SHORT_CHAT,
    USER_DONE,
    FRAME_LIMIT,
    TIME_LIMIT,
}

/** Bounds the user-started, explicitly selected page-capture window. */
object ReplyManualCapturePolicy {
    const val MAX_DURATION_MS = 180_000L
    const val MAX_FRAMES = 4

    fun afterFrame(
        framesCaptured: Int,
        totalTurns: Int,
        elapsedMs: Long,
    ): ManualCaptureDecision = when {
        framesCaptured == 1 && totalTurns == 0 -> ManualCaptureDecision.SHORT_CHAT
        framesCaptured >= MAX_FRAMES -> ManualCaptureDecision.FRAME_LIMIT
        elapsedMs >= MAX_DURATION_MS -> ManualCaptureDecision.TIME_LIMIT
        else -> ManualCaptureDecision.CONTINUE
    }
}
