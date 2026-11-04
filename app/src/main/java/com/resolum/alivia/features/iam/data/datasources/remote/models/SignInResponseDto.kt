package com.resolum.alivia.features.iam.data.datasources.remote.models

data class SignInRequestDto(val email: String, val password: String)

/** Body of {@code POST /api/v1/auth/sign-in}. */
data class SignInResponseDto(
    val accessToken: String?,
    val refreshToken: String?,
    val tokenType: String?,
    val expiresIn: Long?,
    val user: AccountDto?,
)

data class AccountDto(
    val id: String?,
    val email: String?,
    val role: String?,
    val status: String?,
    val profileId: Long?,
)
