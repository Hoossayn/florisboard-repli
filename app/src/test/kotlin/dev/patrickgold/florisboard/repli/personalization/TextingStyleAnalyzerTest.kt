package dev.patrickgold.florisboard.repli.personalization

import dev.patrickgold.florisboard.repli.profile.VoiceStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextingStyleAnalyzerTest {
    private val analyzer = TextingStyleAnalyzer()

    @Test
    fun `learns lowercase short emoji style and ignores attachment placeholders`() {
        val snapshot = analyzer.analyze(
            listOf(
                "yeahh 😂",
                "i'm coming",
                "okay bet 😂",
                "what time?",
                "😂",
                "<Media omitted>",
            ),
        )

        assertEquals(5, snapshot.style.messagesAnalyzed)
        assertEquals(1.0, snapshot.style.lowercaseStartRatio, 0.001)
        assertEquals(listOf("😂"), snapshot.style.preferredEmojis)
        assertTrue(snapshot.style.averageWords < 3.0)
        assertEquals(VoiceStyle.WARM, analyzer.inferFallbackStyle(snapshot.style))
    }

    @Test
    fun `infers direct fallback from capitalized punctuated messages`() {
        val snapshot = analyzer.analyze(
            listOf(
                "That works for me.",
                "I will confirm tomorrow.",
                "Please send the document.",
                "Thank you for the update.",
            ),
        )

        assertEquals(VoiceStyle.DIRECT, analyzer.inferFallbackStyle(snapshot.style))
        assertTrue(snapshot.style.summary().contains("usually capitalized"))
    }
}
