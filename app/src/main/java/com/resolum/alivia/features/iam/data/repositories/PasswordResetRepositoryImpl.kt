package com.resolum.alivia.features.iam.data.repositories

import com.resolum.alivia.features.iam.data.datasources.remote.services.ForgotPasswordRequestDto
import com.resolum.alivia.features.iam.data.datasources.remote.services.PasswordResetService
import com.resolum.alivia.features.iam.data.datasources.remote.services.ResetPasswordRequestDto
import com.resolum.alivia.features.iam.domain.repositories.PasswordResetRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import retrofit2.HttpException
import retrofit2.Response

class PasswordResetRepositoryImpl(private val service: PasswordResetService) : BaseRepository(), PasswordResetRepository {
    override suspend fun sendCode(email: String) = safeCall { service.forgotPassword(ForgotPasswordRequestDto(email)).orThrow() }

    override suspend fun reset(email: String, code: String, password: String) =
        safeCall { service.resetPassword(ResetPasswordRequestDto(email, code, password)).orThrow() }

    private fun Response<Unit>.orThrow() {
        if (!isSuccessful) throw HttpException(this)
    }
}
