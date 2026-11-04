package com.resolum.alivia.features.shared.data.datasources.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.resolum.alivia.features.shared.data.datasources.datastore.StoredSession
import com.resolum.alivia.features.shared.data.datasources.datastore.TokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

/**
 * The access token lasts minutes and a visit lasts hours: when the backend answers 401, the session is renewed
 * once with the refresh token and the call is repeated. If the renewal fails the session is closed.
 */
class TokenRefreshAuthenticator(
    private val baseUrl: String,
    private val tokens: TokenDataStore,
    private val onSessionExpired: () -> Unit,
) : Authenticator {
    private val gson = Gson()
    private val client = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/api/v1/auth/")) return null
        if (response.priorResponse != null) return null
        synchronized(this) {
            val current = tokens.session ?: return null
            val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
            // Another call already renewed the session while this one waited.
            if (sent != null && sent != current.accessToken) {
                return response.request.newBuilder().header("Authorization", "Bearer ${current.accessToken}").build()
            }
            val renewed = renew(current) ?: run {
                runBlocking { tokens.clear() }
                onSessionExpired()
                return null
            }
            runBlocking { tokens.save(renewed) }
            return response.request.newBuilder().header("Authorization", "Bearer ${renewed.accessToken}").build()
        }
    }

    private fun renew(current: StoredSession): StoredSession? = runCatching {
        val body = gson.toJson(mapOf("refreshToken" to current.refreshToken))
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(baseUrl + "api/v1/auth/refresh").post(body).build()
        client.newCall(request).execute().use { reply ->
            if (!reply.isSuccessful) return null
            val json = gson.fromJson(reply.body?.string(), JsonObject::class.java)
            current.copy(
                accessToken = json.get("accessToken").asString,
                refreshToken = json.get("refreshToken").asString,
            )
        }
    }.getOrNull()
}
