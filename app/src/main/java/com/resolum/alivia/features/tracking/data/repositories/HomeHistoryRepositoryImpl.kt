package com.resolum.alivia.features.tracking.data.repositories

import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.datasources.remote.map
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import com.resolum.alivia.features.tracking.data.datasources.remote.HomeEventsSocket
import com.resolum.alivia.features.tracking.data.datasources.remote.services.HistoryEventDto
import com.resolum.alivia.features.tracking.data.datasources.remote.services.HomeHistoryService
import com.resolum.alivia.features.tracking.domain.models.HistoryDevice
import com.resolum.alivia.features.tracking.domain.models.HistoryEvent
import com.resolum.alivia.features.tracking.domain.models.LiveHistoryMessage
import com.resolum.alivia.features.tracking.domain.repositories.HomeHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.time.Instant

class HomeHistoryRepositoryImpl(
    private val service: HomeHistoryService,
    private val socket: HomeEventsSocket,
) : BaseRepository(), HomeHistoryRepository {

    override suspend fun recent(personId: Long, limit: Int): NetworkResult<List<HistoryEvent>> =
        safeCall { service.recent(personId, limit) }.map { dto -> dto.items.orEmpty().mapNotNull { it.toDomain() } }

    override suspend fun devices(personId: Long): NetworkResult<List<HistoryDevice>> =
        safeCall { service.status(personId) }.map { dto ->
            dto.devices.orEmpty().mapNotNull { d ->
                val id = d.deviceId ?: return@mapNotNull null
                HistoryDevice(id, d.alias?.trim()?.ifEmpty { null } ?: "Dispositivo", d.type)
            }
        }

    override fun live(personId: Long): Flow<LiveHistoryMessage> = socket.messages(personId).mapNotNull { message ->
        when (message.kind) {
            "connected" -> LiveHistoryMessage.Connected
            "event" -> message.event?.toDomain()?.let { LiveHistoryMessage.Event(it) }
            else -> null
        }
    }
}

internal fun HistoryEventDto.toDomain(): HistoryEvent? {
    val id = eventId ?: return null
    val kind = type ?: return null
    val at = occurredAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return HistoryEvent(id, kind, severity, result, action, deviceId, at, batteryPct, detail, commandId)
}
