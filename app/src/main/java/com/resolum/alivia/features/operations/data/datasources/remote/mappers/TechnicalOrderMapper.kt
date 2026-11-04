package com.resolum.alivia.features.operations.data.datasources.remote.mappers

import com.resolum.alivia.features.operations.data.datasources.remote.models.TechnicalOrderDto
import com.resolum.alivia.features.operations.domain.models.DeviceType
import com.resolum.alivia.features.operations.domain.models.EvaluationResult
import com.resolum.alivia.features.operations.domain.models.InstallationResult
import com.resolum.alivia.features.operations.domain.models.InstalledDevice
import com.resolum.alivia.features.operations.domain.models.OrderProgress
import com.resolum.alivia.features.operations.domain.models.ServiceType
import com.resolum.alivia.features.operations.domain.models.TechnicalOrder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

object TechnicalOrderMapper {
    fun toDomain(dto: TechnicalOrderDto): TechnicalOrder? {
        val id = dto.id ?: return null
        val type = enumOrNull<ServiceType>(dto.serviceType) ?: return null
        val date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return null
        val start = runCatching { LocalTime.parse(dto.startTime) }.getOrNull() ?: return null
        val end = runCatching { LocalTime.parse(dto.endTime) }.getOrNull() ?: return null
        val startedAt = instantOrNull(dto.startedAt)
        val progress = when (dto.status) {
            "COMPLETED" -> OrderProgress.COMPLETED
            "CANCELED" -> OrderProgress.CANCELED
            else -> if (startedAt != null) OrderProgress.IN_PROGRESS else OrderProgress.SCHEDULED
        }
        return TechnicalOrder(
            id = id,
            code = dto.code ?: "ORD-$id",
            draftId = dto.draftId.orEmpty(),
            serviceType = type,
            incidentId = dto.incidentId,
            progress = progress,
            date = date,
            startTime = start,
            endTime = end,
            technicianName = dto.technicianName.orEmpty(),
            contactName = dto.contactName?.takeIf { it.isNotBlank() },
            contactPhone = dto.contactPhone?.takeIf { it.isNotBlank() },
            directions = dto.directions?.takeIf { it.isNotBlank() },
            evaluationResult = enumOrNull<EvaluationResult>(dto.evaluationResult),
            installationResult = enumOrNull<InstallationResult>(dto.installationResult),
            completionNotes = dto.completionNotes,
            startedAt = startedAt,
            completedAt = instantOrNull(dto.completedAt),
            installedDevices = dto.installedDevices.orEmpty().mapNotNull { device ->
                val kind = enumOrNull<DeviceType>(device.type) ?: return@mapNotNull null
                InstalledDevice(device.alias.orEmpty(), device.macAddress.orEmpty(), kind)
            },
        )
    }

    private inline fun <reified E : Enum<E>> enumOrNull(value: String?): E? =
        enumValues<E>().firstOrNull { it.name == value }

    private fun instantOrNull(value: String?): Instant? = value?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
