package dev.patrickgold.florisboard.repli.data

import dev.patrickgold.florisboard.repli.profile.VoiceProfile
import dev.patrickgold.florisboard.repli.profile.VoiceStyle
import java.net.URLDecoder
import java.net.URLEncoder

/** Pure-JVM codec so profile persistence stays unit-testable outside Android. */
object VoiceProfileCodec {
    fun encode(profile: VoiceProfile): String = listOf(
        profile.id,
        encode(profile.name),
        encode(profile.relationship),
        profile.style.name,
        encode(profile.personaId),
        profile.nameAliases.joinToString(",") { encode(it) },
    ).joinToString(SEPARATOR)

    fun decode(value: String): VoiceProfile? {
        val parts = value.split(SEPARATOR, limit = 6)
        if (parts.size !in 4..6) return null
        val style = runCatching { VoiceStyle.valueOf(parts[3]) }.getOrNull() ?: return null
        val name = decodeOrNull(parts[1]) ?: return null
        val relationship = decodeOrNull(parts[2]) ?: return null
        val personaId = if (parts.size >= 5) decodeOrNull(parts[4]) ?: return null
            else style.name.lowercase()
        val aliases = if (parts.size == 6 && parts[5].isNotEmpty()) {
            parts[5].split(',').map { decodeOrNull(it) ?: return null }.distinct()
        } else emptyList()
        return VoiceProfile(parts[0], name, relationship, style, personaId, aliases)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun decodeOrNull(value: String): String? =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8.name()) }.getOrNull()

    private const val SEPARATOR = "|"
}
