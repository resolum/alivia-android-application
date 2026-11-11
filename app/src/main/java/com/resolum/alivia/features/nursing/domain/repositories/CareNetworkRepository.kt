package com.resolum.alivia.features.nursing.domain.repositories

import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CareCaregiver
import com.resolum.alivia.features.nursing.domain.models.CareNetworkMember
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

interface CareNetworkRepository {
    suspend fun assistedPersons(): NetworkResult<List<AssistedPerson>>
    suspend fun members(householdId: Long): NetworkResult<List<CareNetworkMember>>
    suspend fun caregivers(assistedPersonId: Long, householdId: Long?): NetworkResult<List<CareCaregiver>>
    suspend fun myCaregiverIds(): NetworkResult<Set<Long>>

    /** Creates the activity and its schedule; [days] empty means a one-time occurrence on [date]. */
    suspend fun createActivity(
        person: AssistedPerson,
        type: CareActivityType,
        title: String,
        description: String?,
        caregiverId: Long?,
        date: LocalDate,
        days: Set<DayOfWeek>,
        start: LocalTime,
        end: LocalTime,
    ): NetworkResult<Long>

    suspend fun complete(activityId: Long, assignmentId: Long, date: LocalDate, notes: String?): NetworkResult<Unit>
}
