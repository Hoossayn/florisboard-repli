package dev.patrickgold.florisboard.repli.identity

/** Conservative boundary for an untrusted chat title returned by image reading. */
object CapturedContactName {
    private val genericTitles = setOf(
        "whatsapp", "telegram", "messenger", "messages", "chat", "online", "typing", "you",
    )

    fun prepare(value: String?): String? {
        val name = value?.replace(Regex("\\s+"), " ")?.trim() ?: return null
        if (name.length !in 1..MAX_LENGTH || name.none { it.isLetterOrDigit() }) return null
        val key = name.lowercase()
        if (key in genericTitles || key.startsWith("last seen") ||
            Regex("^\\d+\\s+(members|participants)$").matches(key)
        ) return null
        return name
    }

    const val MAX_LENGTH = 80
}
