package com.resolum.alivia.features.analytics.data.datasources.remote.models

/**
 * Autonomy metrics of the signed-in account's home. Only the fields below are read: any audio reference the backend
 * might add is ignored on purpose, so the app never exposes or plays a recording.
 */
data class MetricResponseDto(
    val lastUpdatedAt: String? = null,
    val edgeOnline: Boolean? = null,
    val pendingEvents: Boolean? = null,
    val devices: List<DeviceDto>? = null,
    val actions: List<ActionDto>? = null,
) {
    data class DeviceDto(val id: Long? = null, val name: String? = null, val type: String? = null)

    data class ActionDto(
        val eventId: String? = null,
        val deviceId: Long? = null,
        val deviceName: String? = null,
        val deviceType: String? = null,
        val intent: String? = null,
        val source: String? = null,
        val result: String? = null,
        val occurredAt: String? = null,
        val executedLocally: Boolean? = null,
    )
}
