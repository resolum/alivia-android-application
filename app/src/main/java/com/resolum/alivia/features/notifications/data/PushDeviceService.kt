package com.resolum.alivia.features.notifications.data

import com.resolum.alivia.features.notifications.domain.PushDeviceRepository
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

data class RegisterPushDeviceRequestDto(val token: String, val platform: String = "ANDROID")

interface PushDeviceService {
    /** Registers the token for the signed-in account, or moves it there from another account. */
    @POST("api/v1/push-devices")
    suspend fun register(@Body body: RegisterPushDeviceRequestDto): Response<Unit>

    @DELETE("api/v1/push-devices/{token}")
    suspend fun unregister(@Path("token") token: String): Response<Unit>
}

class PushDeviceRepositoryImpl(private val service: PushDeviceService) : BaseRepository(), PushDeviceRepository {
    override suspend fun register(token: String) = safeCall {
        val response = service.register(RegisterPushDeviceRequestDto(token))
        if (!response.isSuccessful) throw HttpException(response)
    }

    /** A token the backend no longer has is already unregistered. */
    override suspend fun unregister(token: String) = safeCall {
        val response = service.unregister(token)
        if (!response.isSuccessful && response.code() != 404) throw HttpException(response)
    }
}
