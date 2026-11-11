package com.resolum.alivia.features.nursing.data.datasources.remote.mappers

import com.resolum.alivia.features.nursing.data.datasources.remote.models.ActivityResponseDto
import com.resolum.alivia.features.nursing.domain.models.CareActivity
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.CareActivityType
import com.resolum.alivia.features.nursing.domain.models.CarePriority
import java.time.LocalDate
import java.time.LocalTime

object ActivityMapper {
    /** Null when the date or the start time is not valid; unknown types, statuses and priorities get a default. */
    fun toDomain(dto: ActivityResponseDto): CareActivity? {
        val date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return null
        val start = runCatching { LocalTime.parse(dto.startTime) }.getOrNull() ?: return null
        val key = listOfNotNull(dto.assignmentId?.toString(), dto.activity?.id?.toString(), dto.date, dto.startTime).joinToString("-")
        return CareActivity(
            id = key,
            assignmentId = dto.assignmentId,
            title = nonBlank(dto.activity?.title) ?: "Actividad",
            type = enumOr(dto.activity?.type, CareActivityType.OTHER),
            assistedPerson = nonBlank(dto.assistedPerson?.name),
            responsible = nonBlank(dto.responsible?.name) ?: nonBlank(dto.responsible?.email),
            date = date,
            startTime = start,
            endTime = dto.endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            status = enumOr(dto.status, CareActivityStatus.SCHEDULED),
            recurring = dto.recurring ?: false,
            priority = enumOr(dto.priority, CarePriority.ROUTINE),
        )
    }

    private fun nonBlank(value: String?) = value?.trim()?.ifEmpty { null }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
}
