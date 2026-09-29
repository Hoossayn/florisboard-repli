package dev.patrickgold.florisboard.repli.profile

data class VoiceProfile(
    val id: String,
    val name: String,
    val relationship: String,
    val style: VoiceStyle,
    val personaId: String = style.name.lowercase(),
)

enum class VoiceStyle(val displayName: String) {
    CASUAL("Casual"),
    WARM("Warm"),
    DIRECT("Direct"),
}
