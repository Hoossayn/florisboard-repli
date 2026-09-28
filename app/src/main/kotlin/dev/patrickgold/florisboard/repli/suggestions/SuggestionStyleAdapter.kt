package dev.patrickgold.florisboard.repli.suggestions

import dev.patrickgold.florisboard.repli.profile.LearnedTextingStyle
import kotlin.math.abs

/** Applies the same learned texting habits to every suggestion source, including
 * bundled Smart Reply and the deterministic offline fallback. */
object SuggestionStyleAdapter {
    fun adapt(candidates: List<String>, style: LearnedTextingStyle): List<String> = candidates
        .mapIndexed { index, candidate ->
            var adapted = candidate
            if (style.lowercaseStartRatio >= 0.7) {
                adapted = adapted.replaceFirstChar(Char::lowercase)
            }
            if (style.terminalPunctuationRatio <= 0.25) {
                adapted = adapted.trimEnd('.', '!')
            }
            if (style.emojiMessageRatio < 0.1) {
                adapted = stripEmojis(adapted).trim()
            }
            if (index == 0 && style.emojiMessageRatio >= 0.35 &&
                style.preferredEmojis.isNotEmpty() && !containsEmoji(adapted)
            ) {
                adapted = "$adapted ${style.preferredEmojis.first()}"
            }
            adapted
        }
        .filter(String::isNotBlank)
        .distinct()
        .sortedBy { candidate ->
            abs(candidate.split(Regex("\\s+")).size - style.averageWords)
        }

    private fun containsEmoji(text: String): Boolean {
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            if (codePoint in 0x1F300..0x1FAFF || codePoint in 0x2600..0x27BF) return true
            offset += Character.charCount(codePoint)
        }
        return false
    }

    private fun stripEmojis(text: String): String = buildString {
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            val isEmojiPart = codePoint in 0x1F300..0x1FAFF ||
                codePoint in 0x2600..0x27BF ||
                codePoint in 0x1F1E6..0x1F1FF ||
                codePoint == 0xFE0F ||
                codePoint == 0x200D
            if (!isEmojiPart) appendCodePoint(codePoint)
            offset += Character.charCount(codePoint)
        }
    }
}
