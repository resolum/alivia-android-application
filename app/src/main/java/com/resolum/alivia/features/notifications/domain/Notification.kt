package com.resolum.alivia.features.notifications.domain

import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import java.time.LocalDateTime

/** A message of the notification center. */
data class AppNotification(
    val id: Long,
    val type: String?,
    val title: String,
    val body: String?,
    val read: Boolean,
    val createdAt: LocalDateTime?,
) {
    /** Failures and disconnections show as alerts; the rest as information. */
    val isAlert: Boolean get() = type in setOf("ACTUATOR_FAILURE", "LOW_BATTERY", "EDGE_DISCONNECTED", "HELP_REQUEST_DELAYED", "PAYMENT_FAILED")
}

interface NotificationRepository {
    suspend fun mine(): NetworkResult<List<AppNotification>>
    suspend fun markRead(ids: List<Long>): NetworkResult<Unit>
}

class GetNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke() = repository.mine()
}

class MarkNotificationsReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(ids: List<Long>): NetworkResult<Unit> =
        if (ids.isEmpty()) NetworkResult.Success(Unit) else repository.markRead(ids)
}
