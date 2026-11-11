package com.resolum.alivia.features.devices.data.repositories

import com.resolum.alivia.features.devices.data.datasources.remote.services.HomeDeviceService
import com.resolum.alivia.features.devices.domain.models.HomeDevice
import com.resolum.alivia.features.devices.domain.models.HomeDeviceType
import com.resolum.alivia.features.devices.domain.models.HomeDevices
import com.resolum.alivia.features.devices.domain.repositories.HomeDeviceRepository
import com.resolum.alivia.features.shared.data.datasources.remote.NetworkResult
import com.resolum.alivia.features.shared.data.repositories.BaseRepository

class HomeDeviceRepositoryImpl(private val service: HomeDeviceService) : BaseRepository(), HomeDeviceRepository {
    override suspend fun mine(): NetworkResult<HomeDevices?> = when (val result = safeCall { service.mine() }) {
        is NetworkResult.Error -> if (result.code == 404) NetworkResult.Success(null) else result
        is NetworkResult.Success -> NetworkResult.Success(
            HomeDevices(
                address = result.data.directions?.trim()?.ifEmpty { null },
                contactName = result.data.contactName?.trim()?.ifEmpty { null },
                devices = result.data.devices.orEmpty().mapIndexedNotNull { index, dto ->
                    val mac = dto.macAddress?.trim()?.ifEmpty { null } ?: return@mapIndexedNotNull null
                    HomeDevice(
                        id = dto.id ?: (index + 1L),
                        alias = dto.alias?.trim()?.ifEmpty { null } ?: "Dispositivo",
                        macAddress = mac,
                        type = HomeDeviceType.entries.firstOrNull { it.name == dto.type } ?: HomeDeviceType.OTHER,
                    )
                },
            ),
        )
    }
}
