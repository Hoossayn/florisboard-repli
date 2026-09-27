package dev.patrickgold.florisboard.ime.nlp.latin

import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.editor.InputAttributes

/** Restrict language predictions to ordinary text fields which accept composing text. */
object TypingPredictionPolicy {
    fun allows(info: FlorisEditorInfo): Boolean {
        val input = info.inputAttributes
        if (info.isRawInputEditor || input.type != InputAttributes.Type.TEXT ||
            input.flagTextNoSuggestions || input.flagTextAutoComplete) return false
        return when (input.variation) {
            InputAttributes.Variation.NORMAL,
            InputAttributes.Variation.SHORT_MESSAGE,
            InputAttributes.Variation.LONG_MESSAGE,
            InputAttributes.Variation.WEB_EDIT_TEXT,
            InputAttributes.Variation.EMAIL_SUBJECT -> true
            else -> false
        }
    }
}
