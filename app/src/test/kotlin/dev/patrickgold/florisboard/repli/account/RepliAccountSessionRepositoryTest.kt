package dev.patrickgold.florisboard.repli.account

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class RepliAccountSessionRepositoryTest {
    private fun encode(raw: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())

    private fun token(issuedAt: Long, expiresAt: Long): String {
        val header = encode("""{"alg":"HS256","typ":"JWT"}""")
        val payload = encode(
            """{"iss":"assisted-backend","aud":"assisted-mobile","sub":"local-user","iat":$issuedAt,"exp":$expiresAt}"""
        )
        return "$header.$payload.signature"
    }

    @Test
    fun `accepts a well-formed session token`() {
        val now = System.currentTimeMillis() / 1_000L
        val state = RepliAccountSessionRepository.acceptFromTrustedSignIn(token(now, now + 900)).getOrThrow()
        assertNotNull(state.expiresAtEpochSeconds)
        RepliAccountSessionRepository.clear()
    }

    @Test
    fun `rejects expired or foreign tokens`() {
        val now = System.currentTimeMillis() / 1_000L
        assertFailsWith<IllegalArgumentException> {
            RepliAccountSessionRepository.acceptFromTrustedSignIn(token(now - 3_600, now - 10)).getOrThrow()
        }
        assertFailsWith<IllegalArgumentException> {
            RepliAccountSessionRepository.acceptFromTrustedSignIn("not.a.token").getOrThrow()
        }
    }
}
