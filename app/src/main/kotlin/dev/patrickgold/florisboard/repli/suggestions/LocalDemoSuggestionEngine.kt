package dev.patrickgold.florisboard.repli.suggestions

import dev.patrickgold.florisboard.repli.profile.VoiceStyle

/**
 * A deterministic, on-device stand-in for the eventual personalized model.
 * Keeping it behind SuggestionEngine lets the keyboard work before any chat data
 * leaves the phone and gives us a stable seam for a remote or on-device LLM.
 */
class LocalDemoSuggestionEngine : SuggestionEngine {
    override fun suggest(request: SuggestionRequest): List<String> {
        val message = request.incomingMessage.trim().lowercase()
        val intent = detectIntent(message)
        val candidates = replies[request.profile.style]?.get(intent)
            ?: replies.getValue(VoiceStyle.CASUAL).getValue(Intent.GENERIC)
        return request.learnedStyle?.let { SuggestionStyleAdapter.adapt(candidates, it) } ?: candidates
    }

    private fun detectIntent(message: String): Intent = when {
        message.isBlank() -> Intent.GENERIC
        containsAny(message, "how are you", "how're you", "how r u", "you good") -> Intent.HOW_ARE_YOU
        containsAny(message, "thank", "thanks", "tysm", "appreciate") -> Intent.THANKS
        containsAny(message, "sorry", "my bad", "apolog") -> Intent.SORRY
        containsAny(message, "tomorrow", "tonight", "what time", "when are", "meet", "available") -> Intent.PLANS
        message == "hi" || message == "hey" || message == "hello" || message.startsWith("hey ") -> Intent.GREETING
        else -> Intent.GENERIC
    }

    private fun containsAny(message: String, vararg needles: String) = needles.any(message::contains)

    private enum class Intent { GREETING, HOW_ARE_YOU, THANKS, SORRY, PLANS, GENERIC }

    private val replies = mapOf(
        VoiceStyle.CASUAL to mapOf(
            Intent.GREETING to listOf("heyy", "hey what's up", "hii how are you"),
            Intent.HOW_ARE_YOU to listOf("i'm good, you?", "doing alright, how about you", "i'm gooddd"),
            Intent.THANKS to listOf("of course", "anytimee", "no worries"),
            Intent.SORRY to listOf("it's okay", "don't worry about it", "we're good"),
            Intent.PLANS to listOf("yeah that works", "what time?", "let me check and get back to you"),
            Intent.GENERIC to listOf("yeahh", "fair enough", "wait tell me more"),
        ),
        VoiceStyle.WARM to mapOf(
            Intent.GREETING to listOf("heyy 😊", "hi! how are you?", "hey, good to hear from you"),
            Intent.HOW_ARE_YOU to listOf("i'm good, how are you?", "doing well 😊 how about you?", "i'm alright, thanks for asking"),
            Intent.THANKS to listOf("of course 😊", "anytime!", "you're very welcome"),
            Intent.SORRY to listOf("it's okay, don't worry", "no hard feelings 😊", "I understand, we're okay"),
            Intent.PLANS to listOf("yes, that works for me 😊", "I'd love that—what time?", "let me check and I'll let you know"),
            Intent.GENERIC to listOf("yeah, absolutely", "that makes sense", "tell me more 😊"),
        ),
        VoiceStyle.DIRECT to mapOf(
            Intent.GREETING to listOf("Hey.", "Hi, what's up?", "Hello."),
            Intent.HOW_ARE_YOU to listOf("I'm good. You?", "Doing well, thanks.", "I'm alright. How are you?"),
            Intent.THANKS to listOf("You're welcome.", "No problem.", "Of course."),
            Intent.SORRY to listOf("It's okay.", "Understood.", "No problem—we're good."),
            Intent.PLANS to listOf("That works.", "What time?", "I'll check and let you know."),
            Intent.GENERIC to listOf("Sounds good.", "Understood.", "Can you share more details?"),
        ),
    )
}
