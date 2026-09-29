package dev.patrickgold.florisboard.repli.suggestions

import dev.patrickgold.florisboard.repli.capture.ConversationTurn
import dev.patrickgold.florisboard.repli.profile.LearnedTextingStyle
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

interface DeviceReplyGenerator : AutoCloseable {
    suspend fun suggest(
        turns: List<ConversationTurn>,
        learnedStyle: LearnedTextingStyle? = null,
    ): DeviceReplyResult

    override fun close() = Unit
}

/** Remote first only after approval; every remote failure resolves through bundled inference. */
class ReplyGenerationCoordinator(
    private val device: DeviceReplyGenerator,
    private val remote: RemoteReplyGenerator?,
) : AutoCloseable {
    suspend fun suggest(
        turns: List<ConversationTurn>,
        learnedStyle: LearnedTextingStyle?,
        approvedRemoteRequest: PreparedRemoteReplyRequest? = null,
        instructions: String? = approvedRemoteRequest?.instructions,
    ): ReplyGenerationResult {
        var failure = CloudReplyFailure.UNAVAILABLE
        if (remote != null && approvedRemoteRequest != null) {
            try {
                val remoteReplies = withTimeout(REMOTE_TIMEOUT_MS) {
                    requireThreeCandidates(remote.suggest(approvedRemoteRequest))
                }
                return ReplyGenerationResult(
                    replies = remoteReplies,
                    explanation = "Tap to insert, then edit",
                    origin = ReplyOrigin.REMOTE,
                )
            } catch (_: TimeoutCancellationException) {
                failure = CloudReplyFailure.TIMEOUT
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // Use only fixed labels, never exception messages, provider bodies, or tokens.
                failure = when (error) {
                    is RemoteReplyException -> error.reason
                    is SocketTimeoutException -> CloudReplyFailure.TIMEOUT
                    is IOException -> CloudReplyFailure.CONNECTION
                    else -> CloudReplyFailure.UNAVAILABLE
                }
            }
        }

        val local = device.suggest(turns, learnedStyle)
        val fellBack = approvedRemoteRequest != null
        val guidanceNotice = if (!instructions.isNullOrBlank()) "Guidance not applied on device · " else ""
        return ReplyGenerationResult(
            replies = local.replies,
            explanation = if (fellBack) {
                "${failure.label} · $guidanceNotice${local.explanation}"
            } else {
                "$guidanceNotice${local.explanation}"
            },
            origin = if (fellBack) ReplyOrigin.ON_DEVICE_FALLBACK else ReplyOrigin.ON_DEVICE,
        )
    }

    override fun close() = device.close()

    private companion object {
        // Covers a possible identity exchange plus the bounded provider request.
        const val REMOTE_TIMEOUT_MS = 25_000L
    }
}
