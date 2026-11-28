package com.resolum.alivia.features.tracking.data.datasources.remote.services

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class HistoryEventDto(
    val eventId: String? = null,
    val type: String? = null,
    val severity: String? = null,
    val result: String? = null,
    val deviceId: Long? = null,
    val occurredAt: String? = null,
    val commandId: String? = null,
    val action: String? = null,
    val batteryPct: Int? = null,
    val detail: String? = null,
)

data class RecentEventsDto(val personId: Long? = null, val items: List<HistoryEventDto>? = null)

data class HomeStatusDto(val devices: List<DeviceStatusDto>? = null) {
    data class DeviceStatusDto(val deviceId: Long? = null, val alias: String? = null, val type: String? = null)
}

/** One message of the live channel: `connected` once, then one `event` for each new event. */
data class LiveMessageDto(val kind: String? = null, val personId: Long? = null, val event: HistoryEventDto? = null)

interface HomeHistoryService {
    @GET("api/v1/assisted-persons/{personId}/events/recent")
    suspend fun recent(@Path("personId") personId: Long, @Query("limit") limit: Int): RecentEventsDto

    @GET("api/v1/assisted-persons/{personId}/devices/status")
    suspend fun status(@Path("personId") personId: Long): HomeStatusDto
}
