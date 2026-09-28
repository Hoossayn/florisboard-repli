package dev.patrickgold.florisboard.repli.data

/** Returns a prefix whose UTF-8 representation fits [maxBytes] without splitting
 * a Unicode code point. Storage limits are byte limits, not UTF-16 character limits. */
fun String.takeUtf8Bytes(maxBytes: Int): String {
    require(maxBytes >= 0)
    if (toByteArray(Charsets.UTF_8).size <= maxBytes) return this

    val result = StringBuilder()
    var offset = 0
    var usedBytes = 0
    while (offset < length) {
        val codePoint = codePointAt(offset)
        val codePointText = String(Character.toChars(codePoint))
        val codePointBytes = codePointText.toByteArray(Charsets.UTF_8).size
        if (usedBytes + codePointBytes > maxBytes) break
        result.append(codePointText)
        usedBytes += codePointBytes
        offset += Character.charCount(codePoint)
    }
    return result.toString()
}
