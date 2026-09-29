package dev.patrickgold.florisboard.repli.account

import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.repli.suggestions.RepliAccountSessionProvider
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Erases server history before a saved chat is removed locally. */
object ProfileMemoryClient {
    suspend fun delete(profileId: String) {
        require(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}").matches(profileId))
        val token = RepliAccountSessionProvider.bearerTokenForRequest()
            ?: throw IllegalStateException("Sign in to remove this chat's cloud memory")
        val reply = URI(BuildConfig.REPLI_BACKEND_URL)
        require(reply.scheme.equals("https", ignoreCase = true) && !reply.host.isNullOrBlank())
        val origin = URI(reply.scheme, null, reply.host, reply.port, "/", null, null)
        val endpoint = origin.resolve("/v1/profile-memory/$profileId").toString()
        withContext(Dispatchers.IO) {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5_000
                readTimeout = 10_000
                useCaches = false
                instanceFollowRedirects = false
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
                    connection.responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    RepliAccountSessionProvider.invalidate(token)
                    throw IllegalStateException("Sign in again to remove this chat's cloud memory")
                }
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("Cloud memory could not be removed. Try again while connected.")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
