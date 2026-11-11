package com.resolum.alivia.features.iam.data.datasources.remote.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ForgotPasswordRequestDto(val email: String)

data class ResetPasswordRequestDto(val email: String, val code: String, val password: String)

interface PasswordResetService {
    /** Always 202, whether or not the email has an account. */
    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): Response<Unit>

    /** 204 when the password changed; 400 for a wrong or expired code, or a weak password. */
    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): Response<Unit>
}
