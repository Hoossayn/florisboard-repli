package dev.patrickgold.florisboard.repli.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal class RepliAccountBackendException(message: String) : Exception(message)
internal class RepliAccountAuthException(message: String) : Exception(message)
internal class RepliRecentAuthRequiredException(message: String) : Exception(message)

internal data class RepliBackendResponse(val status: Int, val body: String)

/**
 * First-party Repli backend client. Network is used only to exchange the Firebase
 * ID token for a short-lived Repli session and to delete the account.
 * Captured chat text is only ever sent after explicit per-request approval
 * (chat capture UI lands separately).
 */
internal class RepliAccountBackendClient(
    private val sessionEndpoint: String,
    private val accountEndpoint: String,
) {
    suspend fun exchangeSession(firebaseIdToken: String): String = withContext(Dispatchers.IO) {
        val response = request("POST", sessionEndpoint, firebaseIdToken)
        if (response.status == HttpURLConnection.HTTP_UNAUTHORIZED ||
            response.status == HttpURLConnection.HTTP_FORBIDDEN
        ) {
            throw RepliAccountAuthException("The account session was rejected.")
        }
        if (response.status !in 200..299) throw RepliAccountBackendException("Account service is unavailable.")
        val json = runCatching { JSONObject(response.body) }
            .getOrElse { throw RepliAccountBackendException("Account service returned invalid data.") }
        val expiresIn = (json.opt("expires_in") as? Number)?.toDouble()
        if (json.optString("token_type") != "Bearer" || expiresIn == null || !expiresIn.isFinite() ||
            expiresIn % 1.0 != 0.0 || expiresIn !in 1.0..3_600.0
        ) {
            throw RepliAccountBackendException("Account service returned an invalid session contract.")
        }
        json.optString("token").takeIf { TOKEN_PATTERN.matches(it) }
            ?: throw RepliAccountBackendException("Account service omitted the session token.")
    }

    suspend fun deleteAccount(firebaseIdToken: String) = withContext(Dispatchers.IO) {
        val response = request("DELETE", accountEndpoint, firebaseIdToken)
        if (response.status == HttpURLConnection.HTTP_UNAUTHORIZED) {
            val error = runCatching { JSONObject(response.body).optString("error") }.getOrNull()
            if (error == "recent_authentication_required") {
                throw RepliRecentAuthRequiredException("Sign in again before deleting the account.")
            }
            throw RepliAccountAuthException("The account session was rejected.")
        }
        if (response.status !in 200..299) throw RepliAccountBackendException("Account deletion is unavailable.")
    }

    private fun request(method: String, endpoint: String, firebaseIdToken: String): RepliBackendResponse {
        require(FIREBASE_TOKEN_PATTERN.matches(firebaseIdToken) && firebaseIdToken.length <= 16 * 1_024) {
            "Firebase returned an invalid identity token."
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 10_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $firebaseIdToken")
            if (method == "POST") {
                doOutput = true
                setFixedLengthStreamingMode(0)
            }
        }
        try {
            if (method == "POST") connection.outputStream.use { }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.use { it.readBoundedUtf8(16 * 1_024) }
                .orEmpty()
            return RepliBackendResponse(status, body)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val TOKEN_PATTERN = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
        private val FIREBASE_TOKEN_PATTERN = TOKEN_PATTERN

        fun fromReplyEndpoint(replyEndpoint: String): RepliAccountBackendClient {
            val reply = URI(replyEndpoint)
            require(reply.scheme.equals("https", ignoreCase = true) && !reply.host.isNullOrBlank()) {
                "The Repli backend must be a complete HTTPS URL."
            }
            val origin = URI(reply.scheme, null, reply.host, reply.port, "/", null, null)
            return RepliAccountBackendClient(
                origin.resolve("/v1/session/exchange").toString(),
                origin.resolve("/v1/account").toString(),
            )
        }
    }
}

private fun InputStream.readBoundedUtf8(maxBytes: Int): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        if (output.size() + count > maxBytes) throw RepliAccountBackendException("Account response was too large.")
        output.write(buffer, 0, count)
    }
    return output.toString(Charsets.UTF_8.name())
}
