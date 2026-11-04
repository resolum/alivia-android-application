package com.resolum.alivia.features.shared.data.datasources.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionStore by preferencesDataStore(name = "alivia_session")


/**
 * Keeps the session on the device so the technician stays signed in between visits. The network layer reads
 * the tokens synchronously, so the last session is also kept in memory.
 */
class TokenDataStore(private val context: Context) : SessionTokenStore {
    @Volatile
    private var cached: StoredSession? = null

    override val accessToken: String? get() = cached?.accessToken
    val refreshToken: String? get() = cached?.refreshToken
    override val session: StoredSession? get() = cached

    /** Loads the saved session when the app starts. */
    override suspend fun restore(): StoredSession? {
        val preferences = context.sessionStore.data.first()
        val access = preferences[ACCESS]
        val refresh = preferences[REFRESH]
        cached = if (access.isNullOrBlank() || refresh.isNullOrBlank()) null else StoredSession(
            accessToken = access,
            refreshToken = refresh,
            accountId = preferences[ACCOUNT].orEmpty(),
            email = preferences[EMAIL].orEmpty(),
            role = preferences[ROLE].orEmpty(),
        )
        return cached
    }

    override suspend fun save(session: StoredSession) {
        cached = session
        context.sessionStore.edit { preferences ->
            preferences[ACCESS] = session.accessToken
            preferences[REFRESH] = session.refreshToken
            preferences[ACCOUNT] = session.accountId
            preferences[EMAIL] = session.email
            preferences[ROLE] = session.role
        }
    }

    override suspend fun clear() {
        cached = null
        context.sessionStore.edit { it.clear() }
    }

    private companion object {
        val ACCESS = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
        val ACCOUNT = stringPreferencesKey("account_id")
        val EMAIL = stringPreferencesKey("email")
        val ROLE = stringPreferencesKey("role")
    }
}
