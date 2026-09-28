package dev.patrickgold.florisboard.repli.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Base64

data class RepliSessionState(
    val authenticated: Boolean,
    val expiresAtEpochSeconds: Long? = null,
)

/**
 * Holds only the short-lived Repli backend token in process memory.
 * Never stores Firebase passwords, Admin credentials, or OpenAI keys.
 */
object RepliAccountSessionRepository {
    private val lock = Any()
    private var session: ValidatedSession? = null
    private val mutableState = MutableStateFlow(RepliSessionState(authenticated = false))

    val state: StateFlow<RepliSessionState> = mutableState.asStateFlow()

    fun acceptFromTrustedSignIn(token: String): Result<RepliSessionState> = runCatching {
        val validated = validateRepliSessionToken(token, System.currentTimeMillis() / 1_000L)
        synchronized(lock) {
            session = validated
            RepliSessionState(authenticated = true, expiresAtEpochSeconds = validated.expiresAtEpochSeconds)
                .also { mutableState.value = it }
        }
    }

    fun bearerToken(): String? = synchronized(lock) {
        val current = session ?: return@synchronized null
        if (current.expiresAtEpochSeconds <= System.currentTimeMillis() / 1_000L + MIN_REQUEST_LIFETIME_SECONDS) {
            session = null
            mutableState.value = RepliSessionState(authenticated = false)
            null
        } else {
            current.token
        }
    }

    fun invalidate(token: String) {
        synchronized(lock) {
            if (session?.token == token) {
                session = null
                mutableState.value = RepliSessionState(authenticated = false)
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            session = null
            mutableState.value = RepliSessionState(authenticated = false)
        }
    }

    internal data class ValidatedSession(val token: String, val expiresAtEpochSeconds: Long)

    const val EXPECTED_ISSUER = "assisted-backend"
    const val EXPECTED_AUDIENCE = "assisted-mobile"
        const val MAX_TOKEN_CHARACTERS = 4_096
        const val MAX_LIFETIME_SECONDS = 3_600L
        const val CLOCK_SKEW_SECONDS = 30L
        const val MIN_REQUEST_LIFETIME_SECONDS = 30L

        internal fun validateRepliSessionToken(token: String, nowEpochSeconds: Long): ValidatedSession {
            val normalized = token.trim()
            require(normalized.length in 1..MAX_TOKEN_CHARACTERS && TOKEN_PATTERN.matches(normalized)) {
                "That session token is not valid."
            }
            val parts = normalized.split('.')
            require(parts.size == 3) { "That session token is not valid." }
            val header = decodeSegment(parts[0])
            val payload = decodeSegment(parts[1])
            require(stringField(header, "alg") == "HS256" && stringField(header, "typ") == "JWT") {
                "That session token is not valid."
            }
            val issuedAt = longField(payload, "iat")
            val expiresAt = longField(payload, "exp")
            require(
                stringField(payload, "iss") == EXPECTED_ISSUER &&
                    stringField(payload, "aud") == EXPECTED_AUDIENCE &&
                    (stringField(payload, "sub") ?: "").matches(SUBJECT_PATTERN) &&
                    issuedAt <= nowEpochSeconds + CLOCK_SKEW_SECONDS &&
                    expiresAt > nowEpochSeconds + MIN_REQUEST_LIFETIME_SECONDS &&
                    expiresAt > issuedAt &&
                    expiresAt - issuedAt <= MAX_LIFETIME_SECONDS
            ) { "That session token is expired or not valid for Repli." }
            return ValidatedSession(normalized, expiresAt)
        }

        private fun decodeSegment(value: String): String = try {
            String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
        } catch (_: Exception) {
            throw IllegalArgumentException("That session token is not valid.")
        }

        private fun stringField(json: String, name: String): String? =
            Regex("\"$name\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)

        private fun longField(json: String, name: String): Long {
            val raw = Regex("\"$name\"\\s*:\\s*(-?\\d+)").find(json)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("That session token is not valid.")
            val number = raw.toDoubleOrNull() ?: throw IllegalArgumentException("That session token is not valid.")
            require(number.isFinite() && number % 1.0 == 0.0 && number >= 0.0 && number <= Long.MAX_VALUE.toDouble()) {
                "That session token is not valid."
            }
            return number.toLong()
        }

        val TOKEN_PATTERN = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
        val SUBJECT_PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._:@/-]{0,127}")
}
