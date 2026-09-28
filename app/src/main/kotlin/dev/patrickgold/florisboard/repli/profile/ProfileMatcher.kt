package dev.patrickgold.florisboard.repli.profile

/** Matches a notification sender name to a voice profile conservatively.
 * Comparison happens on whole-word tokens, never raw substrings, so "Ann"
 * matches "Message from Ann" but can never match "Joanne". Every word of the
 * profile name must be present in the sender string; extra sender words such
 * as "Message from" are tolerated, but partial overlaps like "Mary" matching
 * the profile "Mary Jane" are rejected because they cannot disambiguate
 * between people who share a first name. */
object ProfileMatcher {
    private val tokenSeparator = Regex("[^\\p{L}\\p{Nd}]+")

    fun matches(sender: String, profileName: String): Boolean {
        val senderTokens = tokenize(sender)
        val profileTokens = tokenize(profileName)
        if (senderTokens.isEmpty() || profileTokens.isEmpty()) return false
        return profileTokens.all { it in senderTokens }
    }

    fun matches(sender: String, profile: VoiceProfile): Boolean = matches(sender, profile.name)

    private fun tokenize(value: String): List<String> =
        value.lowercase().trim().split(tokenSeparator).filter(String::isNotBlank)
}
