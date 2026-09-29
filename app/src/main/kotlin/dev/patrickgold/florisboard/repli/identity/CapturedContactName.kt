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

    /** The header is read on device and never sent with the cropped conversation image. */
    fun fromHeaderLines(lines: List<String>): String? {
        val cleaned = lines.map { it.trim() }.filter { it.isNotEmpty() }
        val title = cleaned.firstOrNull() ?: return null
        val subtitles = cleaned.drop(1)
        if (subtitles.any { subtitle ->
                subtitle.contains(',') || subtitle.contains("participants", ignoreCase = true) ||
                    subtitle.contains("members", ignoreCase = true)
            }) return null
        return prepare(title.removeSuffix(" · practice chat"))
    }

    const val MAX_LENGTH = 80
}
