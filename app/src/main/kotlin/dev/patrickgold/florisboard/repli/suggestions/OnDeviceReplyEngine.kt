package dev.patrickgold.florisboard.repli.suggestions

import dev.patrickgold.florisboard.repli.capture.ConversationTurn
import dev.patrickgold.florisboard.repli.profile.LearnedTextingStyle
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class DeviceReplyResult(val replies: List<String>, val explanation: String)

/** Bundled, real ML inference. Never substitutes canned replies for a model failure. */
class OnDeviceReplyEngine : DeviceReplyGenerator {
    private val generator by lazy { SmartReply.getClient() }
    private var used = false

    override suspend fun suggest(
        turns: List<ConversationTurn>,
        learnedStyle: LearnedTextingStyle?,
    ): DeviceReplyResult {
        if (turns.isEmpty()) return DeviceReplyResult(emptyList(), "No messages found. Capture again or review the context.")
        used = true
        val now = System.currentTimeMillis()
        val conversation = turns.takeLast(RemoteReplyPrivacyPolicy.MAX_CONTEXT_TURNS).mapIndexed { index, turn ->
            val time = now - (10 - index) * 1_000L
            if (turn.fromMe) TextMessage.createForLocalUser(turn.text, time)
            else TextMessage.createForRemoteUser(turn.text, time, "other-person")
        }
        val result = generator.suggestReplies(conversation).awaitResult()
        val rawReplies = result.suggestions.map { it.text }.filter(String::isNotBlank).distinct().take(3)
        val replies = learnedStyle?.let { SuggestionStyleAdapter.adapt(rawReplies, it) } ?: rawReplies
        val explanation = when {
            result.status == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE ->
                "On-device replies currently support English. You can still review the captured text."
            replies.isEmpty() -> "The model couldn't find a confident reply. Review the context or add a newer message."
            else -> "On-device replies · tap to insert"
        }
        return DeviceReplyResult(replies, explanation)
    }

    override fun close() { if (used) generator.close() }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
