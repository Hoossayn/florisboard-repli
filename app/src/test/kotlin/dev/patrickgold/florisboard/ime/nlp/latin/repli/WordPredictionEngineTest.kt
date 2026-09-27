package dev.patrickgold.florisboard.ime.nlp.latin.repli

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordPredictionEngineTest {
    private val engine = WordPredictionEngine().apply {
        val dictionary = File("src/main/assets/ime/dict/repli-en_us.dict")
        dictionary.inputStream().use { installLexicon(BundledKeyboardLexicon.load(it)) }
    }

    @Test
    fun `commits a common typo but leaves dialect and mid-word text alone`() {
        assertEquals("the", engine.autocorrection(TypingContext("teh", "", 3, 3))?.word)
        assertNull(engine.autocorrection(TypingContext("abeg", "", 4, 4)))
        assertNull(engine.autocorrection(TypingContext("teh", "re", 3, 3)))
    }

    @Test
    fun `suggests a completion without exposing email text`() {
        assertTrue(engine.suggest(TypingContext("hell", "", 4, 4)).any { it.word == "hello" })
        assertTrue(engine.suggest(TypingContext("name@example", "", 12, 12)).isEmpty())
    }
}
