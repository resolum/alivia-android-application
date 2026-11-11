package com.resolum.alivia.features.operations.data.datasources.remote.services

import com.resolum.alivia.features.operations.data.datasources.remote.models.WorkingDayDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ScheduleService {
    /** Days with an assigned schedule between [from] and [to] (ISO dates, at most 62 days). */
    @GET("api/v1/technical-orders/mine/schedule")
    suspend fun mine(@Query("from") from: String, @Query("to") to: String): List<WorkingDayDto>
}
