package com.resolum.alivia.features.nursing.data.datasources.remote.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class AssistedPersonDto(
    val id: Long? = null,
    val relativeId: Long? = null,
    val personProfileId: Long? = null,
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

data class CaregiverDto(val id: Long? = null, val userId: Long? = null, val status: String? = null)

data class CreateCareActivityRequestDto(
    val relativeId: Long?,
    val assistedPersonId: Long,
    val type: String,
    val title: String,
    val description: String?,
)

/** ONE_TIME uses [date]; WEEKLY uses [days] (MONDAY…SUNDAY). Times are "HH:mm". */
data class AssignCareActivityRequestDto(
    val relativeId: Long?,
    val caregiverId: Long?,
    val scheduleType: String,
    val days: List<String>?,
    val date: String?,
    val startTime: String,
    val endTime: String,
)

data class CompleteActivityRequestDto(val careActivityId: Long, val assignmentId: Long, val date: String, val notes: String?)

data class HouseholdMemberDto(
    val userId: Long? = null,
    val responsibility: String? = null,
    val status: String? = null,
    val fullName: String? = null,
)

interface CareNetworkService {
    /** Assisted persons the signed-in caregiver looks after. */
    @GET("api/v1/assisted-persons")
    suspend fun assistedPersons(): List<AssistedPersonDto>

    /** Caregivers of the signed-in account (one per assisted person). */
    @GET("api/v1/caregivers")
    suspend fun myCaregivers(): List<CaregiverDto>

    @GET("api/v1/caregivers")
    suspend fun caregiversOf(@Query("assistedPersonId") assistedPersonId: Long): List<CaregiverDto>

    /** Returns the id of the new care activity. */
    @POST("api/v1/activities")
    suspend fun createActivity(@Body body: CreateCareActivityRequestDto): Long

    /** Returns the id of the new assignment. */
    @POST("api/v1/activities/{activityId}/assignments")
    suspend fun assign(@Path("activityId") activityId: Long, @Body body: AssignCareActivityRequestDto): Long

    @POST("api/v1/activity-logs/complete")
    suspend fun complete(@Body body: CompleteActivityRequestDto): Response<Long>

    @GET("api/v1/households/{householdId}/members")
    suspend fun members(@Path("householdId") householdId: Long): List<HouseholdMemberDto>
}
