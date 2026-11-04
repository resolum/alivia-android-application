package com.resolum.alivia.features.operations.data.datasources.remote.services

import com.resolum.alivia.features.operations.data.datasources.remote.models.EvaluationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.InstallationResultRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.RegisterDeviceRequestDto
import com.resolum.alivia.features.operations.data.datasources.remote.models.TechnicalOrderDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TechnicalOrderService {
    @GET("api/v1/technical-orders/mine")
    suspend fun mine(): List<TechnicalOrderDto>

    @GET("api/v1/technical-orders/{orderId}")
    suspend fun get(@Path("orderId") orderId: Long): TechnicalOrderDto

    @POST("api/v1/technical-orders/{orderId}/start")
    suspend fun start(@Path("orderId") orderId: Long): TechnicalOrderDto

    @POST("api/v1/technical-orders/{orderId}/evaluation-result")
    suspend fun recordEvaluation(@Path("orderId") orderId: Long, @Body body: EvaluationResultRequestDto): TechnicalOrderDto

    @POST("api/v1/technical-orders/{orderId}/devices")
    suspend fun registerDevice(@Path("orderId") orderId: Long, @Body body: RegisterDeviceRequestDto): TechnicalOrderDto

    @DELETE("api/v1/technical-orders/{orderId}/devices/{macAddress}")
    suspend fun removeDevice(@Path("orderId") orderId: Long, @Path("macAddress") macAddress: String): TechnicalOrderDto

    @POST("api/v1/technical-orders/{orderId}/installation-result")
    suspend fun recordInstallation(@Path("orderId") orderId: Long, @Body body: InstallationResultRequestDto): TechnicalOrderDto
}
