package com.resolum.alivia.features.profiles.data.datasources.remote.services

import com.resolum.alivia.features.profiles.data.datasources.remote.models.HouseholdResponseDto
import retrofit2.http.GET

interface HouseholdService {
    /** 404 when the account does not belong to any home. */
    @GET("api/v1/households/mine")
    suspend fun mine(): HouseholdResponseDto
}
