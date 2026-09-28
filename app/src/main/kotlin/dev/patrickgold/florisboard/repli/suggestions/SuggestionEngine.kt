package dev.patrickgold.florisboard.repli.suggestions

import dev.patrickgold.florisboard.repli.profile.VoiceProfile
import dev.patrickgold.florisboard.repli.profile.LearnedTextingStyle

data class SuggestionRequest(
    val incomingMessage: String,
    val profile: VoiceProfile,
    val learnedStyle: LearnedTextingStyle? = null,
)

interface SuggestionEngine {
    fun suggest(request: SuggestionRequest): List<String>
}
