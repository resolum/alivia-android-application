package com.resolum.alivia.features.notifications.domain

import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

/** Gives the push token of this installation (Firebase Cloud Messaging). */
interface PushTokenProvider {
    /** The current token, or null when the device cannot receive push notifications. */
    suspend fun currentToken(): String?
}

/** Devices of the signed-in account that receive push notifications. */
interface PushDeviceRepository {
    suspend fun register(token: String): NetworkResult<Unit>
    suspend fun unregister(token: String): NetworkResult<Unit>
}

/** Links this device to the signed-in account so the backend can send it notifications. */
class RegisterPushDeviceUseCase(
    private val tokens: PushTokenProvider,
    private val repository: PushDeviceRepository,
) {
    /** Registers [token] when it is known (a new token from Firebase), otherwise the current one. */
    suspend operator fun invoke(token: String? = null): NetworkResult<Unit> {
        val current = token?.takeIf { it.isNotBlank() } ?: tokens.currentToken()?.takeIf { it.isNotBlank() }
            ?: return NetworkResult.Error(NO_TOKEN)
        return repository.register(current)
    }

    companion object {
        const val NO_TOKEN = "Este dispositivo no puede recibir notificaciones."
    }
}

/** Stops the notifications of the account on this device; used before signing out. */
class UnregisterPushDeviceUseCase(
    private val tokens: PushTokenProvider,
    private val repository: PushDeviceRepository,
) {
    suspend operator fun invoke(): NetworkResult<Unit> {
        val token = tokens.currentToken()?.takeIf { it.isNotBlank() }
            ?: return NetworkResult.Success(Unit)
        return repository.unregister(token)
    }
}

/** What a received push message shows, with the defaults of the notification center. */
data class PushMessage(
    val notificationId: Long?,
    val title: String,
    val body: String?,
    val isAlert: Boolean,
    val type: String? = null,
) {
    /** A reminder of the agenda, shown on its own channel so it is not taken for an alert. */
    val isReminder: Boolean get() = type == "CARE_ACTIVITY_REMINDER"

    /** The agenda may have changed: a new or reassigned activity, or a reminder of one. */
    val touchesAgenda: Boolean get() = type in AGENDA_TYPES

    companion object {
        /**
         * Reads the message the backend sends: the visible title and body, and the data
         * `notificationId`, `type` and `sourceId`.
         */
        fun from(title: String?, body: String?, data: Map<String, String>): PushMessage = PushMessage(
            notificationId = data["notificationId"]?.toLongOrNull(),
            title = title?.trim()?.ifEmpty { null } ?: data["title"]?.trim()?.ifEmpty { null } ?: "Notificación",
            body = body?.trim()?.ifEmpty { null } ?: data["body"]?.trim()?.ifEmpty { null },
            isAlert = data["type"] in AppNotification.ALERT_TYPES,
            type = data["type"]?.trim()?.ifEmpty { null },
        )

        private val AGENDA_TYPES = setOf("ASSIGNMENT_CREATED", "CARE_ACTIVITY_REMINDER")
    }
}
