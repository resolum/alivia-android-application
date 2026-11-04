package com.resolum.alivia.features.shared.data.datasources.datastore

/** Session tokens and the account that owns them, as the backend issued them. */
data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val accountId: String,
    val email: String,
    val role: String,
)

/** Where the session lives on the device; the network layer reads its tokens on every call. */
interface SessionTokenStore {
    val accessToken: String?
    val session: StoredSession?

    /** Loads the saved session when the app starts. */
    suspend fun restore(): StoredSession?
    suspend fun save(session: StoredSession)
    suspend fun clear()
}
