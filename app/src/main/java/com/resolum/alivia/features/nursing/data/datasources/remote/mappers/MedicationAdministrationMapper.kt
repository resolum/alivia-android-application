package com.resolum.alivia.features.nursing.data.datasources.remote.mappers

import com.resolum.alivia.features.nursing.data.datasources.remote.services.MedicationAdministrationDto
import com.resolum.alivia.features.nursing.domain.models.CareActivityStatus
import com.resolum.alivia.features.nursing.domain.models.MedicationAdministration
import com.resolum.alivia.features.nursing.domain.models.MedicationOutcome
import com.resolum.alivia.features.nursing.domain.models.MedicationResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object MedicationAdministrationMapper {
    /** Null when the id, date or start time is missing. */
    fun toDomain(dto: MedicationAdministrationDto): MedicationAdministration? {
        val id = dto.administrationId?.trim()?.ifEmpty { null } ?: return null
        val date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return null
        val start = runCatching { LocalTime.parse(dto.startTime) }.getOrNull() ?: return null
        val status = CareActivityStatus.entries.firstOrNull { it.name == dto.status } ?: CareActivityStatus.SCHEDULED
        return MedicationAdministration(
            id = id,
            medication = dto.medication?.trim()?.ifEmpty { null } ?: "Medicamento",
            assistedPerson = dto.assistedPerson?.name?.trim()?.ifEmpty { null },
            date = date,
            startTime = start,
            endTime = dto.endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
            responsible = dto.responsible?.name?.trim()?.ifEmpty { null },
            status = status,
            outcome = dto.outcome?.let { outcome(it, status) },
        )
    }

    /** The backend reports a dose closed by the missed-dose job as OMITTED; the occurrence status tells them apart. */
    private fun outcome(dto: MedicationAdministrationDto.OutcomeDto, status: CareActivityStatus): MedicationOutcome? {
        val result = when {
            status == CareActivityStatus.MISSED -> MedicationResult.MISSED
            dto.result == "ADMINISTERED" -> MedicationResult.ADMINISTERED
            dto.result == "OMITTED" -> MedicationResult.OMITTED
            else -> return null
        }
        return MedicationOutcome(
            result = result,
            occurredAt = dto.occurredAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
            reason = dto.reason?.trim()?.ifEmpty { null },
        )
    }
}
