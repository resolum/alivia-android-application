package com.resolum.alivia.features.iam.data.datasources.remote.services

import com.resolum.alivia.features.iam.data.datasources.remote.models.SignInRequestDto
import com.resolum.alivia.features.iam.data.datasources.remote.models.SignInResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("api/v1/auth/sign-in")
    suspend fun signIn(@Body request: SignInRequestDto): SignInResponseDto
}
