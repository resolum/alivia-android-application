package com.resolum.alivia.features.analytics.data.datasources.remote.mappers

import com.resolum.alivia.features.analytics.data.datasources.remote.models.MetricResponseDto
import com.resolum.alivia.features.analytics.domain.models.ActionResult
import com.resolum.alivia.features.analytics.domain.models.ActionSource
import com.resolum.alivia.features.analytics.domain.models.AutonomyAction
import com.resolum.alivia.features.analytics.domain.models.AutonomyDevice
import com.resolum.alivia.features.analytics.domain.models.AutonomyMetrics
import com.resolum.alivia.features.analytics.domain.models.ControlledDeviceType
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

object MetricMapper {
    fun toDomain(dto: MetricResponseDto, zone: ZoneId): AutonomyMetrics = AutonomyMetrics(
        actions = dto.actions.orEmpty().mapNotNull { action(it, zone) },
        devices = dto.devices.orEmpty().mapNotNull { device ->
            val id = device.id ?: return@mapNotNull null
            AutonomyDevice(id, device.name?.trim()?.ifEmpty { null } ?: "Dispositivo $id", enumOr(device.type, ControlledDeviceType.OTHER))
        },
        lastUpdatedAt = dateTime(dto.lastUpdatedAt, zone),
        // Without a report the Edge is taken as reachable; only an explicit false warns the caregiver.
        edgeOnline = dto.edgeOnline != false,
        pendingEvents = dto.pendingEvents == true,
    )

    /** Null without an event id or a valid moment, since such an event cannot be counted once. */
    private fun action(dto: MetricResponseDto.ActionDto, zone: ZoneId): AutonomyAction? {
        val eventId = dto.eventId?.trim()?.ifEmpty { null } ?: return null
        val occurredAt = dateTime(dto.occurredAt, zone) ?: return null
        val type = enumOr(dto.deviceType, ControlledDeviceType.OTHER)
        return AutonomyAction(
            eventId = eventId,
            deviceId = dto.deviceId,
            deviceName = dto.deviceName?.trim()?.ifEmpty { null } ?: "Dispositivo",
            deviceType = type,
            intent = dto.intent?.trim()?.ifEmpty { null } ?: "Acción",
            source = enumOr(dto.source, ActionSource.OTHER),
            result = enumOr(dto.result, ActionResult.NO_CONFIRMATION),
            occurredAt = occurredAt,
            executedLocally = dto.executedLocally == true,
        )
    }

    /** Accepts moments with an offset (converted to [zone]) or local ones. */
    private fun dateTime(value: String?, zone: ZoneId): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value) }.getOrNull()
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.trim()?.uppercase()?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
}
