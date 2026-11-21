package com.resolum.alivia.features.analytics.data.datasources.remote.services

import com.resolum.alivia.features.analytics.data.datasources.remote.models.MetricResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MetricService {
    /** Actions requested by the assisted person between [from] and [to] (ISO dates), optionally of one device. */
    @GET("api/v1/autonomy-metrics")
    suspend fun autonomy(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("deviceId") deviceId: Long?,
    ): MetricResponseDto
}
