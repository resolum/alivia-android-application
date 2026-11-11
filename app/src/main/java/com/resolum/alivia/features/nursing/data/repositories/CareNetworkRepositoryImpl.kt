package com.resolum.alivia.features.nursing.data.repositories

import com.resolum.alivia.features.nursing.data.datasources.remote.services.AssignCareActivityRequestDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CareNetworkService
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CompleteActivityRequestDto
import com.resolum.alivia.features.nursing.data.datasources.remote.services.CreateCareActivityRequestDto
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CareCaregiver
import com.resolum.alivia.features.nursing.domain.models.AssistedPerson
import com.resolum.alivia.features.nursing.domain.models.CareNetworkMember
import com.resolum.alivia.features.nursing.domain.models.CareRole
import com.resolum.alivia.features.nursing.domain.repositories.CareNetworkRepository
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import retrofit2.HttpException

class CareNetworkRepositoryImpl(private val service: CareNetworkService) : BaseRepository(), CareNetworkRepository {
    override suspend fun assistedPersons() = safeCall {
        service.assistedPersons().mapNotNull { dto ->
            AssistedPerson(
                id = dto.id ?: return@mapNotNull null,
                relativeId = dto.relativeId,
                personProfileId = dto.personProfileId,
                householdId = dto.householdId,
                firstName = dto.firstName?.trim().orEmpty(),
                lastName = dto.lastName?.trim().orEmpty(),
                dni = dto.dni?.trim()?.ifEmpty { null },
                birthDate = dto.birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                phoneNumber = dto.phoneNumber?.trim()?.ifEmpty { null },
                kinship = dto.kinship,
                mobilityLevel = dto.mobilityLevel,
                emergencyContactName = dto.emergencyContactName?.trim()?.ifEmpty { null },
                emergencyContactPhone = dto.emergencyContactPhone?.trim()?.ifEmpty { null },
            )
        }
    }

    override suspend fun members(householdId: Long) = safeCall {
        service.members(householdId)
            .filter { it.status == null || it.status == "ACTIVE" }
            .mapNotNull { dto ->
                val role = CareRole.entries.firstOrNull { it.name == dto.responsibility } ?: return@mapNotNull null
                CareNetworkMember(dto.userId ?: return@mapNotNull null, role, dto.fullName?.trim()?.ifEmpty { null })
            }
            .sortedBy { it.role.ordinal }
    }

    override suspend fun myCaregiverIds() = safeCall { service.myCaregivers().mapNotNull { it.id }.toSet() }

    override suspend fun caregivers(assistedPersonId: Long, householdId: Long?) = safeCall {
        val mine = runCatching { service.myCaregivers() }.getOrDefault(emptyList()).mapNotNull { it.id }.toSet()
        // The household members bring the names the caregivers list lacks.
        val names = householdId?.let { id ->
            runCatching { service.members(id) }.getOrDefault(emptyList())
                .mapNotNull { member -> member.userId?.let { it to member.fullName?.trim()?.ifEmpty { null } } }.toMap()
        }.orEmpty()
        service.caregiversOf(assistedPersonId)
            .filter { it.status == null || it.status == "ACTIVE" }
            .mapNotNull { dto -> dto.id?.let { CareCaregiver(it, dto.userId, it in mine, dto.userId?.let(names::get)) } }
            .sortedByDescending { it.isMe }
    }

    override suspend fun createActivity(
        person: AssistedPerson,
        type: CareActivityType,
        title: String,
        description: String?,
        caregiverId: Long?,
        date: LocalDate,
        days: Set<DayOfWeek>,
        start: LocalTime,
        end: LocalTime,
    ) = safeCall {
        val activityId = service.createActivity(
            CreateCareActivityRequestDto(person.relativeId, person.id, type.name, title, description),
        )
        service.assign(
            activityId,
            AssignCareActivityRequestDto(
                relativeId = person.relativeId,
                caregiverId = caregiverId,
                scheduleType = if (days.isEmpty()) "ONE_TIME" else "WEEKLY",
                days = days.takeIf { it.isNotEmpty() }?.sortedBy { it.value }?.map { it.name },
                date = date.toString().takeIf { days.isEmpty() },
                startTime = start.format(HOUR),
                endTime = end.format(HOUR),
            ),
        )
        activityId
    }

    override suspend fun complete(activityId: Long, assignmentId: Long, date: LocalDate, notes: String?) = safeCall {
        val response = service.complete(CompleteActivityRequestDto(activityId, assignmentId, date.toString(), notes))
        if (!response.isSuccessful) throw HttpException(response)
    }

    private companion object {
        val HOUR: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
