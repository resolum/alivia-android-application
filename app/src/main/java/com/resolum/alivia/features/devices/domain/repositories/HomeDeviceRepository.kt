package com.resolum.alivia.features.devices.domain.repositories

import com.resolum.alivia.features.devices.domain.models.HomeDevices
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult

interface HomeDeviceRepository {
    /** The home's devices, or null when the home has no active service yet. */
    suspend fun mine(): NetworkResult<HomeDevices?>
}
