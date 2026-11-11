package com.resolum.alivia.features.notifications.data

import com.resolum.alivia.features.notifications.domain.AppNotification
import com.resolum.alivia.features.notifications.domain.NotificationRepository
import com.resolum.alivia.features.shared.data.repositories.BaseRepository
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

data class NotificationDto(
    val id: Long? = null,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

data class MarkReadRequestDto(val ids: List<Long>)

interface NotificationService {
    /** Notifications of the signed-in account. */
    @GET("api/v1/notifications")
    suspend fun mine(): List<NotificationDto>

    @POST("api/v1/notifications/read")
    suspend fun markRead(@Body body: MarkReadRequestDto): Response<Unit>
}

class NotificationRepositoryImpl(private val service: NotificationService) : BaseRepository(), NotificationRepository {
    override suspend fun mine() = safeCall {
        service.mine().mapNotNull { dto ->
            AppNotification(
                id = dto.id ?: return@mapNotNull null,
                type = dto.type,
                title = dto.title?.trim()?.ifEmpty { null } ?: "Notificación",
                body = dto.body?.trim()?.ifEmpty { null },
                read = dto.status == "READ",
                createdAt = parse(dto.createdAt),
            )
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun markRead(ids: List<Long>) = safeCall {
        val response = service.markRead(MarkReadRequestDto(ids))
        if (!response.isSuccessful) throw HttpException(response)
    }

    /** Accepts "2026-11-10T08:15:00" and offsets like "2026-11-10T13:15:00Z", in Lima time. */
    private fun parse(value: String?): LocalDateTime? = value?.let {
        runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.of("America/Lima")).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(it) }.getOrNull()
    }
}
