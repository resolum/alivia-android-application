package com.resolum.alivia.features.devices.data.datasources.remote.services

import retrofit2.http.GET

data class HomeDevicesResponseDto(
    val directions: String? = null,
    val contactName: String? = null,
    val devices: List<DeviceDto>? = null,
) {
    data class DeviceDto(val id: Long? = null, val alias: String? = null, val macAddress: String? = null, val type: String? = null)
}

interface HomeDeviceService {
    /** Installed devices of the account's home; 404 while the home has no active service. */
    @GET("api/v1/support-incidents/devices")
    suspend fun mine(): HomeDevicesResponseDto
}
