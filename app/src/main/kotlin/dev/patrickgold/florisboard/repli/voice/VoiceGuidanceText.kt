package dev.patrickgold.florisboard.repli.voice

import dev.patrickgold.florisboard.repli.suggestions.RemoteReplyPrivacyPolicy
import java.util.Locale

object VoiceGuidanceText {
    fun combine(draft: String, transcript: String): String? {
        val text = transcript.trim()
        if (text.isBlank()) return null
        val combined = if (draft.isBlank()) text else draft + (if (draft.endsWith("\n")) "" else "\n") + text
        return combined.takeIf { it.length <= RemoteReplyPrivacyPolicy.MAX_INSTRUCTION_CHARACTERS }
    }

    /** Regional fallback is allowed only within the user's language, and only to an installed model. */
    fun installedLanguage(requested: String, installed: List<String>): String? =
        installed.firstOrNull { it.equals(requested, ignoreCase = true) }
            ?: installed.firstOrNull { Locale.forLanguageTag(it).language == Locale.forLanguageTag(requested).language }
}
