package com.resolum.alivia.features.iam.domain.repositories

import com.resolum.alivia.features.iam.domain.models.Session
import com.resolum.alivia.features.iam.domain.models.SignIn
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

interface AuthRepository {
    suspend fun signIn(credentials: SignIn): NetworkResult<Session>
    suspend fun restoreSession(): Session?
    suspend fun signOut()
}
