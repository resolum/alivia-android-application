package com.resolum.alivia.features.iam.data.repositories

import com.resolum.alivia.features.iam.data.datasources.remote.AuthFacadeService
import com.resolum.alivia.features.iam.data.datasources.remote.mappers.SignInMapper
import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.SignIn
import com.resolum.alivia.features.iam.domain.models.UserRole
import com.resolum.alivia.features.iam.domain.repositories.AuthRepository
import com.resolum.alivia.features.shared.data.datasources.datastore.SessionTokenStore
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository

class AuthRepositoryImpl(
    private val facade: AuthFacadeService,
    private val tokens: SessionTokenStore,
) : BaseRepository(), AuthRepository {

    /** Signs in and keeps the session only for the roles that use the mobile app. */
    override suspend fun signIn(credentials: SignIn): NetworkResult<Session> {
        val result = safeCall { facade.signIn(credentials.email, credentials.password) }
        return when (result) {
            is NetworkResult.Error -> if (result.code == 401) {
                NetworkResult.Error("Correo o contraseña incorrectos.", 401)
            } else {
                result
            }
            is NetworkResult.Success -> {
                val stored = SignInMapper.toStoredSession(result.data)
                    ?: return NetworkResult.Error("La respuesta del servidor no es válida.")
                val session = SignInMapper.toSession(stored)
                if (!session.role.canUseMobileApp) return NetworkResult.Error(NOT_ALLOWED, 403)
                tokens.save(stored)
                NetworkResult.Success(session)
            }
        }
    }

    override suspend fun restoreSession(): Session? {
        val stored = tokens.restore() ?: return null
        val session = SignInMapper.toSession(stored)
        if (session.role == UserRole.UNKNOWN || !session.role.canUseMobileApp) {
            tokens.clear()
            return null
        }
        return session
    }

    override suspend fun signOut() = tokens.clear()

    companion object {
        const val NOT_ALLOWED = "Esta aplicación es para técnicos y cuidadores. Ingresa desde la web de Alivia."
    }
}
