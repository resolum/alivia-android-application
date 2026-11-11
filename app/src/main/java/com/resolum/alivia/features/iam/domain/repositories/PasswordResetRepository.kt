package com.resolum.alivia.features.iam.domain.repositories

import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

interface PasswordResetRepository {
    suspend fun sendCode(email: String): NetworkResult<Unit>
    suspend fun reset(email: String, code: String, password: String): NetworkResult<Unit>
}
