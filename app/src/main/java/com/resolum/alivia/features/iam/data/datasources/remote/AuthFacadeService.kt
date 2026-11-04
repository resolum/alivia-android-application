package com.resolum.alivia.features.iam.data.datasources.remote

import com.resolum.alivia.features.iam.data.datasources.remote.models.SignInRequestDto
import com.resolum.alivia.features.iam.data.datasources.remote.models.SignInResponseDto
import com.resolum.alivia.features.iam.data.datasources.remote.services.AuthService

/** Entry point of the IAM endpoints the app uses. */
class AuthFacadeService(private val service: AuthService) {
    suspend fun signIn(email: String, password: String): SignInResponseDto =
        service.signIn(SignInRequestDto(email, password))
}
