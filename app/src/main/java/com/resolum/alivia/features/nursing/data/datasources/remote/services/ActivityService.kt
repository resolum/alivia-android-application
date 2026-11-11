package com.resolum.alivia.features.nursing.data.datasources.remote.services

import com.resolum.alivia.features.nursing.data.datasources.remote.models.ActivityResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ActivityService {
    /** Occurrences of the care agenda between [from] and [to] (ISO dates) of the signed-in caregiver. */
    @GET("api/v1/care-agenda")
    suspend fun agenda(@Query("from") from: String, @Query("to") to: String): List<ActivityResponseDto>
}
