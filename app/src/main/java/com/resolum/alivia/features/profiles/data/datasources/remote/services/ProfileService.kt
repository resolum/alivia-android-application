package com.resolum.alivia.features.profiles.data.datasources.remote.services

import com.resolum.alivia.features.profiles.data.datasources.remote.models.ProfileResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Part

interface ProfileService {
    @GET("api/v1/profiles/me")
    suspend fun me(): ProfileResponseDto

    /** Parts left null are not sent, so the backend keeps those values. */
    @Multipart
    @PATCH("api/v1/profiles/me")
    suspend fun update(
        @Part("firstName") firstName: RequestBody?,
        @Part("lastName") lastName: RequestBody?,
        @Part("phoneNumber") phoneNumber: RequestBody?,
        @Part photo: MultipartBody.Part?,
    ): ProfileResponseDto
}
