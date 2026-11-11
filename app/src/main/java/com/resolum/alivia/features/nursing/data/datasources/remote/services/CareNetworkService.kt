package com.resolum.alivia.features.nursing.data.datasources.remote.services

import retrofit2.http.GET
import retrofit2.http.Path

data class AssistedPersonDto(
    val id: Long? = null,
    val householdId: Long? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val dni: String? = null,
    val birthDate: String? = null,
    val phoneNumber: String? = null,
    val kinship: String? = null,
    val mobilityLevel: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
)

data class HouseholdMemberDto(val userId: Long? = null, val responsibility: String? = null, val status: String? = null)

interface CareNetworkService {
    /** Assisted persons the signed-in caregiver looks after. */
    @GET("api/v1/assisted-persons")
    suspend fun assistedPersons(): List<AssistedPersonDto>

    @GET("api/v1/households/{householdId}/members")
    suspend fun members(@Path("householdId") householdId: Long): List<HouseholdMemberDto>
}
