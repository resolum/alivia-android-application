package com.resolum.alivia.features.nursing.data.datasources.remote.services

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Dose of `/api/v1/medication-administrations` as the backend returns it. */
data class MedicationAdministrationDto(
    val administrationId: String? = null,
    val medication: String? = null,
    val assistedPerson: PersonDto? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val responsible: ResponsibleDto? = null,
    val status: String? = null,
    val outcome: OutcomeDto? = null,
) {
    data class PersonDto(val id: Long? = null, val name: String? = null)
    data class ResponsibleDto(val type: String? = null, val id: Long? = null, val name: String? = null)
    data class OutcomeDto(val result: String? = null, val occurredAt: String? = null, val reason: String? = null)
}

data class RecordMedicationOutcomeRequestDto(val result: String, val reason: String?, val occurredAt: String)

interface MedicationAdministrationService {
    @GET("api/v1/medication-administrations/{id}")
    suspend fun find(@Path("id") id: String): MedicationAdministrationDto

    /** Answers 409 when the dose already has a result, so it is never recorded twice. */
    @POST("api/v1/medication-administrations/{id}/outcomes")
    suspend fun record(@Path("id") id: String, @Body body: RecordMedicationOutcomeRequestDto): MedicationAdministrationDto
}
